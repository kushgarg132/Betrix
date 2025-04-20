package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.service.GameService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class GameWebSocketController {

    @Autowired
    private GameService gameService;

    @MessageMapping("/game/{gameId}/bet")
    @SendTo("/topic/game/{gameId}")
    public Game processBet(String gameId, @RequestBody ActionPayload actionPayload) {
        return gameService.placeBet(gameId, actionPayload.playerId, actionPayload.amount);
    }

    @MessageMapping("/game/{gameId}/fold")
    @SendTo("/topic/game/{gameId}")
    public Game processFold(String gameId, String playerId) {
        return gameService.fold(gameId, playerId);
    }

    @Data
    public class ActionPayload {
        private String playerId;
        private double amount;
    }
}