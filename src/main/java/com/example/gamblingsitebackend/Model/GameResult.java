package com.example.gamblingsitebackend.Model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameResult {

    private String outcome;
    private double walletBalance;
}
