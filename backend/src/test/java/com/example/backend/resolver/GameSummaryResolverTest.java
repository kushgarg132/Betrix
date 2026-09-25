package com.example.backend.resolver;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lobby list (GameSummary) does not expose other players' usernames, so the frontend has no
 * way to know "is this my table" itself. isYourGame answers exactly that one question, resolved
 * server-side from the caller's own identity, without leaking anyone else's.
 */
class GameSummaryResolverTest {

    private final GameSummaryResolver resolver = new GameSummaryResolver();

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x", List.of()));
    }

    private static Game gameWith(String... usernames) {
        Game g = new Game(10, 20);
        for (String u : usernames) {
            g.getPlayers().add(new Player(u, u, 1000));
        }
        return g;
    }

    @Test
    void trueWhenTheCallerIsSeatedAtTheTable() {
        loginAs("alice");
        assertTrue(resolver.isYourGame(gameWith("bob", "alice")));
    }

    @Test
    void falseWhenTheCallerIsNotSeatedAtTheTable() {
        loginAs("mallory");
        assertFalse(resolver.isYourGame(gameWith("bob", "alice")));
    }

    @Test
    void falseForAnAnonymousViewerBrowsingTheLobby() {
        SecurityContextHolder.clearContext();
        assertFalse(resolver.isYourGame(gameWith("bob", "alice")));
    }
}
