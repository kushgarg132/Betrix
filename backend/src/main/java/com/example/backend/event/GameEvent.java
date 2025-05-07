package com.example.backend.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Base class for all game-related events
 */
@Data
@NoArgsConstructor
public abstract class GameEvent {
    private String gameId;
    private LocalDateTime timestamp;
    
    public GameEvent(String gameId) {
        this.gameId = gameId;
        this.timestamp = LocalDateTime.now();
    }
} 