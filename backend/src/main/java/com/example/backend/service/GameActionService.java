package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.event.PlayerActionEvent;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.scheduler.GameScheduler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class GameActionService {
    private static final Logger logger = LoggerFactory.getLogger(GameActionService.class);

    private final GameRepository gameRepository;
    private final GameValidatorService gameValidatorService;
    private final BettingManager bettingManager;
    private final GameEventPublisher eventPublisher;
    private final GameScheduler gameScheduler;

    @Transactional
    public void placeBet(String gameId, String playerId, double amount) {
        logger.info("Player '{}' is placing a bet of {} in game '{}'", playerId, amount, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);

            gameValidatorService.validatePlayerTurn(game, playerId);
            gameValidatorService.validatePlayerBetAmount(game, player, amount);

            long usedTimeBankMs = gameScheduler.cancelPlayerTimeout(gameId, playerId);
            if (usedTimeBankMs > 0) {
                player.setTimeBankMs(Math.max(0, player.getTimeBankMs() - usedTimeBankMs));
            }

            bettingManager.placeBet(game, player, amount, null);

            String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.BET, amount, new Game(game)));

            bettingManager.handleCurrentBettingRound(game, playerId);

            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("server session pool is open") || e.getMessage().contains("state should be: open"))) {
                logger.debug("Could not place bet (database shutting down): {}", e.getMessage());
            } else {
                logger.error("Error placing bet: {}", e.getMessage());
                throw new RuntimeException("Failed to place bet", e);
            }
        }
    }

    @Transactional
    public void check(String gameId, String playerId) {
        logger.info("Player '{}' is checking in game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);

            gameValidatorService.validatePlayerTurn(game, playerId);

            long usedTimeBankMs = gameScheduler.cancelPlayerTimeout(gameId, playerId);
            if (usedTimeBankMs > 0) {
                player.setTimeBankMs(Math.max(0, player.getTimeBankMs() - usedTimeBankMs));
            }

            bettingManager.placeBet(game, player, 0, null);

            String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.CHECK, 0.0, new Game(game)));

            bettingManager.handleCurrentBettingRound(game, playerId);

            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("server session pool is open") || e.getMessage().contains("state should be: open"))) {
                logger.debug("Could not check (database shutting down): {}", e.getMessage());
            } else {
                logger.error("Error checking: {}", e.getMessage());
                throw new RuntimeException("Failed to check", e);
            }
        }
    }

    @Transactional
    public void fold(String gameId, String playerId) {
        logger.info("Player '{}' is folding in game '{}'", playerId, gameId);
        try {
            Game game = gameValidatorService.validateGameExists(gameId);
            Player player = gameValidatorService.validatePlayerExists(game, playerId);

            gameValidatorService.validatePlayerTurn(game, playerId);

            long usedTimeBankMs = gameScheduler.cancelPlayerTimeout(gameId, playerId);
            if (usedTimeBankMs > 0) {
                player.setTimeBankMs(Math.max(0, player.getTimeBankMs() - usedTimeBankMs));
            }

            bettingManager.fold(game, player);

            String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            gameScheduler.schedulePlayerTimeout(game.getId(), currentPlayerId);

            eventPublisher.publishEvent(new PlayerActionEvent(
                    gameId, player, PlayerActionEvent.ActionType.FOLD, null, new Game(game)));

            bettingManager.handleCurrentBettingRound(game, playerId);

            game.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            gameRepository.save(game);
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("server session pool is open") || e.getMessage().contains("state should be: open"))) {
                logger.debug("Could not fold (database shutting down): {}", e.getMessage());
            } else {
                logger.error("Error folding: {}", e.getMessage());
                throw new RuntimeException("Failed to fold", e);
            }
        }
    }
}
