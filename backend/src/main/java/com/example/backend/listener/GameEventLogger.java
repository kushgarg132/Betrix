package com.example.backend.listener;

import com.example.backend.entity.GameEvent;
import com.example.backend.entity.User;
import com.example.backend.event.*;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.Player;
import com.example.backend.repository.UserRepository;
import com.example.backend.resolver.SubscriptionResolver;
import com.example.backend.service.BotService;
import com.example.backend.service.GameNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GameEventLogger {
    private static final Logger logger = LoggerFactory.getLogger(GameEventLogger.class);

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final GameNotificationService notificationService;
    private final BotService botService;
    private final UserRepository userRepository;

    @EventListener
    public void logGameEvent(com.example.backend.event.GameEvent event) {
        try {
            GameEvent dbEvent = new GameEvent();
            dbEvent.setGameId(event.getGameId());
            dbEvent.setTimestamp(event.getTimestamp());
            dbEvent.setEventType(event.getClass().getSimpleName());
            dbEvent.setEventData(event);
            mongoTemplate.save(dbEvent, "game_events");
            logger.debug("Logged game event: {} for game {}", dbEvent.getEventType(), dbEvent.getGameId());
        } catch (Exception e) {
            logger.error("Error logging game event: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void onPlayerJoined(PlayerJoinedEvent event) {
        notify(event.getGameId(), GameUpdate.GameUpdateType.PLAYER_JOINED, event.getPlayer());
    }

    @EventListener
    public void onGameStarted(GameStartedEvent event) {
        notify(event.getGameId(), GameUpdate.GameUpdateType.GAME_STARTED, event.getGame());
        botService.onGameUpdate(event.getGameId(), event.getGame());
    }

    @EventListener
    public void onCardsDealt(CardsDealtEvent event) {
        if (event.getPlayerCards() != null) {
            // Deliver each player's private hand via player-specific sink
            event.getPlayerCards().forEach((playerId, cards) -> {
                GameUpdate update = GameUpdate.builder()
                        .gameId(event.getGameId())
                        .type(GameUpdate.GameUpdateType.CARDS_DEALT)
                        .payload(cards)
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .build();
                notificationService.notifyPlayerUpdate(update, playerId);
            });
        }
        if (event.getCommunityCards() != null && !event.getCommunityCards().isEmpty()) {
            notify(event.getGameId(), GameUpdate.GameUpdateType.COMMUNITY_CARDS, event.getCommunityCards());
        }
    }

    @EventListener
    public void onRoundStarted(RoundStartedEvent event) {
        notify(event.getGameId(), GameUpdate.GameUpdateType.ROUND_STARTED, event.getGame());
        botService.onGameUpdate(event.getGameId(), event.getGame());
    }

    @EventListener
    public void onPlayerAction(PlayerActionEvent event) {
        notify(event.getGameId(), GameUpdate.GameUpdateType.PLAYER_ACTION, event.getGameState());
        if (event.getGameState() != null) {
            botService.onGameUpdate(event.getGameId(), event.getGameState());
        }
    }

    @EventListener
    public void onGameEnded(GameEndedEvent event) {
        // GAME_ENDED means a hand ended, not the table closed — do NOT clean up bots or sinks here.
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("game", event.getGame());
        payload.put("winners", event.getWinners());
        payload.put("bestHand", event.getBestHand());

        notify(event.getGameId(), GameUpdate.GameUpdateType.GAME_ENDED, payload);
        logger.debug("Hand ended in game {}", event.getGameId());
        updatePlayerStats(event);
    }

    private void updatePlayerStats(GameEndedEvent event) {
        if (event.getGame() == null || event.getGame().getPlayers() == null) return;
        try {
            Set<String> winnerUsernames = event.getWinners() == null ? Set.of() :
                    event.getWinners().stream()
                            .map(Player::getUsername)
                            .collect(Collectors.toSet());

            for (Player player : event.getGame().getPlayers()) {
                if (player.isBot()) continue;
                userRepository.findByUsername(player.getUsername()).ifPresent(user -> {
                    user.setHandsPlayed(user.getHandsPlayed() + 1);
                    if (winnerUsernames.contains(player.getUsername())) {
                        user.setHandsWon(user.getHandsWon() + 1);
                        user.setNetProfit((int) (user.getNetProfit() + player.getLastWinAmount()));
                    }
                    userRepository.save(user);
                });
            }
        } catch (Exception e) {
            logger.error("Error updating player stats: {}", e.getMessage());
        }
    }

    private void notify(String gameId, GameUpdate.GameUpdateType type, Object payload) {
        notificationService.notifyGameUpdate(GameUpdate.builder()
                .gameId(gameId)
                .type(type)
                .payload(payload)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }
}
