package com.example.backend.listener;

import com.example.backend.entity.GameEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Listens for game events and logs them to the database
 */
@Component
@RequiredArgsConstructor
public class GameEventLogger {
    private static final Logger logger = LoggerFactory.getLogger(GameEventLogger.class);

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    public void logGameEvent(com.example.backend.event.GameEvent event) {
        try {
            // Create a database event record
            GameEvent dbEvent = new GameEvent();
            dbEvent.setGameId(event.getGameId());
            dbEvent.setTimestamp(event.getTimestamp());
            dbEvent.setEventType(event.getClass().getSimpleName());
            dbEvent.setEventData(event);
            
            // Save to database
            mongoTemplate.save(dbEvent, "game_events");
            
            logger.debug("Logged game event: {} for game {}", 
                dbEvent.getEventType(), 
                dbEvent.getGameId());
        } catch (Exception e) {
            logger.error("Error logging game event: {}", e.getMessage(), e);
        }
    }
} 