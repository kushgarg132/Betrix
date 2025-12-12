package com.example.backend.event;

import com.example.backend.model.Player;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event fired when a player joins a game
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerJoinedEvent extends GameEvent {
    private Player player;
    
    public PlayerJoinedEvent(String gameId, Player player) {
        super(gameId);
        this.player = player;
    }
} 