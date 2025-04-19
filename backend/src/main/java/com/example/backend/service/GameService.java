package com.example.backend.service;

import com.example.backend.entity.Game;

public interface GameService {
    Game createGame();
    Game getGame(String gameId);
    Game joinGame(String gameId, String playerId);
    Game startNewHand(String gameId);
    Game placeBet(String gameId, String playerId, double amount);
    Game check(String gameId, String playerId);
    Game fold(String gameId, String playerId);
    Game leaveGame(String gameId, String playerId);
    boolean deleteGame(String gameId);
}