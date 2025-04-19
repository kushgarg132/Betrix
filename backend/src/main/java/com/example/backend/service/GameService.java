package com.example.backend.service;

import com.example.backend.model.Game;

public interface GameService {
    Game createGame();
    Game joinGame(String gameId, String playerId);
    Game placeBet(String gameId, String playerId, double amount);
    Game fold(String gameId, String playerId);
    Game dealCards(String gameId);
    Game nextRound(String gameId);
}