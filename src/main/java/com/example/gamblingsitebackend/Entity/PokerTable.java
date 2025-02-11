package com.example.gamblingsitebackend.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "poker_tables")
public class PokerTable {

    @Id
    private String id;
    private String tableId;  // Unique identifier for each table
    private List<String> players;  // List of player usernames
    private Map<String, List<String>> playerHands;  // Cards dealt to each player
    private int currentTurn;  // Index of the player whose turn it is
    private int pot;  // Total pot amount
}
