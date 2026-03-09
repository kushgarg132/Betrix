package com.example.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {
    private String senderId;
    private String senderName;
    private String message;
    private LocalDateTime timestamp;
}
