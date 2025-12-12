package com.example.backend.event;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
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
    private Player player;
    private ActionType actionType;
    private Double amount;
    private Game gameState;
    
    public enum ActionType {
        BET, FOLD, CHECK, LEAVE
    }
    
    public PlayerActionEvent(String gameId, Player player, ActionType actionType, Double amount, Game gameState) {
        super(gameId);
        this.player = player;
        this.actionType = actionType;
        this.amount = amount;
        this.gameState = gameState;
    }
} 