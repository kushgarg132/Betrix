package com.example.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class Pot {
    private long amount;
    private Set<String> eligiblePlayerIds;
    
    public Pot(long amount) {
        this.amount = amount;
        this.eligiblePlayerIds = new HashSet<>();
    }
    
    public Pot(long amount, Set<String> eligiblePlayerIds) {
        this.amount = amount;
        this.eligiblePlayerIds = new HashSet<>(eligiblePlayerIds);
    }
    
    public void addAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative amount to pot");
        }
        this.amount += amount;
    }
    
    public void addEligiblePlayer(String playerId) {
        this.eligiblePlayerIds.add(playerId);
    }
    
    public boolean isPlayerEligible(String playerId) {
        return eligiblePlayerIds.contains(playerId);
    }
} 