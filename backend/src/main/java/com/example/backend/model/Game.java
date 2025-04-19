
package com.example.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "games")
public class Game {
    @Id
    private String id;
    private List<Player> players;
    private List<Card> communityCards;
    private Deck deck;
    private double pot;
    private GameStatus status;
    private int currentPlayerIndex;
    private double currentBet;
    
    public Game() {
        this.players = new ArrayList<>();
        this.communityCards = new ArrayList<>();
        this.deck = new Deck();
        this.pot = 0.0;
        this.status = GameStatus.WAITING;
        this.currentBet = 0.0;
    }
    
    public enum GameStatus {
        WAITING, PRE_FLOP, FLOP, TURN, RIVER, FINISHED
    }
}