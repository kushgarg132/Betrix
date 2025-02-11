package com.example.gamblingsitebackend.Extras;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlayerAction {
    private String username;
    private String action;  // "check", "bet", "fold", etc.
}
