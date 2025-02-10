package com.example.gamblingsitebackend.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String walletId;
    private String username;
    private String type;  // DEPOSIT, WITHDRAW, GAME
    private double amount;
    private String description;
    private LocalDateTime timestamp;
}
