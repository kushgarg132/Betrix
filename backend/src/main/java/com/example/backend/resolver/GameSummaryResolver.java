package com.example.backend.resolver;

import com.example.backend.entity.Game;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

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
}
