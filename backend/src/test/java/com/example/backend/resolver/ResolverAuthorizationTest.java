package com.example.backend.resolver;

import com.example.backend.config.WebSocketAuthInterceptor;
import com.example.backend.entity.Game;
import com.example.backend.model.BotDifficulty;
import com.example.backend.model.ChatMessagePayload;
import com.example.backend.model.Player;
import com.example.backend.repository.GameEventRepository;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.AuthRateLimiter;
import com.example.backend.security.GoogleIdTokenVerifier;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.BotService;
import com.example.backend.service.GameNotificationService;
import com.example.backend.service.GameReplayService;
import com.example.backend.service.GameService;
import com.example.backend.service.GameValidatorService;
import com.example.backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Nothing in these mutations, this query, or this subscription takes a client-supplied playerId
 * any more: the acting player is always the caller's own seat at the table, looked up from the
 * authenticated username. There is no "wrong playerId" to send any more, only "not seated here".
 */
class ResolverAuthorizationTest {

    private GameService gameService;
    private BotService botService;
    private GameNotificationService notifications;
    private MutationResolver mutations;
    private QueryResolver queries;
    private SubscriptionResolver subscriptions;
    private Game game;
    private Player alice;
    private Player bot;

    @BeforeEach
    void setUp() {
        GameRepository repo = mock(GameRepository.class);
        GameValidatorService validator = new GameValidatorService(repo, mock(UserRepository.class));
        gameService = mock(GameService.class);
        botService = mock(BotService.class);
        notifications = mock(GameNotificationService.class);
        mutations = new MutationResolver(mock(AuthenticationManager.class), mock(JwtTokenProvider.class),
                mock(UserService.class), gameService, notifications, botService, validator, new AuthRateLimiter(1000),
                mock(GoogleIdTokenVerifier.class));
        queries = new QueryResolver(gameService, mock(UserRepository.class), mock(GameEventRepository.class),
                mock(GameReplayService.class), validator);
        subscriptions = new SubscriptionResolver(validator);

        alice = new Player("Alice", "alice", 1000);
        bot = new Player("Bot", "bot-1", 1000);
        bot.setBot(true);
        game = new Game(10, 20);
        game.getPlayers().addAll(List.of(alice, bot));
        when(repo.findById(game.getId())).thenReturn(Optional.of(game));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x", List.of()));
    }

    private static Map<String, Object> fold() {
        return Map.of("actionType", "FOLD");
    }

    @Test
    void aCallerWithNoSeatAtTheTableCannotAct() {
        loginAs("mallory");

        assertThrows(AccessDeniedException.class, () -> mutations.playerAction(game.getId(), fold()));

        verifyNoInteractions(gameService);
    }

    @Test
    void actingResolvesToTheCallersOwnSeatNeverAnyoneElses() {
        loginAs("alice");

        mutations.playerAction(game.getId(), fold());

        verify(gameService).fold(game.getId(), alice.getId());
    }

    @Test
    void onlySeatedPlayersCanStartHandOrManageBots() {
        loginAs("mallory");

        assertThrows(AccessDeniedException.class, () -> mutations.startHand(game.getId()));
        assertThrows(AccessDeniedException.class, () -> mutations.addBot(game.getId(), BotDifficulty.EASY));
        assertThrows(AccessDeniedException.class, () -> mutations.removeBot(game.getId(), bot.getId()));

        verify(gameService, never()).startNewHand(anyString());
        verifyNoInteractions(botService);
    }

    @Test
    void seatedPlayerCanAddAndRemoveBotsButNotHumans() {
        loginAs("alice");

        mutations.addBot(game.getId(), BotDifficulty.EASY);
        mutations.removeBot(game.getId(), bot.getId());
        assertThrows(IllegalArgumentException.class, () -> mutations.removeBot(game.getId(), alice.getId()));

        verify(botService).addBot(game.getId(), BotDifficulty.EASY);
        verify(botService).removeBot(game.getId(), bot.getId());
        verify(botService, never()).removeBot(game.getId(), alice.getId());
    }

    @Test
    void leaveSitOutSitInAllResolveToTheCallersOwnSeat() {
        loginAs("mallory");
        assertThrows(AccessDeniedException.class, () -> mutations.leaveGame(game.getId()));
        assertThrows(AccessDeniedException.class, () -> mutations.sitOut(game.getId()));
        assertThrows(AccessDeniedException.class, () -> mutations.sitIn(game.getId()));
        verifyNoInteractions(gameService);

        loginAs("alice");
        mutations.leaveGame(game.getId());
        mutations.sitOut(game.getId());
        mutations.sitIn(game.getId());

        verify(gameService).leaveGame(game.getId(), alice.getId());
        verify(gameService).sitOut(game.getId(), alice.getId());
        verify(gameService).sitIn(game.getId(), alice.getId());
    }

    @Test
    void chatMustComeFromYourOwnSeatAndCarriesYourName() {
        loginAs("mallory");
        assertThrows(AccessDeniedException.class, () -> mutations.sendChat(game.getId(), "hi"));
        verifyNoInteractions(notifications);

        loginAs("alice");
        ChatMessagePayload sent = mutations.sendChat(game.getId(), "  hi  ");
        assertEquals("Alice", sent.getSenderName());
        assertEquals(alice.getId(), sent.getSenderId());
        assertEquals("hi", sent.getMessage());
    }

    @Test
    void chatRejectsEmptyAndOversizedMessages() {
        loginAs("alice");

        assertThrows(IllegalArgumentException.class, () -> mutations.sendChat(game.getId(), "   "));
        assertThrows(IllegalArgumentException.class, () -> mutations.sendChat(game.getId(), "x".repeat(501)));
    }

    @Test
    void cannotReadAnotherPlayersHoleCards() {
        loginAs("mallory");
        assertThrows(AccessDeniedException.class, () -> queries.gameForPlayer(game.getId()));
        verify(gameService, never()).getGameForPlayer(anyString(), anyString());

        loginAs("alice");
        queries.gameForPlayer(game.getId());
        verify(gameService).getGameForPlayer(game.getId(), alice.getId());
    }

    @Test
    void privatePlayerStreamIsOwnerOnly() {
        // anonymous (no HTTP auth, no websocket-context username either)
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> subscriptions.playerUpdated(game.getId(), null));

        // another authenticated websocket user, not seated at this table
        assertThrows(AccessDeniedException.class, () -> subscriptions.playerUpdated(game.getId(), "mallory"));

        // the owner, identified from the websocket connection's own username
        assertNotNull(subscriptions.playerUpdated(game.getId(), "alice"));
    }

    @Test
    void eventLogAndReplayAreAdminOnly() throws Exception {
        for (String name : List.of("gameEvents", "gameEventsByType", "replayGame", "replayGameUntilEvent")) {
            var method = java.util.Arrays.stream(QueryResolver.class.getMethods())
                    .filter(m -> m.getName().equals(name)).findFirst().orElseThrow();
            assertEquals("hasRole('ADMIN')", method.getAnnotation(PreAuthorize.class).value(), name);
        }
    }
}
