package com.example.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Player {
    private String id;
    private String username;
    private List<Card> hand;
    private double chips;
    private boolean isActive;
    private double currentBet;
    private boolean hasFolded;
    
    public Player(String id, String username, double initialChips) {
        this.id = id;
        this.username = username;
        this.chips = initialChips;
        this.hand = new ArrayList<>();
        this.isActive = true;
        this.currentBet = 0;
        this.hasFolded = false;
    }
    
    public void reset() {
        this.hand.clear();
        this.currentBet = 0;
        this.hasFolded = false;
    }
    
    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Cannot add null card to hand");
        }
        hand.add(card);
    }
    
    public void placeBet(double amount) {
        if (amount <= 0) {
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
    }
}