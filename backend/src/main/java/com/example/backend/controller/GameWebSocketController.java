package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.model.ActionPayload;
import com.example.backend.model.GameUpdate;
import com.example.backend.service.GameService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class GameWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketController.class);

    @Autowired
    private GameService gameService;

    @MessageMapping("/game/{gameId}/action")
    public void processAction(@Header("simpDestination") String destination, @RequestBody ActionPayload actionPayload) {
        logger.debug("Processing action: {} for game destination: {}", actionPayload.getActionType(), destination);

        // Extract gameId from the destination
        String gameId = extractGameIdFromDestination(destination);

        switch (actionPayload.getActionType()) {
            case CHECK:
                processCheck(gameId, actionPayload);
                break;
            case BET:
                processBet(gameId, actionPayload);
                break;
            case FOLD:
                processFold(gameId, actionPayload);
                break;
            case LEAVE:
                leaveGame(gameId, actionPayload);
                break;
            case SIT_OUT:
                processSitOut(gameId, actionPayload);
                break;
            case SIT_IN:
                processSitIn(gameId, actionPayload);
                break;
            default:
                logger.error("Invalid action type: {}", actionPayload.getActionType());
                throw new IllegalArgumentException("Invalid action type " + actionPayload.getActionType());
        }
    }

    private String extractGameIdFromDestination(String destination) {
        // Extract the gameId from the destination path (e.g.,
        // "/app/game/{gameId}/action")
        String[] parts = destination.split("/");
        return parts[parts.length - 2]; // Assuming the gameId is the third-to-last segment
    }

    public void processCheck(String gameId, @RequestBody ActionPayload actionPayload) {
        logger.info("Processing check action for player {} in game {}", actionPayload.getPlayerId(), gameId);
        gameService.check(gameId, actionPayload.getPlayerId());
    }

    public void processBet(String gameId, @RequestBody ActionPayload actionPayload) {
        logger.info("Processing bet action of {} for player {} in game {}",
                actionPayload.getAmount(), actionPayload.getPlayerId(), gameId);
        gameService.placeBet(gameId, actionPayload.getPlayerId(), actionPayload.getAmount());
    }

    public void processFold(String gameId, @RequestBody ActionPayload actionPayload) {
        logger.info("Processing fold action for player {} in game {}", actionPayload.getPlayerId(), gameId);
        gameService.fold(gameId, actionPayload.getPlayerId());
    }

    public void leaveGame(String gameId, @RequestBody ActionPayload actionPayload) {
        logger.info("Processing leave game action for player {} in game {}", actionPayload.getPlayerId(), gameId);
        gameService.leaveGame(gameId, actionPayload.getPlayerId());
    }

    public void processSitOut(String gameId, @RequestBody ActionPayload actionPayload) {
        logger.info("Processing sit out action for player {} in game {}", actionPayload.getPlayerId(), gameId);
        gameService.sitOut(gameId, actionPayload.getPlayerId());
    }

    public void processSitIn(String gameId, @RequestBody ActionPayload actionPayload) {
        logger.info("Processing sit in action for player {} in game {}", actionPayload.getPlayerId(), gameId);
        gameService.sitIn(gameId, actionPayload.getPlayerId());
    }

    @MessageMapping("/game/{gameId}/start")
    public void startGame(@Header("simpDestination") String destination) {
        logger.info("Received request to start game: {}", destination);

        // Extract gameId from the destination
        String gameId = extractGameIdFromDestination(destination);

        try {
            gameService.startNewHand(gameId);
            logger.debug("Game started successfully: {}", gameId);
        } catch (Exception e) {
            logger.error("Error starting game {}: {}", gameId, e.getMessage());
        }
    }
}