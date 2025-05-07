package com.example.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a game event record stored in the database
 */
@Data
@NoArgsConstructor
@Document(collection = "game_events")
public class GameEvent {
    @Id
    private String id;
    private String gameId;
    private LocalDateTime timestamp;
    private String eventType;
    private String eventData;
} 