package com.example.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;

/**
 * Represents a game event record stored in the database
 */
@Data
@NoArgsConstructor
@Document(collection = "game_events")
@CompoundIndex(def = "{'gameId': 1, 'timestamp': 1}", name = "gameId_timestamp")
public class GameEvent {
    @Id
    private String id;
    private String gameId;
    private OffsetDateTime timestamp;
    private String eventType;
    private Object eventData;
} 