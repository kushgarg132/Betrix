package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import com.example.backend.scheduler.GameScheduler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turn timers, the bot registry and the all-in run-out are held in memory, so after a restart a hand
 * that was in progress would sit there forever. On startup, put them back for every stored game.
 * Waiting games need nothing: the scheduler's periodic job already starts them.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.recover-on-startup", havingValue = "true", matchIfMissing = true)
public class GameRecovery {
    private static final Logger logger = LoggerFactory.getLogger(GameRecovery.class);

    private static final List<Game.GameStatus> BETTING = List.of(Game.GameStatus.PRE_FLOP_BETTING,
            Game.GameStatus.FLOP_BETTING, Game.GameStatus.TURN_BETTING, Game.GameStatus.RIVER_BETTING);

    private final GameRepository gameRepository;
    private final GameScheduler gameScheduler;
    private final BotService botService;
    private final GameLocks locks;

    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        try {
            List<Game> games = gameRepository.findAll();
            games.forEach(g -> recoverGame(g.getId()));
            logger.info("Startup recovery checked {} games", games.size());
        } catch (Exception e) {
            logger.error("Startup recovery could not list games: {}", e.getMessage());
        }
    }

    private void recoverGame(String gameId) {
        try {
            locks.run(gameId, () -> {
                Game game = gameRepository.findById(gameId).orElse(null);
                if (game == null) {
                    return;
                }
                botService.registerExistingBots(game);
                if (!BETTING.contains(game.getStatus())) {
                    return;
                }
                if (everyoneStillInIsAllIn(game)) {
                    gameScheduler.scheduleAllInAction(gameId);
                    return;
                }
                if (game.getCurrentPlayerIndex() >= 0 && game.getCurrentPlayerIndex() < game.getPlayers().size()) {
                    gameScheduler.schedulePlayerTimeout(gameId, game.getPlayers().get(game.getCurrentPlayerIndex()).getId());
                    botService.onGameUpdate(gameId, game);
                }
                logger.info("Recovered game {} in {}", gameId, game.getStatus());
            });
        } catch (Exception e) {
            logger.error("Could not recover game {}: {}", gameId, e.getMessage());
        }
    }

    /** Same rule as BettingManager: at most one player left who can still act. */
    private static boolean everyoneStillInIsAllIn(Game game) {
        List<Player> inHand = game.getPlayers().stream().filter(p -> p.isActive() && !p.isHasFolded()).toList();
        return inHand.size() > 1 && inHand.stream().filter(Player::isAllIn).count() >= inHand.size() - 1;
    }
}
