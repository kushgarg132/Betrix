package com.example.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
public class BettingRound {
    private Map<String, Long> bets = new HashMap<>();
    private RoundType roundType;
    private boolean roundComplete;
    
    public enum RoundType {
        PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    public BettingRound() {
        this.roundComplete = false;
    }
}