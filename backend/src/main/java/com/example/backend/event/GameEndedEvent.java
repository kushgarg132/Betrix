package com.example.backend.event;

import com.example.backend.entity.Game;
import com.example.backend.model.HandResult;
import com.example.backend.model.Player;
import com.example.backend.service.HandEvaluator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Event fired when a game ends
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GameEndedEvent extends GameEvent {
    private Game game;
    private List<Player> winners;
    private HandResult bestHand;

    public GameEndedEvent(String gameId, Game game, List<Player> winners, HandResult bestHand) {
        super(gameId);
        this.game = game;
        this.winners = winners;
        this.bestHand = bestHand != null ? new HandResult(bestHand) : null;
    }
} 