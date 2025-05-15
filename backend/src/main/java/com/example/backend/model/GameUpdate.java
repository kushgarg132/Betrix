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
        PLAYER_JOINED,
        GAME_STARTED,
        CARDS_DEALT,
        ROUND_STARTED,
        COMMUNITY_CARDS,
        PLAYER_ACTION,
        GAME_ENDED
    }
}
