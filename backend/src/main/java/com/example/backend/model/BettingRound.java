package com.example.backend.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class BettingRound {
    private Map<String, Double> playerBets;
    private boolean roundComplete;
    
    public BettingRound() {
        this.playerBets = new HashMap<>();
        this.roundComplete = false;
    }
}