package com.example.backend.model;

import lombok.Data;

@Data
public class GameUpdate {
    private String gameId;
    private GameUpdateType type;
    private Object payload;
    
    public enum GameUpdateType {
        PLAYER_JOINED,
        PLAYER_BET,
        PLAYER_FOLDED,
        CARDS_DEALT,
        ROUND_STARTED,
        GAME_ENDED
    }
}