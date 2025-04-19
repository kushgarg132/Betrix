
package com.example.backend.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class Player {
    private String id;
    private String username;
    private List<Card> hand;
    private double chips;
    private boolean isActive;
    
    public Player(String id, String username, double initialChips) {
        this.id = id;
        this.username = username;
        this.chips = initialChips;
        this.hand = new ArrayList<>();
        this.isActive = true;
    }
}