package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.entity.GameEvent;
import com.example.backend.event.*;
import com.example.backend.repository.GameEventRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for replaying game events to reconstruct game state at any point
 */
@Service
@RequiredArgsConstructor
public class GameReplayService {
    private static final Logger logger = LoggerFactory.getLogger(GameReplayService.class);
    
    private final GameEventRepository gameEventRepository;

    // Stored events carry Jackson's serialized form of live game objects, which includes
    // computed getters (Player.isAllIn(), etc.) that have no matching field to deserialize into.
    // A copy so this does not loosen the shared, injected ObjectMapper used elsewhere.
    private final ObjectMapper objectMapper;

    private ObjectMapper replayMapper() {
        return objectMapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
    
    /**
     * Replay all events for a game to reconstruct its current state
     */
    public Game replayGame(String gameId) {
        logger.info("Replaying events for game: {}", gameId);
        
        try {
            List<GameEvent> events = gameEventRepository.findByGameIdOrderByTimestampAsc(gameId);
            Game game = null;
            
            for (GameEvent eventRecord : events) {
                game = applyEvent(game, eventRecord);
            }
            
            return game;
        } catch (Exception e) {
            logger.error("Error replaying game events: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to replay game events", e);
        }
    }
    
    /**
     * Replay events up to a specific point in time
     */
    public Game replayGameUntilEvent(String gameId, String eventId) {
        logger.info("Replaying events for game {} until event {}", gameId, eventId);
        
        try {
            List<GameEvent> allEvents = gameEventRepository.findByGameIdOrderByTimestampAsc(gameId);
            List<GameEvent> events = new ArrayList<>();
            
            // Collect events until we reach the target event
            boolean foundEvent = false;
            for (GameEvent event : allEvents) {
                events.add(event);
                if (event.getId().equals(eventId)) {
                    foundEvent = true;
                    break;
                }
            }
            
            if (!foundEvent) {
                throw new RuntimeException("Event not found: " + eventId);
            }
            
            // Replay events
            Game game = null;
            for (GameEvent eventRecord : events) {
                game = applyEvent(game, eventRecord);
            }
            
            return game;
        } catch (Exception e) {
            logger.error("Error replaying game events: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to replay game events", e);
        }
    }
    
    /**
     * Apply a single event to update game state
     */
    private Game applyEvent(Game game, GameEvent eventRecord) {
        String eventType = eventRecord.getEventType();
        // Spring Data Mongo hands eventData back as a Map (the field is typed Object), never as
        // JSON text, so convertValue is the right conversion here, not toString()+readValue.
        Object eventData = eventRecord.getEventData();
        ObjectMapper mapper = replayMapper();

        switch (eventType) {
            case "GameStartedEvent":
                return mapper.convertValue(eventData, GameStartedEvent.class).getGame();

            case "PlayerJoinedEvent":
                PlayerJoinedEvent playerJoinedEvent = mapper.convertValue(eventData, PlayerJoinedEvent.class);
                if (game == null) {
                    throw new IllegalStateException("Game not initialized before player joined");
                }
                game.getPlayers().add(playerJoinedEvent.getPlayer());
                return game;

            case "PlayerActionEvent":
                return mapper.convertValue(eventData, PlayerActionEvent.class).getGameState();

            case "RoundStartedEvent":
                return mapper.convertValue(eventData, RoundStartedEvent.class).getGame();

            case "GameEndedEvent":
                return mapper.convertValue(eventData, GameEndedEvent.class).getGame();

            case "CardsDealtEvent":
                // Cards dealt events don't directly change game state - return as is
                return game;

            default:
                logger.warn("Unknown event type: {}", eventType);
                return game;
        }
    }
} 