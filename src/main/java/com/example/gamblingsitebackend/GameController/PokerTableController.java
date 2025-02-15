package com.example.gamblingsitebackend.GameController;

import com.example.gamblingsitebackend.Entity.PokerTable;
import com.example.gamblingsitebackend.Model.TableRequest;
import com.example.gamblingsitebackend.Service.PokerGameService;
import com.example.gamblingsitebackend.Service.PokerTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PokerTableController {
    private final SimpMessagingTemplate messagingTemplate;  // Used to send messages to specific topics
    private final PokerTableService pokerTableService;

    @MessageMapping("/request-tables-list")  // Handles messages sent to /app/game-action
    @SendTo("/topic/tables-list")  // Broadcasts responses to /topic/game-updates
    public List<PokerTable> handleAvailableTableRequest() {
        return pokerTableService.getAvailableTables();
    }

    @MessageMapping("/request-tables-join")  // Handles messages sent to /app/game-action
    @SendTo("/topic/tables-join")  // Broadcasts responses to /topic/game-updates
    public Object handleJoinTableRequest(@Payload TableRequest tableRequest) {
        System.out.println("Received join table request: " + tableRequest.getTableId());
        return pokerTableService.joinTable(tableRequest.getTableId() , tableRequest.getPlayer());
    }


}
