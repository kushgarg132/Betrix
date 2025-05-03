package com.example.backend.entity;

import com.example.backend.model.BettingRound;
import com.example.backend.model.Card;
import com.example.backend.model.Deck;
import com.example.backend.model.Player;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Document(collection = "games")
public class Game {
    private int MAX_PLAYERS = 6;
    
    @Id
    private String id;
    private List<Player> players;
    private Deck deck;
    private List<Card> communityCards;
    private double pot;
    private GameStatus status;
    private BettingRound currentBettingRound;
    private int dealerPosition;
    private int currentPlayerIndex;
    private double minimumBet;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields required by BettingManager
    private String smallBlindUserId;
    private String bigBlindUserId;
    private double smallBlindAmount;
    private double bigBlindAmount;
    private double currentBet;
    private Map<String, PlayerAction> lastActions;
    
    public enum GameStatus {
        WAITING, PLAYING, FINISHED,
        // Additional states used in BettingManager
        STARTING, PRE_FLOP_BETTING, FLOP_BETTING, TURN_BETTING, RIVER_BETTING, SHOWDOWN
    }
    
    public enum PlayerAction {
        NONE, FOLD, CHECK, CALL, RAISE, ALL_IN
    }
    
    public Game() {
        this.id = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.communityCards = new ArrayList<>();
        this.pot = 0;
        this.status = GameStatus.WAITING;
        this.currentBettingRound = new BettingRound();
        this.dealerPosition = 0;
        this.currentPlayerIndex = 0;
        this.minimumBet = 1.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.currentBet = 0;
        this.lastActions = new HashMap<>();
        this.smallBlindAmount = 0.5;
        this.bigBlindAmount = 1.0;
    }

    public Game(Game game) {
        this.id = game.getId() != null ? game.getId() : UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        game.getPlayers().forEach(p -> {this.players.add(new Player(p)); this.players.forEach(Player::hideDetails);});
        this.deck = new Deck();
        this.communityCards = game.getCommunityCards() != null ? new ArrayList<>(game.getCommunityCards()) : new ArrayList<>();
        this.pot = game.getPot();
        this.status = game.getStatus() != null ? game.getStatus() : GameStatus.WAITING;
        this.currentBettingRound = game.getCurrentBettingRound();
        this.dealerPosition = game.getDealerPosition();
        this.currentPlayerIndex = game.getCurrentPlayerIndex() != -1 ? game.getCurrentPlayerIndex() : 0;
        this.minimumBet = game.getMinimumBet() != -1 ? game.getMinimumBet() : 1.0;
        this.createdAt = game.getCreatedAt() != null ? game.getCreatedAt() : LocalDateTime.now();
        this.updatedAt = game.getUpdatedAt() != null ? game.getUpdatedAt() : LocalDateTime.now();
        this.currentBet = game.getCurrentBet() != -1 ? game.getCurrentBet() : 0.0;
        this.lastActions = game.getLastActions() != null ? new HashMap<>(game.getLastActions()) : new HashMap<>();
        this.smallBlindAmount = game.getSmallBlindAmount();
        this.bigBlindAmount = game.getBigBlindAmount();
        this.players.forEach(p -> {p.setId(null);p.setHand(null);});
        this.MAX_PLAYERS = game.getMAX_PLAYERS();
        this.smallBlindUserId = game.getSmallBlindUserId();
        this.bigBlindUserId = game.getBigBlindUserId();
    }

    public boolean isPlayersTurn(String playerId) {
        if (currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return false;
        }
        return players.get(currentPlayerIndex).getId().equals(playerId);
    }
    
    public boolean isGameFull() {
        return players.size() >= MAX_PLAYERS;
    }
    
    public boolean hasPlayer(String username) {
        return players.stream().anyMatch(p -> p.getUsername().equals(username));
    }

    public Player getPlayerByUsername(String userId) {
        return players.stream().filter(p -> p.getUsername().equals(userId)).findFirst().orElse(null);
    }
    
    public void moveToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (!players.get(currentPlayerIndex).isActive() || 
                 players.get(currentPlayerIndex).isHasFolded());
    }
    
    public void resetForNewHand() {
        deck = new Deck();
        communityCards.clear();
        pot = 0;
        currentBettingRound = new BettingRound();
        currentBet = 0;
        lastActions.clear();
        
        // Rotate dealer position
        dealerPosition = (dealerPosition + 1) % players.size();
        
        // Set small and big blind positions
        int smallBlindPosition = (dealerPosition + 1) % players.size();
        int bigBlindPosition = (dealerPosition + 2) % players.size();
        
        smallBlindUserId = players.get(smallBlindPosition).getUsername();
        bigBlindUserId = players.get(bigBlindPosition).getUsername();
        
        currentPlayerIndex = (dealerPosition + 1) % players.size();
        
        // Reset player hands and bets
        for (Player player : players) {
            player.reset();
        }
        
        updatedAt = LocalDateTime.now();
    }
    
    public void setupNextRound() {
        currentBettingRound = new BettingRound();
        currentBet = 0;
        
        // Reset player actions for the new round
        for (Player player : players) {
            if (player.isActive()) {
                lastActions.put(player.getUsername(), PlayerAction.NONE);
            }
        }
        
        // Set first player to act (after dealer in post-flop rounds)
        int firstToActIdx = (dealerPosition + 1) % players.size();
        while (!players.get(firstToActIdx).isActive()) {
            firstToActIdx = (firstToActIdx + 1) % players.size();
        }
        currentPlayerIndex = firstToActIdx;
    }
}