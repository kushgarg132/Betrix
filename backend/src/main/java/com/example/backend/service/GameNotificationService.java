package com.example.backend.service;

import com.example.backend.model.GameUpdate;
import com.example.backend.resolver.SubscriptionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class GameNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(GameNotificationService.class);

    public void notifyGameUpdate(GameUpdate update) {
        if (update.getTimestamp() == null) {
            update.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        }
        SubscriptionResolver.publishGameUpdate(update.getGameId(), update);
        logger.debug("Sent game update: type={}, gameId={}", update.getType(), update.getGameId());
    }

    public void notifyPlayerUpdate(GameUpdate update, String playerId) {
        if (update.getTimestamp() == null) {
            update.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        }
        SubscriptionResolver.publishPlayerUpdate(update.getGameId(), playerId, update);
        logger.debug("Sent player update: gameId={}, playerId={}", update.getGameId(), playerId);
    }
}
