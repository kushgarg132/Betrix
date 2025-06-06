package com.example.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Player {
    private String id;
    private String name;
    private String username;
    private List<Card> hand;
    private double chips;
    private boolean isActive;
    private double currentBet;
    private boolean hasFolded;
    private double lastWinAmount; // Amount won in the last hand
    private HandResult bestHand; // Best hand for the player
    
    public Player(String name, String username, double initialChips) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.username = username;
        this.chips = initialChips;
        this.hand = new ArrayList<>();
        this.isActive = true;
        this.currentBet = 0;
        this.hasFolded = false;
        this.lastWinAmount = 0;
        this.bestHand = null;
    }

    public Player(Player player) {
        this.id = player.getId() != null ? player.getId() : UUID.randomUUID().toString();
        this.name = player.getName();
        this.username = player.getUsername();
        this.chips = player.getChips();
        this.hand = player.getHand() != null ? new ArrayList<>(player.getHand()) : new ArrayList<>();
        this.isActive = player.isActive();
        this.currentBet = player.getCurrentBet();
        this.hasFolded = player.isHasFolded();
        this.lastWinAmount = player.getLastWinAmount();
        this.bestHand = player.getBestHand();
    }

    public void hideDetails(){
        this.hand = new ArrayList<>();
        this.id = null;
    }

    public void reset() {
        this.hand.clear();
        this.currentBet = 0;
        this.hasFolded = false;
        this.isActive = true;
        this.lastWinAmount = 0;
        this.bestHand = null;
    }
    
    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot add null card to hand");
        }
        hand.add(card);
    }
    
    public void placeBet(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Bet amount must be positive");
        }
        if (amount > chips) {
            throw new IllegalArgumentException("Insufficient chips");
        }
        chips -= amount;
        currentBet += amount;
    }
    
    public void awardPot(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Award amount cannot be negative");
        }
        chips += amount;
        lastWinAmount = amount;
    }
    
    /**
     * Check if the player is all-in (has no chips left)
     * @return true if player has no chips left
     */
    public boolean isAllIn() {
        return isActive() && !hasFolded && chips == 0;
    }
}