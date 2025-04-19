package com.example.backend.service;

import com.example.backend.model.Card;
import com.example.backend.entity.Game;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class GameServiceImpl implements GameService {
    
    private final GameRepository gameRepository;
    private final HandEvaluator handEvaluator;
    private final BettingManager bettingManager;
    private final GameNotificationService notificationService;


    @Autowired
    public GameServiceImpl(GameRepository gameRepository,
                         HandEvaluator handEvaluator,
                         BettingManager bettingManager,
                         GameNotificationService notificationService) {
        this.gameRepository = gameRepository;
        this.handEvaluator = handEvaluator;
        this.bettingManager = bettingManager;
        this.notificationService = notificationService;
    }
    @Override
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @Override
    @Transactional
    public Game createGame() {
        Game game = new Game();
        return gameRepository.save(game);
    }
    
    @Override
    public Game getGame(String gameId) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw new IllegalArgumentException("Game ID cannot be null or empty");
        }
        return gameRepository.findById(gameId)
            .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));
    }
    
    @Override
    @Transactional
    public Game joinGame(String gameId, String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }

        Game game = getGame(gameId);
        
        if (game.isGameFull()) {
            throw new RuntimeException("Game is full");
        }
        
        if (game.hasPlayer(playerId)) {
            throw new RuntimeException("Player already in game");
        }
        
        if (game.getStatus() != Game.GameStatus.WAITING) {
            throw new RuntimeException("Cannot join a game in progress");
        }
        
        // Add player to game with 1000 chips
        Player player = new Player(playerId, "Player " + (game.getPlayers().size() + 1), 1000.0);
        game.getPlayers().add(player);
        game.setUpdatedAt(LocalDateTime.now());
        
        Game savedGame = gameRepository.save(game);
        
        try {
            notifyGameUpdate(savedGame);
        } catch (Exception e) {
            // Log the exception but don't fail the operation
            System.err.println("Failed to send game update notification: " + e.getMessage());
        }
        
        return savedGame;
    }
    
    @Override
    @Transactional
    public Game startNewHand(String gameId) {
        Game game = getGame(gameId);
        
        if (game.getPlayers().size() < 2) {
            throw new RuntimeException("Need at least 2 players to start a game");
        }
        
        if (game.getStatus() == Game.GameStatus.PLAYING) {
            throw new RuntimeException("Game already in progress");
        }
        
        // Get active players count
        long activePlayers = game.getPlayers().stream().filter(Player::isActive).count();
        if (activePlayers < 2) {
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
        
        // Start betting round
        bettingManager.startNewBettingRound(game);
        
        Game savedGame = gameRepository.save(game);
        
        try {
            notifyGameUpdate(savedGame);
        } catch (Exception e) {
            System.err.println("Failed to send game update notification: " + e.getMessage());
        }
        
        return savedGame;
    }
    
    @Override
    @Transactional
    public Game placeBet(String gameId, String playerId, double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Bet amount cannot be negative");
        }
        
        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);
        
        if (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.FINISHED) {
            throw new RuntimeException("Game not in progress");
        }
        
        if (!game.isPlayersTurn(playerId)) {
            throw new RuntimeException("Not player's turn");
        }
        
        if (player.isHasFolded()) {
            throw new RuntimeException("Player has folded");
        }
        
        // Validate bet amount
        double currentBet = game.getCurrentBet();
        double minRaise = currentBet - player.getCurrentBet();
        
        // Check if it's a valid amount (at least the current bet or all-in)
        if (amount < minRaise && amount < player.getChips()) {
            throw new RuntimeException("Bet must be at least " + minRaise + " or all-in");
        }
        
        // Cap the bet at player's available chips
        double actualBet = Math.min(amount, player.getChips());
        
        try {
            // Place the bet
            bettingManager.placeBet(game, player, actualBet);
            
            // Advance to next player
            game.moveToNextPlayer();
            
            // Check if betting round is complete
            if (bettingManager.isBettingRoundComplete(game)) {
                if (getActivePlayerCount(game) <= 1) {
                    evaluateHandAndAwardPot(game);
                    game.resetForNewHand();
                    game.setStatus(Game.GameStatus.WAITING);
                } else {
                    bettingManager.startNewBettingRound(game);
                }
            }
            
            game.setUpdatedAt(LocalDateTime.now());
            Game savedGame = gameRepository.save(game);
            
            try {
                notifyGameUpdate(savedGame);
            } catch (Exception e) {
                System.err.println("Failed to send game update notification: " + e.getMessage());
            }
            
            return savedGame;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public Game check(String gameId, String playerId) {
        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);
        
        if (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.FINISHED) {
            throw new RuntimeException("Game not in progress");
        }
        
        if (!game.isPlayersTurn(playerId)) {
            throw new RuntimeException("Not player's turn");
        }
        
        if (player.isHasFolded()) {
            throw new RuntimeException("Player has folded");
        }
        
        // Can only check if current bet equals player's bet
        if (game.getCurrentBet() > player.getCurrentBet()) {
            throw new RuntimeException("Cannot check when there's an active bet");
        }
        
        // Record the check action
        game.getLastActions().put(player.getId(), Game.PlayerAction.CHECK);
        
        // Advance to next player
        game.moveToNextPlayer();
        
        // Check if betting round is complete
        if (bettingManager.isBettingRoundComplete(game)) {
            if (getActivePlayerCount(game) <= 1) {
                evaluateHandAndAwardPot(game);
                game.resetForNewHand();
                game.setStatus(Game.GameStatus.WAITING);
            } else {
                bettingManager.startNewBettingRound(game);
            }
        }
        
        game.setUpdatedAt(LocalDateTime.now());
        Game savedGame = gameRepository.save(game);
        
        try {
            notifyGameUpdate(savedGame);
        } catch (Exception e) {
            System.err.println("Failed to send game update notification: " + e.getMessage());
        }
        
        return savedGame;
    }
    
    @Override
    @Transactional
    public Game fold(String gameId, String playerId) {
        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);
        
        if (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.FINISHED) {
            throw new RuntimeException("Game not in progress");
        }
        
        if (!game.isPlayersTurn(playerId)) {
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
            
            // Check if betting round is complete
            if (bettingManager.isBettingRoundComplete(game)) {
                bettingManager.startNewBettingRound(game);
            }
        }
        
        game.setUpdatedAt(LocalDateTime.now());
        Game savedGame = gameRepository.save(game);
        
        try {
            notifyGameUpdate(savedGame);
        } catch (Exception e) {
            System.err.println("Failed to send game update notification: " + e.getMessage());
        }
        
        return savedGame;
    }
    
    @Override
    @Transactional
    public Game leaveGame(String gameId, String playerId) {
        Game game = getGame(gameId);
        Player player = findPlayer(game, playerId);
        
        // Mark player as inactive
        player.setActive(false);
        
        // If game is in progress and it's the player's turn, auto-fold
        if ((game.getStatus() != Game.GameStatus.WAITING && game.getStatus() != Game.GameStatus.FINISHED) 
            && game.isPlayersTurn(playerId)) {
            return fold(gameId, playerId);
        }
        
        // If not enough active players, end the game
        long activePlayers = game.getPlayers().stream().filter(Player::isActive).count();
        if (activePlayers < 2) {
            game.setStatus(Game.GameStatus.FINISHED);
        }
        
        game.setUpdatedAt(LocalDateTime.now());
        Game savedGame = gameRepository.save(game);
        
        try {
            notifyGameUpdate(savedGame);
        } catch (Exception e) {
            System.err.println("Failed to send game update notification: " + e.getMessage());
        }
        
        return savedGame;
    }
    
    @Override
    @Transactional
    public boolean deleteGame(String gameId) {
        Game game = getGame(gameId);
        
        gameRepository.delete(game);
        
        try {
            GameUpdate update = new GameUpdate();
            update.setGameId(gameId);
            notificationService.notifyGameUpdate(update);
        } catch (Exception e) {
            System.err.println("Failed to send game deletion notification: " + e.getMessage());
        }
        
        return true;
    }
    
    private Player findPlayer(Game game, String playerId) {
        if (playerId == null || playerId.isEmpty()) {
            throw new IllegalArgumentException("Player ID cannot be null or empty");
        }
        
        return game.getPlayers().stream()
            .filter(p -> p.getId().equals(playerId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Player not found in game: " + playerId));
    }
    
    private int getActivePlayerCount(Game game) {
        return (int) game.getPlayers().stream()
                .filter(p -> p.isActive() && !p.isHasFolded())
                .count();
    }
    
    private void notifyGameUpdate(Game game) {
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setPayload(Map.of("gameState", game));
        notificationService.notifyGameUpdate(update);
    }
    
    private void evaluateHandAndAwardPot(Game game) {
        // Only evaluate if there are multiple players still in the hand
        List<Player> activePlayers = game.getPlayers().stream()
            .filter(p -> p.isActive() && !p.isHasFolded())
            .toList();
        
        if (activePlayers.isEmpty()) {
            // No active players, unusual case
            return;
        }
        
        if (activePlayers.size() == 1) {
            // Single player gets the pot
            activePlayers.get(0).awardPot(game.getPot());
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
        } catch (Exception e) {
            // If there's any error in hand evaluation, split the pot equally
            double splitAmount = game.getPot() / activePlayers.size();
            for (Player player : activePlayers) {
                player.awardPot(splitAmount);
            }
            
            System.err.println("Error during hand evaluation: " + e.getMessage());
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