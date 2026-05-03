package com.example.backend.resolver;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class GameSummaryResolver {

    @SchemaMapping(typeName = "GameSummary", field = "playerCount")
    public int playerCount(Game game) {
        return game.getPlayers() != null ? game.getPlayers().size() : 0;
    }

    @SchemaMapping(typeName = "GameSummary", field = "maxPlayers")
    public int maxPlayers(Game game) {
        return game.getMAX_PLAYERS();
    }

    @SchemaMapping(typeName = "GameSummary", field = "playerUsernames")
    public List<String> playerUsernames(Game game) {
        // Usernames are not exposed in the lobby for privacy.
        return List.of();
    }
}
