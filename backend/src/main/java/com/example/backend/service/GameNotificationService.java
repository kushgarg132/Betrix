package com.example.backend.service;

import com.example.backend.model.GameUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GameNotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void notifyGameUpdate(GameUpdate update) {
        messagingTemplate.convertAndSend("/topic/game/" + update.getGameId(), update);
    }
}