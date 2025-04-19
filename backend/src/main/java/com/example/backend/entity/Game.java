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
    public static final int MAX_PLAYERS = 6;
    
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
    private String smallBlindId;
    private String bigBlindId;
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
    
    public boolean isPlayersTurn(String playerId) {
        if (currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return false;
        }
        return players.get(currentPlayerIndex).getId().equals(playerId);
    }
    
    public boolean isGameFull() {
        return players.size() >= MAX_PLAYERS;
    }
    
    public boolean hasPlayer(String playerId) {
        return players.stream().anyMatch(p -> p.getId().equals(playerId));
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
        
        smallBlindId = players.get(smallBlindPosition).getId();
        bigBlindId = players.get(bigBlindPosition).getId();
        
        currentPlayerIndex = (dealerPosition + 1) % players.size();
        
        // Reset player hands and bets
        for (Player player : players) {
            player.reset();
        }
        
        updatedAt = LocalDateTime.now();
    }
    
    public void setupNextRound() {
        if (currentBettingRound == null) {
            currentBettingRound = new BettingRound();
        }
        currentBettingRound = new BettingRound();
        currentBet = 0;
        
        // Reset player actions for the new round
        for (Player player : players) {
            if (player.isActive()) {
                lastActions.put(player.getId(), PlayerAction.NONE);
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