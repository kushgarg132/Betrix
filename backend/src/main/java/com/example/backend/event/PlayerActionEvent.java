package com.example.backend.event;

import com.example.backend.entity.Game;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

/**
 * Event fired when a player takes an action (bet, fold, check)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerActionEvent extends GameEvent {
    private String playerId;
    private ActionType actionType;
    private Double amount;
    private Game gameState;
    
    public enum ActionType {
        BET, FOLD, CHECK, LEAVE
    }
    
    public PlayerActionEvent(String gameId, String playerId, ActionType actionType, Double amount, Game gameState) {
        super(gameId);
        this.playerId = playerId;
        this.actionType = actionType;
        this.amount = amount;
        this.gameState = gameState;
    }
} 