package com.example.backend.listener;

import com.example.backend.entity.Game;
import com.example.backend.event.*;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.Player;
import com.example.backend.service.GameNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens for game events and sends WebSocket notifications
 */
@Component
@RequiredArgsConstructor
public class GameWebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketEventListener.class);
    
    private final GameNotificationService notificationService;
    
    @EventListener
    public void handlePlayerJoinedEvent(PlayerJoinedEvent event) {
        logger.debug("Handling PlayerJoinedEvent for gameId: {}", event.getGameId());
        
        // Create a sanitized copy of player data without sensitive info
        Player player = new Player(event.getPlayer());
        player.hideDetails();
        
        GameUpdate update = GameUpdate.builder()
                .gameId(event.getGameId())
                .type(GameUpdate.GameUpdateType.PLAYER_JOINED)
                .payload(player)
                .timestamp(event.getTimestamp())
                .build();
                
        notificationService.notifyGameUpdate(update);
    }
    
    @EventListener
    public void handleGameStartedEvent(GameStartedEvent event) {
        logger.debug("Handling GameStartedEvent for gameId: {}", event.getGameId());
        
        // Create sanitized game state for all players
        Game game = new Game(event.getGame());
        game.getPlayers().forEach(player -> {
            player.setId(null);
            player.setHand(new ArrayList<>());
        });
        
        GameUpdate update = GameUpdate.builder()
                .gameId(event.getGameId())
                .type(GameUpdate.GameUpdateType.GAME_STARTED)
                .payload(game)
                .timestamp(event.getTimestamp())
                .build();
                
        notificationService.notifyGameUpdate(update);
    }
    
    @EventListener
    public void handlePlayerActionEvent(PlayerActionEvent event) {
        logger.debug("Handling PlayerActionEvent for gameId: {}", event.getGameId());

        Player player = new Player(event.getPlayer());
        player.hideDetails();
        Map<String, Object> actionDetails = new HashMap<>();
        actionDetails.put("action", event.getActionType());
        actionDetails.put("player", player);
        actionDetails.put("currentPlayerIndex", event.getGameState().getCurrentPlayerIndex());
        actionDetails.put("pot" , event.getGameState().getPot());
        if (event.getAmount() != null) {
            actionDetails.put("amount", event.getAmount());
        }
        
        GameUpdate update = GameUpdate.builder()
                .gameId(event.getGameId())
                .type(GameUpdate.GameUpdateType.PLAYER_ACTION)
                .payload(event.getGameState())
                .timestamp(event.getTimestamp())
                .build();
                
        notificationService.notifyGameUpdate(update);
    }
    
    @EventListener
    public void handleCardsDealtEvent(CardsDealtEvent event) {
        logger.debug("Handling CardsDealtEvent for gameId: {}", event.getGameId());
        
        if (event.getDealType() != CardsDealtEvent.DealType.PLAYER_CARDS) {
            // For community cards, send to all players
            GameUpdate update = GameUpdate.builder()
                    .gameId(event.getGameId())
                    .type(GameUpdate.GameUpdateType.COMMUNITY_CARDS)
                    .payload(event.getCommunityCards())
                    .timestamp(event.getTimestamp())
                    .build();
                    
            notificationService.notifyGameUpdate(update);
        } else if (event.getPlayerCards() != null) {
            // For player cards, send to individual players
            event.getPlayerCards().forEach((playerId, cards) -> {
                GameUpdate update = GameUpdate.builder()
                        .gameId(event.getGameId())
                        .type(GameUpdate.GameUpdateType.CARDS_DEALT)
                        .payload(cards)
                        .timestamp(event.getTimestamp())
                        .build();

                notificationService.notifyPlayerUpdate(update, playerId);
            });
        }
    }
    
    @EventListener
    public void handleRoundStartedEvent(RoundStartedEvent event) {
        logger.debug("Handling RoundStartedEvent for gameId: {}", event.getGameId());

        // Create sanitized game state for all players
        Game game = new Game(event.getGame());
        game.getPlayers().forEach(Player::hideDetails);

        GameUpdate update = GameUpdate.builder()
                .gameId(event.getGameId())
                .type(GameUpdate.GameUpdateType.ROUND_STARTED)
                .payload(game)
                .timestamp(event.getTimestamp())
                .build();
                
        notificationService.notifyGameUpdate(update);
    }
    
    @EventListener
    public void handleGameEndedEvent(GameEndedEvent event) {
        logger.debug("Handling GameEndedEvent for gameId: {}", event.getGameId());
        
        Map<String, Object> gameEndDetails = new HashMap<>();
        gameEndDetails.put("game", event.getGame());
        gameEndDetails.put("winners", event.getWinners());
        gameEndDetails.put("bestHand" , event.getBestHand());
        
        GameUpdate update = GameUpdate.builder()
                .gameId(event.getGameId())
                .type(GameUpdate.GameUpdateType.GAME_ENDED)
                .payload(gameEndDetails)
                .timestamp(event.getTimestamp())
                .build();
                
        notificationService.notifyGameUpdate(update);
    }
} 