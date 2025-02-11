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
    private String username;
    private Double amount;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String type;  // Example: "DEPOSIT", "WITHDRAWAL", "BET"
    private String description;
}
