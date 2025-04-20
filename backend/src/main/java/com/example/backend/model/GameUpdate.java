package com.example.backend.model;

import lombok.Data;

@Data
public class GameUpdate {
    private String gameId;
    private GameUpdateType type;
    private Object payload;

    public enum GameUpdateType {
        GAME_STARTED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        PLAYER_TURN,
        PLAYER_BET,
        PLAYER_FOLDED,
        CARDS_DEALT,
        ROUND_STARTED,
        ROUND_COMPLETE,
        GAME_ENDED
    }
}
