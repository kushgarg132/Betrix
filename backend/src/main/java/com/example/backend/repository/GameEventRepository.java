package com.example.backend.repository;

import com.example.backend.entity.GameEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for accessing game event records
 */
@Repository
public interface GameEventRepository extends MongoRepository<GameEvent, String> {
    
    /**
     * Find all events for a specific game
     */
    List<GameEvent> findByGameIdOrderByTimestampAsc(String gameId);
    
    /**
     * Find events of a specific type for a game
     */
    List<GameEvent> findByGameIdAndEventTypeOrderByTimestampAsc(String gameId, String eventType);
} 