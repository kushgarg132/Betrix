package com.example.backend.listener;

import com.example.backend.service.GameService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketSessionDisconnectListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionDisconnectListener.class);

    private final GameService gameService;

    // Map of sessionId -> PlayerSessionInfo
    private final Map<String, PlayerSessionInfo> sessionMap = new ConcurrentHashMap<>();

    // Pattern to match: /topic/game/{gameId}/player/{playerId}
    private static final Pattern PLAYER_TOPIC_PATTERN = Pattern.compile("^/topic/game/([^/]+)/player/([^/]+)$");

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        if (destination != null) {
            Matcher matcher = PLAYER_TOPIC_PATTERN.matcher(destination);
            if (matcher.matches()) {
                String gameId = matcher.group(1);
                String playerId = matcher.group(2);

                logger.info("Session {} subscribed to {} (Game: {}, Player: {})", sessionId, destination, gameId,
                        playerId);
                sessionMap.put(sessionId, new PlayerSessionInfo(gameId, playerId));
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        PlayerSessionInfo info = sessionMap.remove(sessionId);
        if (info != null) {
            logger.info("User Disconnected - Session: {}. Attempting to sit out game {} for player {}",
                    sessionId, info.gameId, info.playerId);
            try {
                // Sit out instead of leaving so they can reconnect. Time Bank will handle
                // stalls.
                gameService.sitOut(info.gameId, info.playerId);
            } catch (Exception e) {
                logger.error("Error gracefully handling user disconnect: {}", e.getMessage());
            }
        }
    }

    private static class PlayerSessionInfo {
        final String gameId;
        final String playerId;

        PlayerSessionInfo(String gameId, String playerId) {
            this.gameId = gameId;
            this.playerId = playerId;
        }
    }
}
