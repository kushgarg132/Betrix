package com.example.gamblingsitebackend.GameController;

import com.example.gamblingsitebackend.Model.PlayerPayload;
import com.example.gamblingsitebackend.Service.PokerGameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Controller
public class PokerGameController {

    private final SimpMessagingTemplate messagingTemplate;  // Used to send messages to specific topics
    private final PokerGameService pokerGameService;
    private int pot = 0;  // Example pot value, reset after each round

    public PokerGameController(SimpMessagingTemplate messagingTemplate, PokerGameService pokerGameService) {
        this.messagingTemplate = messagingTemplate;
        this.pokerGameService = pokerGameService;
    }

    @MessageMapping("/game-action")  // Handles messages sent to /app/game-action
    @SendTo("/topic/game-updates")  // Broadcasts responses to /topic/game-updates
    public String handleGameAction(@Payload String message) {
        System.out.println("Received game action: " + message);
        return "Action processed: " + message;
    }

    @MessageMapping("/bet")  // Client sends to /app/bet
    public void handleBet(@Payload String message) {
        int betAmount = 50;  // Example fixed bet amount
        pot += betAmount;
        System.out.println("Bet received: " + betAmount + ", Total pot: " + pot);

        // Broadcast pot update to all players
        messagingTemplate.convertAndSend("/topic/game-updates", "{\"type\":\"POT_UPDATE\", \"pot\":" + pot + "}");
    }

    @MessageMapping("/fold")  // Client sends to /app/fold
    public void handleFold(@Payload String message) {
        System.out.println("Player folded - "+ message);
        messagingTemplate.convertAndSend("/topic/game-updates", "{\"type\":\"PLAYER_ACTION\", \"message\":\"A player has folded.\"}");
    }

    @MessageMapping("/raise")  // Client sends to /app/raise
    public void handleRaise(@Payload String message) {
        int raiseAmount = 100;  // Example raise amount
        pot += raiseAmount;
        System.out.println("Raise received: " + raiseAmount + ", Total pot: " + pot);

        // Broadcast pot update to all players
        messagingTemplate.convertAndSend("/topic/game-updates", "{\"type\":\"POT_UPDATE\", \"pot\":" + pot + "}");
    }

    @MessageMapping("/community-cards")  // Client sends to /app/community-cards
    public void dealCommunityCards() {
        String[] communityCards = {"AS", "KH", "QD", "10C", "8S"};  // Example cards
        System.out.println("Dealing community cards: " + String.join(", ", communityCards));

        // Broadcast community cards to all players
        messagingTemplate.convertAndSend("/topic/game-updates", "{\"type\":\"COMMUNITY_CARDS\", \"cards\":" + toJsonArray(communityCards) + "}");
    }

    @MessageMapping("/end-game")
    public void announceWinner(@Payload PlayerPayload payload) {

        String winner = payload.getPlayer();  // Example logic to determine the winner
        messagingTemplate.convertAndSend("/topic/game-updates", "{\"type\":\"WINNER_ANNOUNCEMENT\", \"winner\":\"" + winner + "\"}");
    }


    // Helper method to format the cards as a JSON array
    private String toJsonArray(String[] cards) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < cards.length; i++) {
            json.append("\"").append(cards[i]).append("\"");
            if (i < cards.length - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }
}
