package com.example.backend.event;

import com.example.backend.model.Card;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * Event fired when cards are dealt to players or community
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CardsDealtEvent extends GameEvent {
    public enum DealType {
        PLAYER_CARDS, FLOP, TURN, RIVER
    }
    
    private DealType dealType;
    private List<Card> communityCards;
    private Map<String, List<Card>> playerCards;
    
    public CardsDealtEvent(String gameId, DealType dealType, List<Card> communityCards) {
        super(gameId);
        this.dealType = dealType;
        this.communityCards = communityCards;
    }
    
    public CardsDealtEvent(String gameId, Map<String, List<Card>> playerCards) {
        super(gameId);
        this.dealType = DealType.PLAYER_CARDS;
        this.playerCards = playerCards;
    }
} 