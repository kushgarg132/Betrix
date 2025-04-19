package com.example.backend.service;

import com.example.backend.model.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// Add these fields to the existing GameServiceImpl class
import com.example.backend.model.GameUpdate;
import java.util.Map;
import java.util.HashMap;
import com.example.backend.service.HandEvaluator;

@Service
public class GameServiceImpl implements GameService {
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameNotificationService notificationService;
    
    @Override
    public Game createGame() {
        Game game = new Game();
        return gameRepository.save(game);
    }
    
    // Update the existing methods to include notifications:
    @Override
    public Game joinGame(String gameId, String playerId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
                
        if (game.getPlayers().size() >= 9) {
            throw new IllegalStateException("Game is full");
        }
        
        Player player = new Player(playerId, "Player " + (game.getPlayers().size() + 1), 1000.0);
        game.getPlayers().add(player);
        
        game = gameRepository.save(game);
        
        GameUpdate update = new GameUpdate();
        update.setGameId(gameId);
        update.setType(GameUpdate.GameUpdateType.PLAYER_JOINED);
        update.setPayload(player);
        notificationService.notifyGameUpdate(update);
        
        return game;
    }
    
    @Override
    public Game placeBet(String gameId, String playerId, double amount) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
                
        Player player = findPlayer(game, playerId);
        if (player.getChips() < amount) {
            throw new IllegalStateException("Not enough chips");
        }
        
        player.setChips(player.getChips() - amount);
        game.setPot(game.getPot() + amount);
        game.setCurrentBet(amount);
        
        return gameRepository.save(game);
    }
    
    @Override
    public Game fold(String gameId, String playerId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
                
        Player player = findPlayer(game, playerId);
        player.setActive(false);
        
        return gameRepository.save(game);
    }
    
    @Override
    public Game dealCards(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
                
        game.getDeck().shuffle();
        for (Player player : game.getPlayers()) {
            if (player.isActive()) {
                player.getHand().add(game.getDeck().drawCard());
                player.getHand().add(game.getDeck().drawCard());
            }
        }
        
        game.setStatus(Game.GameStatus.PRE_FLOP);
        return gameRepository.save(game);
    }
    
    @Override
    public Game nextRound(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));
                
        switch (game.getStatus()) {
            case PRE_FLOP:
                dealFlop(game);
                break;
            case FLOP:
                dealTurn(game);
                break;
            case TURN:
                dealRiver(game);
                break;
            case RIVER:
                endGame(game);
                break;
            default:
                throw new IllegalStateException("Invalid game state");
        }
        
        return gameRepository.save(game);
    }
    
    private void dealFlop(Game game) {
        for (int i = 0; i < 3; i++) {
            game.getCommunityCards().add(game.getDeck().drawCard());
        }
        game.setStatus(Game.GameStatus.FLOP);
    }
    
    private void dealTurn(Game game) {
        game.getCommunityCards().add(game.getDeck().drawCard());
        game.setStatus(Game.GameStatus.TURN);
    }
    
    private void dealRiver(Game game) {
        game.getCommunityCards().add(game.getDeck().drawCard());
        game.setStatus(Game.GameStatus.RIVER);
    }
    
    private void endGame(Game game) {
        Map<Player, HandEvaluator.HandRank> playerHands = new HashMap<>();
        
        for (Player player : game.getPlayers()) {
            if (player.isActive()) {
                HandEvaluator.HandRank handRank = HandEvaluator.evaluate(
                    player.getHand(), 
                    game.getCommunityCards()
                );
                playerHands.put(player, handRank);
            }
        }
        
        Player winner = playerHands.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalStateException("No winner found"));
                
        winner.setChips(winner.getChips() + game.getPot());
        game.setPot(0);
        game.setStatus(Game.GameStatus.FINISHED);
        
        GameUpdate update = new GameUpdate();
        update.setGameId(game.getId());
        update.setType(GameUpdate.GameUpdateType.GAME_ENDED);
        update.setPayload(Map.of(
            "winner", winner,
            "handRank", playerHands.get(winner)
        ));
        notificationService.notifyGameUpdate(update);
    }
    
    private Player findPlayer(Game game, String playerId) {
        return game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }
}