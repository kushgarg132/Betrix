package com.example.backend.event;

import com.example.backend.entity.Game;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event fired when a game starts
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GameStartedEvent extends GameEvent {
    private Game game;
    
    public GameStartedEvent(String gameId, Game game) {
        super(gameId);
        this.game = game;
    }
} 