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
    private final GameLocks locks;

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
        return locks.run(gameId, () -> lifecycleService.joinGame(gameId, username));
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
        locks.run(gameId, () -> lifecycleService.leaveGame(gameId, playerId));
    }

    @Override
    public void sitOut(String gameId, String playerId) {
        locks.run(gameId, () -> lifecycleService.sitOut(gameId, playerId));
    }

    @Override
    public void sitIn(String gameId, String playerId) {
        locks.run(gameId, () -> lifecycleService.sitIn(gameId, playerId));
    }

    @Override
    public boolean deleteGame(String gameId) {
        return locks.run(gameId, () -> lifecycleService.deleteGame(gameId));
    }

    @Override
    public void startNewHand(String gameId) {
        locks.run(gameId, () -> handService.startNewHand(gameId));
    }

    @Override
    public void executeAllInAction(String gameId) {
        locks.run(gameId, () -> handService.executeAllInAction(gameId));
    }

    @Override
    public void placeBet(String gameId, String playerId, long amount) {
        locks.run(gameId, () -> actionService.placeBet(gameId, playerId, amount));
    }

    @Override
    public void check(String gameId, String playerId) {
        locks.run(gameId, () -> actionService.check(gameId, playerId));
    }

    @Override
    public void fold(String gameId, String playerId) {
        locks.run(gameId, () -> actionService.fold(gameId, playerId));
    }
}
