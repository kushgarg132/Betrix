package com.example.backend.controller;

import com.example.backend.entity.Game;
import com.example.backend.model.ActionPayload;
import com.example.backend.model.GameUpdate;
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

//    @MessageMapping("/game/{gameId}/player-game")
//    public void getPlayerCards(String gameId, @RequestBody ActionPayload actionPayload) {
//        return getPlayerGame(gameId, actionPayload);
//    }

    @MessageMapping("/game/{gameId}/action")
    public void processAction(String gameId, @RequestBody ActionPayload actionPayload) {
        if(actionPayload.getActionType() == ActionPayload.ActionType.CHECK) {
            processCheck(gameId, actionPayload);
        }else if(actionPayload.getActionType() == ActionPayload.ActionType.BET) {
            processBet(gameId, actionPayload);
        }else if(actionPayload.getActionType() == ActionPayload.ActionType.FOLD) {
            processFold(gameId, actionPayload);
        }else if(actionPayload.getActionType() == ActionPayload.ActionType.LEAVE) {
            leaveGame(gameId, actionPayload);
        }else{
            throw new IllegalArgumentException("Invalid action type");
        }
    }

    public void processCheck(String gameId, @RequestBody ActionPayload actionPayload) {
        gameService.check(gameId, actionPayload.getPlayerId());
    }

    public void processBet(String gameId, @RequestBody ActionPayload actionPayload) {
        gameService.placeBet(gameId, actionPayload.getPlayerId(), actionPayload.getAmount());
    }

    public void processFold(String gameId, @RequestBody ActionPayload actionPayload) {
        gameService.fold(gameId, actionPayload.getPlayerId());
    }

    public void leaveGame(String gameId, @RequestBody ActionPayload actionPayload) {
        gameService.leaveGame(gameId, actionPayload.getPlayerId());
    }

//    public ResponseEntity<Game> getPlayerGame(String gameId, @RequestBody ActionPayload actionPayload) {
//        Game game = gameService.getGameForPlayer(gameId, actionPayload.getPlayerId());
//        return ResponseEntity.ok(game);
//    }
}