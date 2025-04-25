package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.model.Card;
import com.example.backend.entity.Game;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.Player;
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
    private final HandEvaluator handEvaluator;
    private final BettingManager bettingManager;
    private final GameNotificationService notificationService;

    @Override
    public List<Game> getAllGames() {
        logger.info("Fetching all games");
        List<Game> games = gameRepository.findAll();
        games.forEach(game ->
                game.getPlayers().forEach(player -> {
                    player.setId(null);
                    player.setHand(null);
                }
        ));
        logger.debug("Fetched games: {}", games);
        return games;
    }

    @Override
    @Transactional
    public Game createGame() {
        logger.info("Creating a new game");
        Game game = new Game();
        Game savedGame = gameRepository.save(game);
        logger.debug("Created game: {}", savedGame);
        return savedGame;
    }

    @Override
    public Game getGame(String gameId) {
        logger.info("Fetching game with ID: {}", gameId);
        if (gameId == null || gameId.trim().isEmpty()) {
            logger.error("Game ID cannot be null or empty");
            throw new IllegalArgumentException("Game ID cannot be null or empty");
        }
        return gameRepository.findById(gameId)
                .orElseThrow(() -> {
                    logger.error("Game not found with ID: {}", gameId);
                    return new RuntimeException("Game not found: " + gameId);
                });
    }

    @Override
    public Game getGameForPlayer(String gameId, String playerId) {
        logger.info("Fetching game for player with ID: {}", playerId);
        Game game = getGame(gameId);
        game.getPlayers().forEach(player -> {
            if (!player.getId().equals(playerId)) {
                player.setId(null); // Hide other players' IDs'
                player.setHand(new ArrayList<>()); // Hide other players' hands
            }
        });
        logger.debug("Fetched game for player: {}", game);
        return game;
    }

    @Override
    @Transactional
    public Game joinGame(String gameId, String username) {
        logger.info("User '{}' is attempting to join game with ID: {}", username, gameId);
        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.error("User not found: {}", username);
            return new RuntimeException("User not found: " + username);
        });
        Game game = getGame(gameId);
        try {
            if (game.isGameFull()) {
                logger.error("Game is full");
                throw new RuntimeException("Game is full");
            }
            if (game.hasPlayer(username)) {
                logger.info("User '{}' is already in the game", username);
                return getGameForPlayer(gameId, game.getPlayerByUsername(username).getId());
            }
            if (game.getStatus() != Game.GameStatus.WAITING) {
                logger.error("Cannot join a game in progress");
                throw new RuntimeException("Cannot join a game in progress");
            }
        } catch (Exception e) {
            logger.error("Failed to send game update notification: {}", e.getMessage());
        }

        // Add player to game with 1000 chips
        Player player = new Player(user.getName(), user.getUsername(), user.getBalance());
        game.getPlayers().add(player);
        game.setUpdatedAt(LocalDateTime.now());
        gameRepository.save(game);

        notifyGameUpdate(gameId, GameUpdate.GameUpdateType.PLAYER_JOINED, new Player(player)); // Player Added

        logger.debug("User '{}' joined game: {}", username, game);
        return getGameForPlayer(gameId, player.getId());
    }

    @Override
    @Transactional
    public void startNewHand(String gameId) {
        logger.info("Starting a new hand for game with ID: {}", gameId);
        Game game = getGame(gameId);

        if (game.getPlayers().size() < 2) {
            logger.error("Need at least 2 players to start a game");
            throw new RuntimeException("Need at least 2 players to start a game");
        }

        if (game.getStatus() == Game.GameStatus.PLAYING) {
            logger.error("Game already in progress");
            throw new RuntimeException("Game already in progress");
        }

        // Get active players count
        long activePlayers = game.getPlayers().stream().filter(Player::isActive).count();
        if (activePlayers < 2) {
            logger.error("Need at least 2 active players to start a hand");
            throw new RuntimeException("Need at least 2 active players to start a hand");
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

        try {
            notifyGameUpdate(gameId, GameUpdate.GameUpdateType.GAME_STARTED, new Game(game));
            notifyPlayersHand(game); // GAME STARTED
        } catch (Exception e) {
            logger.error("Failed to send game update notification: {}", e.getMessage());
        }

        // Start betting round
        bettingManager.startNewBettingRound(game);

        game.setUpdatedAt(LocalDateTime.now());
        gameRepository.save(game);

        logger.debug("New hand started for game: {}", game);
    }

    @Override
    @Transactional
    public void placeBet(String gameId, String playerId, double amount) {
        logger.info("Player '{}' is placing a bet of {} in game with ID: {}", playerId, amount, gameId);
        if (amount < 0) {
            logger.error("Bet amount cannot be negative");
            throw new IllegalArgumentException("Bet amount cannot be negative");
        }

        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);

        if (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.FINISHED) {
            logger.error("Game not in progress");
            throw new RuntimeException("Game not in progress");
        }

        if (!game.isPlayersTurn(playerId)) {
            logger.error("Not player's turn");
            throw new RuntimeException("Not player's turn");
        }

        if (player.isHasFolded()) {
            logger.error("Player has folded");
            throw new RuntimeException("Player has folded");
        }

        try {
            // Place the bet
            bettingManager.placeBet(game, player, amount);

            // Advance to next player
            game.moveToNextPlayer();
            notifyGameUpdate(gameId, GameUpdate.GameUpdateType.PLAYER_TURN, game.getCurrentPlayerIndex());

            // Check if betting round is complete
            if (bettingManager.isBettingRoundComplete(game)) {
                if (getActivePlayerCount(game) <= 1 || game.getStatus() == Game.GameStatus.RIVER_BETTING) {
                    // END GAME
                    evaluateHandAndAwardPot(game);
                    game.resetForNewHand();
                    game.setStatus(Game.GameStatus.WAITING);
                    notifyGameUpdate(gameId, GameUpdate.GameUpdateType.GAME_ENDED, game);
                } else {
                    bettingManager.startNewBettingRound(game);
                }
            }

            game.setUpdatedAt(LocalDateTime.now());
            gameRepository.save(game);

            logger.debug("Bet placed. Updated game state: {}", game);
        } catch (IllegalArgumentException e) {
            logger.error("Error placing bet: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void check(String gameId, String playerId) {
        logger.info("Player '{}' is checking in game with ID: {}", playerId, gameId);
        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);

        if (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.FINISHED) {
            logger.error("Game not in progress");
            throw new RuntimeException("Game not in progress");
        }

        if (!game.isPlayersTurn(playerId)) {
            logger.error("Not player's turn");
            throw new RuntimeException("Not player's turn");
        }

        if (player.isHasFolded()) {
            logger.error("Player has folded");
            throw new RuntimeException("Player has folded");
        }

        // Can only check if current bet equals player's bet
        if (game.getCurrentBet() > player.getCurrentBet()) {
            logger.error("Cannot check when there's an active bet");
            throw new RuntimeException("Cannot check when there's an active bet");
        }

        // Place the bet
        bettingManager.placeBet(game, player, 0);

        // Advance to next player
        game.moveToNextPlayer();
        notifyGameUpdate(gameId, GameUpdate.GameUpdateType.PLAYER_TURN, game.getCurrentPlayerIndex());
        // Check if betting round is complete
        if (bettingManager.isBettingRoundComplete(game)) {
            if (getActivePlayerCount(game) <= 1 || game.getStatus() == Game.GameStatus.RIVER_BETTING) {
                evaluateHandAndAwardPot(game);
                game.resetForNewHand();
                game.setStatus(Game.GameStatus.WAITING);
                notifyGameUpdate(gameId, GameUpdate.GameUpdateType.GAME_ENDED, game);
            } else {
                bettingManager.startNewBettingRound(game);
            }
        }

        game.setUpdatedAt(LocalDateTime.now());
        gameRepository.save(game);

        logger.debug("Check action completed. Updated game state: {}", game);
    }

    @Override
    @Transactional
    public void fold(String gameId, String playerId) {
        logger.info("Player '{}' is folding in game with ID: {}", playerId, gameId);
        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);

        if (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.FINISHED) {
            logger.error("Game not in progress");
            throw new RuntimeException("Game not in progress");
        }

        if (!game.isPlayersTurn(playerId)) {
            logger.error("Not player's turn");
            throw new RuntimeException("Not player's turn");
        }

        // Mark player as folded and record action
        player.setHasFolded(true);
        bettingManager.fold(game, player);

        // Check if only one player remains active and not folded
        int activePlayers = getActivePlayerCount(game);

        if (activePlayers == 1) {
            // Award pot to last player
            for (Player p : game.getPlayers()) {
                if (p.isActive() && !p.isHasFolded()) {
                    p.awardPot(game.getPot());
                    break;
                }
            }

            // Reset for new hand
            game.resetForNewHand();
            game.setStatus(Game.GameStatus.WAITING);
        } else {
            // Advance to next player
            game.moveToNextPlayer();
            notifyGameUpdate(gameId, GameUpdate.GameUpdateType.PLAYER_TURN, game.getCurrentPlayerIndex());
            // Check if betting round is complete
            if (bettingManager.isBettingRoundComplete(game)) {
                if (game.getStatus() == Game.GameStatus.RIVER_BETTING) {
                    evaluateHandAndAwardPot(game);
                    game.resetForNewHand();
                    game.setStatus(Game.GameStatus.WAITING);
                    notifyGameUpdate(gameId, GameUpdate.GameUpdateType.GAME_ENDED, game);
                } else {
                    bettingManager.startNewBettingRound(game);
                }
            }
        }

        game.setUpdatedAt(LocalDateTime.now());
        gameRepository.save(game);

        logger.debug("Fold action completed. Updated game state: {}", game);
    }

    @Override
    @Transactional
    public void leaveGame(String gameId, String playerId) {
        logger.info("Player '{}' is leaving game with ID: {}", playerId, gameId);
        Game game = getGame(gameId);
        game.getPlayers().removeIf(p -> p.getId().equals(playerId));

        // If game is in progress and it's the player's turn, auto-fold
        if ((game.getStatus() != Game.GameStatus.WAITING && game.getStatus() != Game.GameStatus.FINISHED)
                && game.isPlayersTurn(playerId)) {
            fold(gameId, playerId);
        }

        // If not enough active players, end the game
        long activePlayers = game.getPlayers().stream().filter(Player::isActive).count();
        if (activePlayers < 2) {
            game.setStatus(Game.GameStatus.FINISHED);
        }

        game.setUpdatedAt(LocalDateTime.now());
        gameRepository.save(game);

        try {
            notifyGameUpdate(gameId, GameUpdate.GameUpdateType.PLAYER_LEFT, playerId);
        } catch (Exception e) {
            logger.error("Failed to send game update notification: {}", e.getMessage());
        }

        logger.debug("Player '{}' left the game. Updated game state: {}", playerId, game);
    }

    @Override
    @Transactional
    public boolean deleteGame(String gameId) {
        logger.info("Deleting game with ID: {}", gameId);
        Game game = getGame(gameId);

        gameRepository.delete(game);

        try {
//            GameUpdate update = new GameUpdate();
//            update.setGameId(gameId);
//            notificationService.notifyGameUpdate(update);
        } catch (Exception e) {
            logger.error("Failed to send game deletion notification: {}", e.getMessage());
        }

        logger.debug("Game with ID '{}' deleted", gameId);
        return true;
    }

    private Player findPlayer(Game game, String playerId) {
        if (playerId == null || playerId.isEmpty()) {
            logger.error("Player ID cannot be null or empty");
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> {
                    logger.error("Player not found in game: {}", playerId);
                    return new RuntimeException("Player not found in game: " + playerId);
                });
    }

    private int getActivePlayerCount(Game game) {
        return (int) game.getPlayers().stream()
                .filter(p -> p.isActive() && !p.isHasFolded())
                .count();
    }

    private void notifyPlayersHand(Game game) {
        game.getPlayers().forEach(player -> {
            notificationService.notifyPlayerUpdate(game.getId(), player.getId(), player.getHand());
        });
    }

    private void notifyGameUpdate(String gameId, GameUpdate.GameUpdateType updateType, Object payload) {
        GameUpdate update = new GameUpdate();
        update.setGameId(gameId);
        update.setType(updateType);

        if (update.getType().equals(GameUpdate.GameUpdateType.PLAYER_JOINED)) {
            Player player = new Player((Player) payload); // Create a copy of the player
            player.setId(null); // Hide sensitive information
            update.setPayload(player);
        } else {
            update.setPayload(payload);
        }

        notificationService.notifyGameUpdate(update);
    }

    private void evaluateHandAndAwardPot(Game game) {
        logger.info("Evaluating hands and awarding pot for game with ID: {}", game.getId());
        // Only evaluate if there are multiple players still in the hand
        List<Player> activePlayers = game.getPlayers().stream()
                .filter(p -> p.isActive() && !p.isHasFolded())
                .toList();

        if (activePlayers.isEmpty()) {
            logger.warn("No active players, unusual case");
            return;
        }

        if (activePlayers.size() == 1) {
            // Single player gets the pot
            activePlayers.get(0).awardPot(game.getPot());
            logger.debug("Single player awarded pot: {}", activePlayers.get(0));
            return;
        }

        try {
            // Multiple players - evaluate hands and find winner(s)
            Map<Player, HandEvaluator.HandResult> playerResults = new HashMap<>();
            HandEvaluator.HandResult bestResult = null;

            // Evaluate each hand
            for (Player player : activePlayers) {
                HandEvaluator.HandResult result = handEvaluator.evaluateHand(
                        player.getHand(),
                        game.getCommunityCards()
                );

                playerResults.put(player, result);

                // Track the best hand
                if (bestResult == null || compareHandResults(result, bestResult) > 0) {
                    bestResult = result;
                }
            }

            // Find all players with the best hand (could be multiple in case of a tie)
            List<Player> winners = new ArrayList<>();
            for (Map.Entry<Player, HandEvaluator.HandResult> entry : playerResults.entrySet()) {
                if (compareHandResults(entry.getValue(), bestResult) == 0) {
                    winners.add(entry.getKey());
                }
            }

            // Split pot among winners
            double winAmount = game.getPot() / winners.size();
            for (Player winner : winners) {
                winner.awardPot(winAmount);
            }

            logger.debug("Pot split among winners: {}", winners);
        } catch (Exception e) {
            // If there's any error in hand evaluation, split the pot equally
            double splitAmount = game.getPot() / activePlayers.size();
            for (Player player : activePlayers) {
                player.awardPot(splitAmount);
            }

            logger.error("Error during hand evaluation: {}", e.getMessage());
        }
    }

    // Compare two hand results
    private int compareHandResults(HandEvaluator.HandResult result1, HandEvaluator.HandResult result2) {
        // First compare hand ranks
        int rankComparison = result1.getRank().ordinal() - result2.getRank().ordinal();
        if (rankComparison != 0) {
            return rankComparison;
        }

        // If ranks are the same, compare high cards
        List<Card> highCards1 = result1.getHighCards();
        List<Card> highCards2 = result2.getHighCards();

        int minSize = Math.min(highCards1.size(), highCards2.size());

        for (int i = 0; i < minSize; i++) {
            int valueComparison = highCards1.get(i).getRank().getValue() -
                    highCards2.get(i).getRank().getValue();
            if (valueComparison != 0) {
                return valueComparison;
            }
        }

        // If we get here, the hands are identical in rank
        return 0;
    }
}