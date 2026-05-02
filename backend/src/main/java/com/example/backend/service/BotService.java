package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.BotDifficulty;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BotService {
    private static final Logger logger = LoggerFactory.getLogger(BotService.class);

    private final GameLifecycleService lifecycleService;
    private final GameRepository gameRepository;
    private final BotActionService botActionService;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, Set<String>> activeBots = new ConcurrentHashMap<>();

    public Player addBot(String gameId, BotDifficulty difficulty) {
        String diff = difficulty != null ? difficulty.name() : "MEDIUM";
        String botUsername = "bot-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        // Join as bot (creates guest-style transient player)
        lifecycleService.joinGame(gameId, botUsername);

        // Re-fetch game to get the actual player with its persisted ID
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));

        Player actual = game.getPlayerByUsername(botUsername);
        if (actual == null) throw new RuntimeException("Bot player not found after join");

        actual.setBot(true);
        actual.setBotDifficulty(diff);
        gameRepository.save(game);

        activeBots.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(actual.getId());
        logger.info("Bot '{}' ({}) joined game {}", botUsername, diff, gameId);
        return actual;
    }

    public void removeBot(String gameId, String botPlayerId) {
        try {
            lifecycleService.leaveGame(gameId, botPlayerId);
        } catch (Exception e) {
            logger.warn("Could not remove bot {} from game {}: {}", botPlayerId, gameId, e.getMessage());
        }
        Set<String> bots = activeBots.get(gameId);
        if (bots != null) bots.remove(botPlayerId);
    }

    public void onGameUpdate(String gameId, Game game) {
        Set<String> bots = activeBots.getOrDefault(gameId, Collections.emptySet());
        if (bots.isEmpty()) return;

        for (String botId : bots) {
            int botIndex = findBotIndex(game, botId);
            if (botIndex < 0) continue;
            if (game.getCurrentPlayerIndex() == botIndex && isBotTurn(game, botIndex)) {
                long delayMs = 1200 + (long) (Math.random() * 1800);
                executor.schedule(() -> botActionService.takeTurn(gameId, botId), delayMs, TimeUnit.MILLISECONDS);
                logger.debug("Bot {} turn scheduled in {}ms in game {}", botId, delayMs, gameId);
            }
        }
    }

    public void cleanupGame(String gameId) {
        activeBots.remove(gameId);
    }

    private int findBotIndex(Game game, String botId) {
        var players = game.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (botId.equals(players.get(i).getId())) return i;
        }
        return -1;
    }

    private boolean isBotTurn(Game game, int idx) {
        Player p = game.getPlayers().get(idx);
        return p.isActive() && !p.isHasFolded() && !p.isSittingOut();
    }
}
