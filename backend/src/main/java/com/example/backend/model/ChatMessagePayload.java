package com.example.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {
    private String senderId;
    private String senderName;
    private String message;
    private OffsetDateTime timestamp;
}
