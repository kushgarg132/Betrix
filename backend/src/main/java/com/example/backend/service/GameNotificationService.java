package com.example.backend.service;

import com.example.backend.model.GameUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class GameNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(GameNotificationService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Send game update to all subscribers of a specific game
     * @param update The update to send
     */
    public void notifyGameUpdate(GameUpdate update) {
        try {
            // Add timestamp if not already set
            if (update.getTimestamp() == null) {
                update.setTimestamp(LocalDateTime.now());
            }
            
            // Send to topic
            String destination = "/topic/game/" + update.getGameId();
            messagingTemplate.convertAndSend(destination, update);
            
            // Log success at debug level
            logger.debug("Sent game update: type={}, gameId={}", update.getType(), update.getGameId());
        } catch (Exception e) {
            logger.error("Failed to send game update notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send update to a specific player
     * @param gameId The game ID
     * @param playerId The player ID
     * @param data The data to send
     */
    public void notifyPlayerUpdate(String gameId, String playerId, Object data) {
        try {
            // Create destination for specific player
            String destination = "/topic/game/" + gameId + "/player/" + playerId;
            
            // Create a player-specific update with timestamp
            GameUpdate update = GameUpdate.builder()
                    .gameId(gameId)
                    .type(GameUpdate.GameUpdateType.CARDS_DEALT)
                    .payload(data)
                    .timestamp(LocalDateTime.now())
                    .build();
                    
            messagingTemplate.convertAndSend(destination, update);
            
            // Log success at debug level
            logger.debug("Sent player update: gameId={}, playerId={}", gameId, playerId);
        } catch (Exception e) {
            logger.error("Failed to send player update notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Send bulk updates efficiently (e.g., for multiple players)
     * @param gameId The game ID
     * @param type Update type
     * @param playerUpdates Map of player IDs to their specific data
     */
    public void sendBulkPlayerUpdates(String gameId, GameUpdate.GameUpdateType type, 
                                     Map<String, Object> playerUpdates) {
        try {
            LocalDateTime timestamp = LocalDateTime.now();
            
            playerUpdates.forEach((playerId, data) -> {
                try {
                    String destination = "/topic/game/" + gameId + "/player/" + playerId;
                    
                    GameUpdate update = GameUpdate.builder()
                            .gameId(gameId)
                            .type(type)
                            .payload(data)
                            .timestamp(timestamp)
                            .build();
                            
                    messagingTemplate.convertAndSend(destination, update);
                } catch (Exception e) {
                    logger.error("Failed to send update to player {}: {}", playerId, e.getMessage());
                }
            });
            
            logger.debug("Sent bulk updates to {} players for game {}", playerUpdates.size(), gameId);
        } catch (Exception e) {
            logger.error("Failed to process bulk player updates: {}", e.getMessage(), e);
        }
    }
}