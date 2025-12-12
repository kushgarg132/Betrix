package com.example.backend.controller;

import com.example.backend.entity.GameEvent;
import com.example.backend.repository.GameEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for accessing game event history
 */
@RestController
@RequestMapping("/api/game-events")
@RequiredArgsConstructor
public class GameEventController {
    private static final Logger logger = LoggerFactory.getLogger(GameEventController.class);
    
    private final GameEventRepository gameEventRepository;
    
    /**
     * Get all events for a game
     */
    @GetMapping("/{gameId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GameEvent>> getGameEvents(@PathVariable String gameId) {
        logger.info("Fetching events for game: {}", gameId);
        try {
            List<GameEvent> events = gameEventRepository.findByGameIdOrderByTimestampAsc(gameId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Error fetching game events: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
    
    /**
     * Get events of a specific type for a game
     */
    @GetMapping("/{gameId}/{eventType}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GameEvent>> getGameEventsByType(
            @PathVariable String gameId,
            @PathVariable String eventType) {
        logger.info("Fetching {} events for game: {}", eventType, gameId);
        try {
            List<GameEvent> events = gameEventRepository.findByGameIdAndEventTypeOrderByTimestampAsc(gameId, eventType);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Error fetching game events by type: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }
} 