package com.example.gamblingsitebackend.Entity;

import com.example.gamblingsitebackend.Extras.Player;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.util.Pair;

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
    private List<Player> players;  // List of player usernames
    private Pair<Double, Double> blinds;
    private int currentTurn;  // Index of the player whose turn it is
    private int potLimit;  // Total pot amount
}
