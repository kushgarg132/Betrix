package com.example.gamblingsitebackend.Model;

import com.example.gamblingsitebackend.Extras.Player;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TableRequest {
    private String tableId;  // Unique identifier for each table
    private Player player;
}
