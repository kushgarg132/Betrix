package com.example.gamblingsitebackend.GameController;

import com.example.gamblingsitebackend.Model.GameControlMessage;
import com.example.gamblingsitebackend.Service.PokerGameService;
import com.example.gamblingsitebackend.Extras.GameUpdate;
import com.example.gamblingsitebackend.Logics.PokerWinnerLogic;  // Logic to determine the winner
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PokerGameController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PokerGameService pokerGameService;

    @MessageMapping("/game-control")
    public void handleGameControl(@Payload GameControlMessage message) {
        switch (message.getAction()) {
            case "start-new-game" -> startNewGame(message.getTableId());
            case "check" -> handleCheck(message);
            case "bet" -> handleBet(message);
            case "raise" -> handleRaise(message);
            case "fold" -> handleFold(message);
            case "deal-community-cards" -> dealCommunityCards(message.getTableId());
            case "end-game" -> announceWinner(message);
            default -> System.err.println("Unknown action: " + message.getAction());
        }
    }

    private void startNewGame(String tableId) {
        GameUpdate gameUpdate = pokerGameService.startNewGame(tableId);
        messagingTemplate.convertAndSend("/topic/game-updates", gameUpdate);
    }

    private void handleCheck(GameControlMessage message) {
        System.out.println("Player Checked");
        GameUpdate gameUpdate = pokerGameService.placeCheck(message.getTableId(), message.getUsername());
        System.out.println("Game update: " + gameUpdate);
        messagingTemplate.convertAndSend("/topic/game-updates", gameUpdate);
    }

    private void handleBet(GameControlMessage message) {
        GameUpdate gameUpdate = pokerGameService.placeBet(message.getTableId(), message.getUsername(), Integer.parseInt(message.getDetails()));
        messagingTemplate.convertAndSend("/topic/game-updates", gameUpdate);
    }

    private void handleRaise(GameControlMessage message) {
        GameUpdate gameUpdate = pokerGameService.placeRaise(message.getTableId(), message.getUsername(), Integer.parseInt(message.getDetails()));
        messagingTemplate.convertAndSend("/topic/game-updates", gameUpdate);
    }

    private void handleFold(GameControlMessage message) {
        GameUpdate gameUpdate = pokerGameService.foldPlayer(message.getUsername());
        messagingTemplate.convertAndSend("/topic/game-updates", gameUpdate);
    }

    private void dealCommunityCards(String tableId) {
        GameUpdate gameUpdate =pokerGameService.dealCommunityCards(tableId);
        messagingTemplate.convertAndSend("/topic/game-updates", gameUpdate);
    }

    private void announceWinner(GameControlMessage message) {
        GameUpdate gameUpdate = pokerGameService.getPlayersForWinnerEvaluation(message.getTableId());

        messagingTemplate.convertAndSend("/topic/game-over", gameUpdate);
    }
}