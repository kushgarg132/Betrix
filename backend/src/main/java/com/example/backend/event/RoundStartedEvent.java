package com.example.backend.event;

import com.example.backend.entity.Game;
import com.example.backend.model.BettingRound;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Event fired when a new betting round starts
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoundStartedEvent extends GameEvent {
    private Game game;
    private BettingRound.RoundType roundType;
    
    public RoundStartedEvent(String gameId, Game game, BettingRound.RoundType roundType) {
        super(gameId);
        this.game = game;
        this.roundType = roundType;
    }
} 