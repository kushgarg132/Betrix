package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.model.*;
import com.example.backend.entity.Game;
import com.example.backend.event.*;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.backend.scheduler.GameScheduler;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final BettingManager bettingManager;
    private final GameValidatorService gameValidatorService;
    private final GameEventPublisher eventPublisher;
    private final GameScheduler gameScheduler;

    @Override
    public List<Game> getAllGames() {
        logger.info("Fetching all games");
        try {
            List<Game> games = gameRepository.findAll();
            games.forEach(game -> {
                game.getPlayers().forEach(Player::hideDetails);
                game.setDeck(new Deck());
            });
            logger.debug("Fetched games: {}", games);
            return games;
        } catch (Exception e) {
            logger.error("Error fetching games: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch games", e);
        }
    }

    @Override
    @Transactional
    public void createGame(BlindPayload payload) {
        logger.info("Creating a new game with small blind: {} and big blind: {}", payload.getSmallBlindAmount(),
                payload.getBigBlindAmount());
        Game game = new Game(payload.getSmallBlindAmount(), payload.getBigBlindAmount());
        gameRepository.save(game);
        logger.debug("Game created with ID: {}", game.getId());
    }

    @Override
    public Game getGameForPlayer(Game activeGame, String playerId) {
        try {
            activeGame.getPlayers().forEach(player -> {
                if (!playerId.equals(player.getId())) {
                    player.hideDetails();
                }
            });
            activeGame.setDeck(null);
            return activeGame;
        } catch (Exception e) {
            logger.error("Error fetching game for player: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch game for player", e);
        }
    }

    @Override
    public Game getGameForPlayer(String gameId, String playerId) {
        logger.info("Fetching game for player with ID: {}", playerId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            logger.debug("Fetched game for player: {}", game);
            return getGameForPlayer(game, playerId);
        } catch (Exception e) {
            logger.error("Error fetching game for player: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch game for player", e);
        }
    }

    @Override
    @Transactional
    public Game joinGame(String gameId, String username) {
        try {
            boolean isGuest = username != null && username.startsWith("guest-");
            User user = null;
            if (!isGuest) {
                user = userRepository.findByUsername(username).orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new RuntimeException("User not found: " + username);
                });
            }

            Game game = gameValidatorService.validateGameExists(gameId);
            gameValidatorService.validateGameNotFull(game);

            if (game.hasPlayer(username)) {
                logger.info("User '{}' is already in the game", username);
                return getGameForPlayer(game, game.getPlayerByUsername(username).getId());
            }

            Player player;
            if (isGuest) {
                // Create a transient guest player with default balance
                int guestBalance = 10000;
                player = new Player("Guest", username, guestBalance);
            } else {
                player = new Player(user.getName(), user.getUsername(), user.getBalance());
            }

            if (game.getStatus() != Game.GameStatus.WAITING) {
                player.setActive(false);
            }

            game.getPlayers().add(player);
            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            // Publish player joined event
            eventPublisher.publishEvent(new PlayerJoinedEvent(gameId, player));

            logger.debug("User '{}' joined game: {}", username, game);
            return getGameForPlayer(game, player.getId());
        } catch (Exception e) {
            logger.error("Error joining game: {}", e.getMessage());
            throw new RuntimeException("Failed to join game", e);
        }
    }

    @Override
    @Transactional
    public void startNewHand(String gameId) {
        logger.info("Starting a new hand for game with ID: {}", gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);

            // Auto Kick Logic: Remove players with insufficient chips
            List<Player> toKick = new ArrayList<>();
            for (Player player : game.getPlayers()) {
                if (player.getChips() < game.getBigBlindAmount()) {
                   toKick.add(player);
                }
            }
            
            for (Player player : toKick) {
                logger.info("Auto kicking player {} due to insufficient funds", player.getUsername());
                leaveGame(gameId, player.getId());
            }

            // Re-validate player count after kicks
             if (game.getPlayers().size() < 2) { // You might want to check active players here, but existing logic checks total players. Sticking to total players for game existence, but maybe we need 2 active players? 
                // Note: Existing logic throws if < 2 players. If auto-kick leaves 1 player, we should probably stop.
                 // However, let's stick to the existing check pattern if possible, or adapt.
                 // Actually, if we kick players, we might drop below 2.
            }
            
            // Check for active players (not sitting out)
            long activePlayersCount = game.getPlayers().stream().filter(p -> !p.isSittingOut()).count();
             if (activePlayersCount < 2) {
                logger.error("Need at least 2 active players to start a game");
                // Instead of throwing, maybe just set status to WAITING and return? 
                // But the caller expects a new hand. Let's throw for now as per existing logic.
                throw new RuntimeException("Need at least 2 active players to start a game");
            }

            if (game.getStatus() != Game.GameStatus.WAITING) {
                logger.error("Game already in progress");
                throw new RuntimeException("Game already in progress");
            }

            // Reset game state for a new hand
            game.resetForNewHand();
            game.setStatus(Game.GameStatus.STARTING);

            // Deal cards to players
            for (Player player : game.getPlayers()) {
                // Reset player state for new hand
                 player.reset();
                 
                if (!player.isSittingOut() && player.getChips() > 0) {
                    player.setActive(true); // Ensure they are marked active if they are playing
                    player.addCard(game.getDeck().drawCard());
                    player.addCard(game.getDeck().drawCard());
                    // Add active players to the main pot's eligible players
                    game.getMainPot().addEligiblePlayer(player.getId());
                } else {
                    player.setActive(false); // Mark as inactive for this hand
                }
            }

            // Publish game started event
            eventPublisher.publishEvent(new GameStartedEvent(gameId, new Game(game)));

            // Publish cards dealt event for player cards
            Map<String, List<Card>> playerCards = new HashMap<>();
            game.getPlayers().forEach(player -> {
                if (player.isActive() && player.getHand() != null && !player.getHand().isEmpty()) {
                    playerCards.put(player.getId(), new ArrayList<>(player.getHand()));
                }
            });
            eventPublisher.publishEvent(new CardsDealtEvent(gameId, playerCards));

            // Start betting round
            bettingManager.startNewBettingRound(game);

            gameRepository.save(game);

            logger.debug("New hand started for game: {}", game);
        } catch (Exception e) {
            logger.error("Error starting new hand: {}", e.getMessage());
            throw new RuntimeException("Failed to start new hand", e);
        }
    }

    @Override
    @Transactional
    public void placeBet(String gameId, String playerId, double amount) {
        logger.info("Player '{}' is placing a bet of {} in game '{}'", playerId, amount, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = findPlayer(game, playerId);

            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }
            gameValidatorService.validatePlayerTurn(game, playerId);
            gameValidatorService.validatePlayerBetAmount(game, player, amount);

            // Cancel any scheduled timeout for this player
            gameScheduler.cancelPlayerTimeout(gameId, playerId);
            // Perform bet logic
            bettingManager.placeBet(game, player, amount, null);
            // Start TimeOut scheduler
            String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId,
                    player,
                    PlayerActionEvent.ActionType.BET,
                    amount,
                    new Game(game)));

            // Check if betting round is complete
            bettingManager.handleCurrentBettingRound(game, playerId);

            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            logger.debug("Player '{}' placed bet: {}", playerId, amount);
        } catch (Exception e) {
            logger.error("Error placing bet: {}", e.getMessage());
            throw new RuntimeException("Failed to place bet", e);
        }
    }

    @Override
    @Transactional
    public void check(String gameId, String playerId) {
        logger.info("Player '{}' is checking in game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = findPlayer(game, playerId);

            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }

            gameValidatorService.validatePlayerTurn(game, playerId);
            // Cancel any scheduled timeout for this player
            gameScheduler.cancelPlayerTimeout(gameId, playerId);

            // Perform check logic (which is a bet of 0)
            bettingManager.placeBet(game, player, 0, null);
            // Start TimeOut scheduler
            String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId,
                    player,
                    PlayerActionEvent.ActionType.CHECK,
                    0.0,
                    new Game(game)));

            bettingManager.handleCurrentBettingRound(game, playerId);

            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            logger.debug("Player '{}' checked", playerId);
        } catch (Exception e) {
            logger.error("Error checking: {}", e.getMessage());
            throw new RuntimeException("Failed to check", e);
        }
    }

    @Override
    @Transactional
    public void fold(String gameId, String playerId) {
        logger.info("Player '{}' is folding in game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = findPlayer(game, playerId);

            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }

            gameValidatorService.validatePlayerTurn(game, playerId);
            // Cancel any scheduled timeout for this player
            gameScheduler.cancelPlayerTimeout(gameId, playerId);
            // Perform fold logic
            bettingManager.fold(game, player);
            // Start TimeOut scheduler
            String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId,
                    player,
                    PlayerActionEvent.ActionType.FOLD,
                    null,
                    new Game(game)));

            bettingManager.handleCurrentBettingRound(game, playerId);

            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            logger.debug("Player '{}' folded", playerId);
        } catch (Exception e) {
            logger.error("Error folding: {}", e.getMessage());
            throw new RuntimeException("Failed to fold", e);
        }
    }

    @Override
    @Transactional
    public void leaveGame(String gameId, String playerId) {
        logger.info("Player '{}' is leaving game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = findPlayer(game, playerId);

            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }

            // If player is current player, fold first
            if (game.isPlayersTurn(playerId)) {
                fold(game.getId(), playerId);
            }

            // Mark player as inactive
            game.getPlayers().remove(player);

            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId,
                    player,
                    PlayerActionEvent.ActionType.LEAVE,
                    null,
                    new Game(game)));

            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

            // If no players left, delete the game
            if (game.getPlayers().isEmpty()) {
                gameRepository.delete(game);
                logger.info("Game '{}' deleted as all players left", gameId);
                return;
            }

            gameRepository.save(game);

            logger.debug("Player '{}' left game", playerId);
        } catch (Exception e) {
            logger.error("Error leaving game: {}", e.getMessage());
            throw new RuntimeException("Failed to leave game", e);
        }
    }

    @Override
    @Transactional
    public void executeAllInAction(String gameId) {
        logger.info("Executing scheduled all-in action for game '{}'", gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);

            // Delegate back to BettingManager to process the next step
            bettingManager.processAllInRound(game);

            gameRepository.save(game);
        } catch (Exception e) {
            logger.error("Error executing all-in action: {}", e.getMessage());
            // Don't rethrow to scheduler, just log
        }
    }

    @Override
    @Transactional
    public void sitOut(String gameId, String playerId) {
        logger.info("Player '{}' is sitting out in game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = findPlayer(game, playerId);

            if (player == null) {
                throw new RuntimeException("Player not found");
            }

            player.setSittingOut(true);

            // If the player sits out during their turn, we might need to fold them?
            // Usually sit out takes effect next hand.
            // If they are currently active in a hand, they remain active until they fold or
            // hand ends.
            // But we can mark them so they don't get cards next hand.

            // Optional: If it's their turn, force a checks/fold?
            // For now, assume sit out is "next hand" effect or just state change.
            // The user requirement didn't specify immediate fold.

            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            // Limit event to player update or generic game update?
            // Reuse PlayerJoinedEvent or create generic PlayerUpdateEvent?
            // Existing events are specific. Let's send a game update via some mechanism if
            // needed.
            // But since we modify the game object and save it, the next polling/socket
            // update should catch it if we emit one.
            // We should probably emit an event. Reusing PlayerAction for now with a custom
            // type if strictly needed,
            // but we added SIT_OUT to ActionType enum.

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId,
                    player,
                    PlayerActionEvent.ActionType.SIT_OUT,
                    null,
                    new Game(game)));

        } catch (Exception e) {
            logger.error("Error sitting out: {}", e.getMessage());
            throw new RuntimeException("Failed to sit out", e);
        }
    }

    @Override
    @Transactional
    public void sitIn(String gameId, String playerId) {
        logger.info("Player '{}' is sitting in in game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = findPlayer(game, playerId);

            if (player == null) {
                throw new RuntimeException("Player not found");
            }

            player.setSittingOut(false);
            // They will be picked up in next hand's deals.

            game.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId,
                    player,
                    PlayerActionEvent.ActionType.SIT_IN,
                    null,
                    new Game(game)));

        } catch (Exception e) {
            logger.error("Error sitting in: {}", e.getMessage());
            throw new RuntimeException("Failed to sit in", e);
        }
    }

    @Override
    @Transactional
    public boolean deleteGame(String gameId) {
        logger.info("Deleting game with ID: {}", gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);

            gameRepository.delete(game);

            logger.debug("Game with ID '{}' deleted", gameId);
            return true;
        } catch (Exception e) {
            logger.error("Error deleting game: {}", e.getMessage());
            throw new RuntimeException("Failed to delete game", e);
        }
    }

    private Player findPlayer(Game game, String playerId) {
        for (Player player : game.getPlayers()) {
            if (player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
}