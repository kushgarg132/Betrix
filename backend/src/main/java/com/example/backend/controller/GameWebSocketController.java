package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    @Autowired
    private GameService gameService;

    @MessageMapping("/game/{gameId}/bet")
    @SendTo("/topic/game/{gameId}")
    public Game processBet(String gameId, String playerId, double amount) {
        return gameService.placeBet(gameId, playerId, amount);
    }

    @MessageMapping("/game/{gameId}/fold")
    @SendTo("/topic/game/{gameId}")
    public Game processFold(String gameId, String playerId) {
        return gameService.fold(gameId, playerId);
    }
}