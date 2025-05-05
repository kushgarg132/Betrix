package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.BlindPayload;
import com.example.backend.model.GameUpdate;

import java.util.List;

public interface GameService {
    List<Game> getAllGames();
    void createGame(BlindPayload payload);
    Game getGame(String gameId);
    Game joinGame(String gameId, String username);
    void startNewHand(String gameId);
    void placeBet(String gameId, String playerId, double amount);
    void check(String gameId, String playerId);
    void fold(String gameId, String playerId);
    void leaveGame(String gameId, String playerId);
    Game getGameForPlayer(String gameId, String playerId);
    Game getGameForPlayer(Game game, String playerId);
    boolean deleteGame(String gameId);
}