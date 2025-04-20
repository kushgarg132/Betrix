package com.example.backend.service;

import com.example.backend.entity.Game;

import java.util.List;

public interface GameService {
    List<Game> getAllGames();
    Game createGame();
    Game getGame(String gameId);
    Game joinGame(String gameId, String username);
    Game startNewHand(String gameId);
    Game placeBet(String gameId, String playerId, double amount);
    Game check(String gameId, String playerId);
    Game fold(String gameId, String playerId);
    void leaveGame(String gameId, String playerId);
    Game getGameForPlayer(String gameId, String playerId);
    boolean deleteGame(String gameId);
}