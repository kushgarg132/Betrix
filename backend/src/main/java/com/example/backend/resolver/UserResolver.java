package com.example.backend.resolver;

import com.example.backend.entity.User;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
public class UserResolver {

    @SchemaMapping(typeName = "User", field = "winRate")
    public float winRate(User user) {
        if (user.getHandsPlayed() == 0) return 0.0f;
        return (float) user.getHandsWon() / user.getHandsPlayed() * 100.0f;
    }
}
