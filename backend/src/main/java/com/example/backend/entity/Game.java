package com.example.backend.entity;

import com.example.backend.model.BettingRound;
import com.example.backend.model.Card;
import com.example.backend.model.Deck;
import com.example.backend.model.Player;
import com.example.backend.model.Pot;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@Document(collection = "games")
public class Game {
    private int MAX_PLAYERS = 6;
    
    // Default timeout values
    public static final int DEFAULT_PLAYER_ACTION_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_GAME_IDLE_TIMEOUT_MINUTES = 10;
    
    @Id
    private String id;
    private List<Player> players;
    private Deck deck;
    private List<Card> communityCards;
    private double pot; // Total pot (sum of all pots)
    private List<Pot> pots; // Multiple pots for side pots
    private GameStatus status;
    private BettingRound currentBettingRound;
    private int dealerPosition;
    private int currentPlayerIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Timeout tracking fields
    private Instant currentPlayerActionDeadline;
    private LocalDateTime lastActivityTime;
    private int playerActionTimeoutSeconds = DEFAULT_PLAYER_ACTION_TIMEOUT_SECONDS;
    private int gameIdleTimeoutMinutes = DEFAULT_GAME_IDLE_TIMEOUT_MINUTES;
    private boolean autoStart = true;
    
    // Additional fields required by BettingManager
    private String smallBlindUserId;
    private String bigBlindUserId;
    private double smallBlindAmount;
    private double bigBlindAmount;
    private double currentBet;
    private Map<String, PlayerAction> lastActions;
    
    public enum GameStatus {
        WAITING, STARTING, PRE_FLOP_BETTING, FLOP_BETTING, TURN_BETTING, RIVER_BETTING, SHOWDOWN , FINISHED
    }
    
    public enum PlayerAction {
        NONE, FOLD, CHECK, SMALL_BLIND, BIG_BLIND, CALL, RAISE, ALL_IN, AUTO_FOLD
    }
    
