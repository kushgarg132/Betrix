package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.BlindPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private final GameLifecycleService lifecycleService;
    private final GameHandService handService;
    private final GameActionService actionService;

    @Override
    public List<Game> getAllGames() {
        return lifecycleService.getAllGames();
    }

    @Override
    public void createGame(BlindPayload payload) {
        lifecycleService.createGame(payload);
    }

    @Override
    public Game joinGame(String gameId, String username) {
        return lifecycleService.joinGame(gameId, username);
    }

    @Override
    public Game getGameForPlayer(String gameId, String playerId) {
        return lifecycleService.getGameForPlayer(gameId, playerId);
    }

    @Override
    public Game getGameForPlayer(Game game, String playerId) {
        return lifecycleService.getGameForPlayer(game, playerId);
    }

    @Override
    public void leaveGame(String gameId, String playerId) {
        lifecycleService.leaveGame(gameId, playerId);
    }

    @Override
    public void sitOut(String gameId, String playerId) {
        lifecycleService.sitOut(gameId, playerId);
    }

    @Override
    public void sitIn(String gameId, String playerId) {
        lifecycleService.sitIn(gameId, playerId);
    }

    @Override
    public boolean deleteGame(String gameId) {
        return lifecycleService.deleteGame(gameId);
    }

    @Override
    public void startNewHand(String gameId) {
        handService.startNewHand(gameId);
    }

    @Override
    public void executeAllInAction(String gameId) {
        handService.executeAllInAction(gameId);
    }

    @Override
    public void placeBet(String gameId, String playerId, double amount) {
        actionService.placeBet(gameId, playerId, amount);
    }

    @Override
    public void check(String gameId, String playerId) {
        actionService.check(gameId, playerId);
    }

    @Override
    public void fold(String gameId, String playerId) {
        actionService.fold(gameId, playerId);
    }
}
