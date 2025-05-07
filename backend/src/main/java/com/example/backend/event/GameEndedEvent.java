package com.example.backend.event;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Event fired when a game ends
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GameEndedEvent extends GameEvent {
    private Game game;
    private List<Player> winners;
    
    public GameEndedEvent(String gameId, Game game, List<Player> winners) {
        super(gameId);
        this.game = game;
        this.winners = winners;
    }
} 