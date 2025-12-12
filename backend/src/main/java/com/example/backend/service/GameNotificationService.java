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
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class GameNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(GameNotificationService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send game update to all subscribers of a specific game
     * @param update The update to send
     */
    public void notifyGameUpdate(GameUpdate update) {
        try {
            // Add timestamp if not already set
            if (update.getTimestamp() == null) {
                update.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
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
     * @param playerId The player ID
     */
    public void notifyPlayerUpdate(GameUpdate update, String playerId) {
        try {
            // Add timestamp if not already set
            if (update.getTimestamp() == null) {
                update.setTimestamp(LocalDateTime.now(ZoneOffset.UTC));
            }
            // Create destination for specific player
            String destination = "/topic/game/" + update.getGameId() + "/player/" + playerId;
            messagingTemplate.convertAndSend(destination, update);
            
            // Log success at debug level
            logger.debug("Sent player update: gameId={}, playerId={}", update.getGameId(), playerId);
        } catch (Exception e) {
            logger.error("Failed to send player update notification: {}", e.getMessage(), e);
        }
    }
}