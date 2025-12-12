package com.example.backend.publisher;

import com.example.backend.event.GameEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Responsible for publishing game events to the event system
 */
@Component
@RequiredArgsConstructor
public class GameEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(GameEventPublisher.class);
    
    private final ApplicationEventPublisher publisher;
    
    public void publishEvent(GameEvent event) {
        logger.debug("Publishing event: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
} 