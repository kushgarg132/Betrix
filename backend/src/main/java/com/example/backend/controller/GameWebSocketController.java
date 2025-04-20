package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.service.GameService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class GameWebSocketController {

    @Autowired
    private GameService gameService;

    @MessageMapping("/game/{gameId}/check")
    @SendTo("/topic/game/{gameId}")
    public Game processCheck(String gameId, @RequestBody ActionPayload actionPayload) {
        return gameService.check(gameId, actionPayload.playerId);
    }

    @MessageMapping("/game/{gameId}/bet")
    @SendTo("/topic/game/{gameId}")
    public Game processBet(String gameId, @RequestBody ActionPayload actionPayload) {
        return gameService.placeBet(gameId, actionPayload.playerId, actionPayload.amount);
    }

    @MessageMapping("/game/{gameId}/fold")
    @SendTo("/topic/game/{gameId}")
    public Game processFold(String gameId, @RequestBody ActionPayload actionPayload) {
        return gameService.fold(gameId, actionPayload.playerId);
    }

    @MessageMapping("/game/{gameId}/leave")
    @SendTo("/topic/game/{gameId}")
    public ResponseEntity.BodyBuilder leaveGame(String gameId, @RequestBody ActionPayload actionPayload) {
        gameService.leaveGame(gameId, actionPayload.playerId);
        return ResponseEntity.ok();
    }

    @Data
    public class ActionPayload {
        private String playerId;
        private double amount;
    }
}