    public Game(int smallBlindAmount, int bigBlindAmount) {
        this.id = UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.communityCards = new ArrayList<>();
        this.pot = 0;
        this.pots = new ArrayList<>();
        this.pots.add(new Pot(0)); // Main pot
        this.status = GameStatus.WAITING;
        this.currentBettingRound = new BettingRound();
        this.dealerPosition = 0;
        this.currentPlayerIndex = -1;
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.currentBet = 0;
        this.lastActions = new HashMap<>();
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.lastActivityTime = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Game(Game game) {
        this.id = game.getId() != null ? game.getId() : UUID.randomUUID().toString();
        this.players = new ArrayList<>();
        game.getPlayers().forEach(p -> {this.players.add(new Player(p));});
        this.deck = new Deck();
        this.communityCards = game.getCommunityCards() != null ? new ArrayList<>(game.getCommunityCards()) : new ArrayList<>();
        this.pot = game.getPot();
        this.pots = game.getPots() != null ? new ArrayList<>(game.getPots()) : new ArrayList<>();
        if (this.pots.isEmpty()) {
            this.pots.add(new Pot(0)); // Main pot
        }
        this.status = game.getStatus() != null ? game.getStatus() : GameStatus.WAITING;
        this.currentBettingRound = game.getCurrentBettingRound();
        this.dealerPosition = game.getDealerPosition();
        this.currentPlayerIndex = game.getCurrentPlayerIndex() != -1 ? game.getCurrentPlayerIndex() : 0;
        this.createdAt = game.getCreatedAt() != null ? game.getCreatedAt() : LocalDateTime.now(ZoneOffset.UTC);
        this.updatedAt = game.getUpdatedAt() != null ? game.getUpdatedAt() : LocalDateTime.now(ZoneOffset.UTC);
        this.currentBet = game.getCurrentBet() != -1 ? game.getCurrentBet() : 0.0;
        this.lastActions = game.getLastActions() != null ? new HashMap<>(game.getLastActions()) : new HashMap<>();
        this.smallBlindAmount = game.getSmallBlindAmount();
        this.bigBlindAmount = game.getBigBlindAmount();
        this.MAX_PLAYERS = game.getMAX_PLAYERS();
        this.smallBlindUserId = game.getSmallBlindUserId();
        this.bigBlindUserId = game.getBigBlindUserId();
        this.currentPlayerActionDeadline = game.getCurrentPlayerActionDeadline();
        this.lastActivityTime = game.getLastActivityTime() != null ? game.getLastActivityTime() : LocalDateTime.now(ZoneOffset.UTC);
        this.playerActionTimeoutSeconds = game.getPlayerActionTimeoutSeconds();
        this.gameIdleTimeoutMinutes = game.getGameIdleTimeoutMinutes();
        this.autoStart = game.isAutoStart();
    }

    // Add a bet amount to the appropriate pot
    public void addToPot(double amount) {
        if (amount <= 0) {
            return; // Ignore zero or negative amounts
        }
        
        this.pot += amount;
        
        // Add to the main pot by default
        if (!pots.isEmpty()) {
            pots.get(0).addAmount(amount);
            
            // Make sure all active players are eligible for the main pot
            players.stream()
                .filter(p -> p.isActive() && !p.isHasFolded())
                .forEach(p -> pots.get(0).addEligiblePlayer(p.getId()));
        } else {
            Pot mainPot = new Pot(amount);
            // Add all active players to the main pot
            players.stream()
                .filter(p -> p.isActive() && !p.isHasFolded())
                .forEach(p -> mainPot.addEligiblePlayer(p.getId()));
            pots.add(mainPot);
        }
    }
    
    // Create a side pot when a player goes all-in
    public void createSidePot(double amount, List<Player> eligiblePlayers) {
        Pot sidePot = new Pot(amount);
        eligiblePlayers.forEach(p -> sidePot.addEligiblePlayer(p.getId()));
        pots.add(sidePot);
    }
    
    // Get the main pot
    public Pot getMainPot() {
        if (pots.isEmpty()) {
            Pot mainPot = new Pot(0);
            pots.add(mainPot);
            return mainPot;
        }
        return pots.get(0);
    }
    
    // Get all side pots
    public List<Pot> getSidePots() {
        if (pots.size() <= 1) {
            return new ArrayList<>();
        }
        return pots.subList(1, pots.size());
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

    public Player getPlayerByUsername(String username) {
        return players.stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
    }
    
    public void moveToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (!players.get(currentPlayerIndex).isActive() || 
                 players.get(currentPlayerIndex).isHasFolded() || 
                 players.get(currentPlayerIndex).isAllIn());
                 
        // Just update the last activity time, but don't set the deadline
        // as we'll do that separately when needed
        updateCurrentPlayerActionDeadline();
        updateLastActivityTime();
    }
    
    /**
     * Updates the current player's action deadline
     */
    public void updateCurrentPlayerActionDeadline() {
        this.currentPlayerActionDeadline = Instant.now().plusSeconds(playerActionTimeoutSeconds);
    }
    
    /**
     * Updates the last activity timestamp
     */
    public void updateLastActivityTime() {
        this.lastActivityTime = LocalDateTime.now(ZoneOffset.UTC);
    }
    
    /**
     * Checks if the current player's action timeout has expired
     */
    public boolean isCurrentPlayerActionTimedOut() {
        if (this.currentPlayerActionDeadline == null) {
            return false;
        }
        return Instant.now().isAfter(this.currentPlayerActionDeadline);
    }
    
    /**
     * Checks if the current player is in the warning period before timeout
     * @param warningSeconds seconds before timeout when warning should appear
     * @return true if the player is in warning period
     */
    public boolean isCurrentPlayerInTimeoutWarningPeriod(int warningSeconds) {
        if (this.currentPlayerActionDeadline == null) {
            return false;
        }
        Instant warningThreshold = this.currentPlayerActionDeadline.minusSeconds(warningSeconds);
        return Instant.now().isAfter(warningThreshold) && !isCurrentPlayerActionTimedOut();
    }
    
    /**
     * Checks if the game has been idle for too long
     */
    public boolean isGameIdle() {
        if (this.lastActivityTime == null) {
            return false;
        }
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(this.lastActivityTime.plusMinutes(gameIdleTimeoutMinutes));
    }
    
    /**
     * Checks if game can auto-start when enough players join
     */
    public boolean canAutoStart() {
        return this.autoStart && 
               this.status == GameStatus.WAITING && 
               this.players.size() >= 2;
    }
    
    public void resetForNewHand() {
        deck = new Deck();
        communityCards.clear();
        pot = 0;
        pots.clear();
        pots.add(new Pot(0)); // Reset with a fresh main pot
        currentBettingRound = new BettingRound();
        currentBet = 0;
        lastActions.clear();
        players.forEach(Player::reset);
        // Rotate dealer position
        dealerPosition = (dealerPosition + 1) % players.size();
        
        // Set small and big blind positions
        int smallBlindPosition = (dealerPosition + 1) % players.size();
        int bigBlindPosition = (dealerPosition + 2) % players.size();
        
        smallBlindUserId = players.get(smallBlindPosition).getUsername();
        bigBlindUserId = players.get(bigBlindPosition).getUsername();
        
        currentPlayerIndex = (dealerPosition + 1) % players.size();
        
        updateLastActivityTime();
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
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
        while (!players.get(firstToActIdx).isActive() || 
               players.get(firstToActIdx).isHasFolded() || 
               players.get(firstToActIdx).isAllIn()) {
            firstToActIdx = (firstToActIdx + 1) % players.size();
        }
        currentPlayerIndex = firstToActIdx;
        
        // Set timeout for next player action
        updateCurrentPlayerActionDeadline();
        updateLastActivityTime();
    }
}