package com.example.gamblingsitebackend.Entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "wallets")
public class Wallet {
    @Id
    private String id;
    private String username;
    private double walletBalance;
}
