package com.example.backend.resolver;

import com.example.backend.entity.Game;
import com.example.backend.security.CurrentUser;
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

    /** Lets the lobby show "Your table" / "Return to Table" without ever exposing who else is
     * seated -- GameSummary carries no player usernames, so this is the only way the caller can
     * find out. False for an anonymous viewer browsing the public lobby list. */
    @SchemaMapping(typeName = "GameSummary", field = "isYourGame")
    public boolean isYourGame(Game game) {
        String username = CurrentUser.username();
        return username != null && game.hasPlayer(username);
    }
}
