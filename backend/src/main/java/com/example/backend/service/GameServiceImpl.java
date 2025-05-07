package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.model.*;
import com.example.backend.entity.Game;
import com.example.backend.event.*;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameServiceImpl.class);

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final BettingManager bettingManager;
    private final GameNotificationService notificationService;
    private final GameValidatorService gameValidatorService;
    private final GameEventPublisher eventPublisher;

    @Override
    public List<Game> getAllGames() {
        logger.info("Fetching all games");
        try {
            List<Game> games = gameRepository.findAll();
            games.forEach(game ->{
                    game.getPlayers().forEach(Player::hideDetails);
                    game.setDeck(new Deck());
            }
            );
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
        logger.info("Creating a new game");
        try {
            Game game = new Game(payload.getSmallBlindAmount() , payload.getBigBlindAmount());
            Game savedGame = gameRepository.save(game);
            logger.debug("Created game: {}", savedGame);
        } catch (Exception e) {
            logger.error("Error creating game: {}", e.getMessage());
            throw new RuntimeException("Failed to create game", e);
        }
    }

    @Override
    public Game getGame(String gameId) {
        logger.info("Fetching game with ID: {}", gameId);
        try {
            gameValidatorService.validateGameExists(gameId);

            return gameRepository.findById(gameId)
                    .orElseThrow(() -> {
                        logger.error("Game not found with ID: {}", gameId);
                        return new RuntimeException("Game not found: " + gameId);
                    });
        } catch (Exception e) {
            logger.error("Error fetching game: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch game", e);
        }
    }

    @Override
    public Game getGameForPlayer(Game activeGame, String playerId) {
        logger.info("Fetching game for player with ID: {}", playerId);
        try {
            Game game = new Game(activeGame);
            game.getPlayers().forEach(player -> {
                if (!player.getId().equals(playerId)) {
                    player.setId(null); // Hide other players' IDs'
                    player.setHand(new ArrayList<>()); // Hide other players' hands
                }
            });
            logger.debug("Fetched game for player: {}", game);
            return game;
        } catch (Exception e) {
            logger.error("Error fetching game for player: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch game for player", e);
        }
    }

    @Override
    public Game getGameForPlayer(String gameId, String playerId) {
        logger.info("Fetching game for player with ID: {}", playerId);
        try {
            Game game = getGame(gameId);
            game.getPlayers().forEach(player -> {
                if (!player.getId().equals(playerId)) {
                    player.setId(null); // Hide other players' IDs'
                    player.setHand(new ArrayList<>()); // Hide other players' hands
                }
            });
            logger.debug("Fetched game for player: {}", game);
            return game;
        } catch (Exception e) {
            logger.error("Error fetching game for player: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch game for player", e);
        }
    }

    @Override
    @Transactional
    public Game joinGame(String gameId, String username) {
        logger.info("User '{}' is attempting to join game with ID: {}", username, gameId);
        try {
            User user = userRepository.findByUsername(username).orElseThrow(() -> {
                logger.error("User not found: {}", username);
                return new RuntimeException("User not found: " + username);
            });

            Game game = gameValidatorService.validateGameExists(gameId);
            gameValidatorService.validateGameNotFull(game);
//            gameValidatorService.validateGameStatus(game, Game.GameStatus.WAITING);

            if (game.hasPlayer(username)) {
                logger.info("User '{}' is already in the game", username);
                return getGameForPlayer(gameId, game.getPlayerByUsername(username).getId());
            }

            // Add player to game with 1000 chips
            Player player = new Player(user.getName(), user.getUsername(), user.getBalance());
            game.getPlayers().add(player);
            game.setUpdatedAt(LocalDateTime.now());
            gameRepository.save(game);

            user.setBalance(0);
            userRepository.save(user);

            // Publish player joined event
            eventPublisher.publishEvent(new PlayerJoinedEvent(gameId, new Player(player)));

            logger.debug("User '{}' joined game: {}", username, game);
            return getGameForPlayer(gameId, player.getId());
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
            Game game = getGame(gameId);

            if (game.getPlayers().size() < 2) {
                logger.error("Need at least 2 players to start a game");
                throw new RuntimeException("Need at least 2 players to start a game");
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
                if (player.isActive()) {
                    player.addCard(game.getDeck().drawCard());
                    player.addCard(game.getDeck().drawCard());
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

            game.setUpdatedAt(LocalDateTime.now());
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
            Game game = getGame(gameId);
            Player player = findPlayer(game, playerId);
            
            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }
            
            // Perform bet logic
            bettingManager.placeBet(game, player, amount, null);
            
            game.setUpdatedAt(LocalDateTime.now());
            game.setLastActivityTime(LocalDateTime.now());
            gameRepository.save(game);
            
            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                gameId, 
                playerId,
                PlayerActionEvent.ActionType.BET,
                amount,
                new Game(game)
            ));
            
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
            Game game = getGame(gameId);
            Player player = findPlayer(game, playerId);
            
            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }
            
            // Perform check logic - a check is essentially a bet of 0
            bettingManager.placeBet(game, player, 0, null);
            
            game.setUpdatedAt(LocalDateTime.now());
            game.setLastActivityTime(LocalDateTime.now());
            gameRepository.save(game);
            
            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                gameId, 
                playerId,
                PlayerActionEvent.ActionType.CHECK,
                null,
                new Game(game)
            ));
            
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
            Game game = getGame(gameId);
            Player player = findPlayer(game, playerId);
            
            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }
            
            // Perform fold logic
            bettingManager.fold(game, player);
            
            game.setUpdatedAt(LocalDateTime.now());
            game.setLastActivityTime(LocalDateTime.now());
            gameRepository.save(game);
            
            // Publish player action event
            eventPublisher.publishEvent(new PlayerActionEvent(
                gameId, 
                playerId,
                PlayerActionEvent.ActionType.FOLD,
                null,
                new Game(game)
            ));
            
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
            Game game = getGame(gameId);
            Player player = findPlayer(game, playerId);
            
            if (player == null) {
                logger.error("Player not found in game: {}", playerId);
                throw new RuntimeException("Player not found in game");
            }

            // Update user balance
            User user = userRepository.findByUsername(player.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + player.getUsername()));
            user.setBalance((int)player.getChips());
            userRepository.save(user);
            
            // Remove player from game
            game.getPlayers().remove(player);
            game.setUpdatedAt(LocalDateTime.now());
            game.setLastActivityTime(LocalDateTime.now());
            
            // If no players left, delete the game
            if (game.getPlayers().isEmpty()) {
                gameRepository.delete(game);
                logger.info("Game '{}' deleted as all players left", gameId);
                return;
            }
            
            gameRepository.save(game);
            
            // Publish player action event for leaving
            eventPublisher.publishEvent(new PlayerActionEvent(
                gameId, 
                playerId,
                PlayerActionEvent.ActionType.LEAVE,
                null,
                new Game(game)
            ));
            
            logger.debug("Player '{}' left game", playerId);
        } catch (Exception e) {
            logger.error("Error leaving game: {}", e.getMessage());
            throw new RuntimeException("Failed to leave game", e);
        }
    }

    @Override
    @Transactional
    public boolean deleteGame(String gameId) {
        logger.info("Deleting game with ID: {}", gameId);
        try {
            Game game = getGame(gameId);

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

    /**
     * Updates all active games to mark the current turn player's action deadline
     */
    @Override
    @Transactional
    public void updateAllGameActionDeadlines() {
        try {
            List<Game> activeGames = gameRepository.findActiveGames();
            
            for (Game game : activeGames) {
                if (game.getCurrentPlayerActionDeadline() == null && 
                    game.getCurrentPlayerIndex() >= 0 &&
                    game.getCurrentPlayerIndex() < game.getPlayers().size()) {
                    
                    game.updateCurrentPlayerActionDeadline();
                    gameRepository.save(game);
                    
                    logger.debug("Updated action deadline for game {}, player {}", 
                                game.getId(), 
                                game.getPlayers().get(game.getCurrentPlayerIndex()).getId());
                }
            }
        } catch (Exception e) {
            logger.error("Error updating game action deadlines: {}", e.getMessage(), e);
        }
    }
}