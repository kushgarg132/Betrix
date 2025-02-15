package com.example.gamblingsitebackend.Model;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GameControlMessage {
    private String action;
    private String tableId;
    private String details;
    private String username;
}