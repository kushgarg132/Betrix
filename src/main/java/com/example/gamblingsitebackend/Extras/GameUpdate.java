package com.example.gamblingsitebackend.Extras;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class GameUpdate {
    private String type;
    private Double pot;
    private List<String> handCards;
    private List<String> communityCards;
    private List<Player> players;
    private String message;
}
