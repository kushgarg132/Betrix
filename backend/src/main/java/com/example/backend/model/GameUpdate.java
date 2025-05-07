package com.example.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameUpdate {
    private String gameId;
    private GameUpdateType type;
    private Object payload;
    private LocalDateTime timestamp;

    public enum GameUpdateType {
        GAME_STARTED,
        PLAYER_JOINED,
        PLAYER_LEFT,
        PLAYER_TURN,
        PLAYER_CHECKED,
        PLAYER_BET,
        PLAYER_FOLDED,
        CARDS_DEALT,
        ROUND_STARTED,
        GAME_ENDED
    }
}
