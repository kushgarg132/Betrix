package com.example.backend.resolver;

import com.example.backend.config.WebSocketAuthInterceptor;
import com.example.backend.entity.Game;
import com.example.backend.model.BotDifficulty;
import com.example.backend.model.ChatMessagePayload;
import com.example.backend.model.Player;
import com.example.backend.repository.GameEventRepository;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Every table action must be tied to the caller's own seat; the client-supplied playerId is not trusted. */
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
                mock(UserService.class), gameService, notifications, botService, validator);
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

    private Map<String, Object> foldAs(String playerId) {
        return Map.of("playerId", playerId, "actionType", "FOLD");
    }

    @Test
    void cannotActAsAnotherPlayersSeat() {
        loginAs("mallory");

        assertThrows(AccessDeniedException.class, () -> mutations.playerAction(game.getId(), foldAs(alice.getId())));

        verifyNoInteractions(gameService);
    }

    @Test
    void canActAsYourOwnSeat() {
        loginAs("alice");

        mutations.playerAction(game.getId(), foldAs(alice.getId()));

        verify(gameService).fold(game.getId(), alice.getId());
    }

    @Test
    void unknownSeatIsDeniedNotAnInternalError() {
        loginAs("alice");

        assertThrows(AccessDeniedException.class, () -> mutations.playerAction(game.getId(), foldAs("nope")));
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
    void chatMustComeFromYourOwnSeatAndCarriesYourName() {
        loginAs("mallory");
        assertThrows(AccessDeniedException.class, () -> mutations.sendChat(game.getId(), "hi", alice.getId()));
        verifyNoInteractions(notifications);

        loginAs("alice");
        ChatMessagePayload sent = mutations.sendChat(game.getId(), "  hi  ", alice.getId());
        assertEquals("Alice", sent.getSenderName());
        assertEquals("hi", sent.getMessage());
    }

    @Test
    void chatRejectsEmptyAndOversizedMessages() {
        loginAs("alice");

        assertThrows(IllegalArgumentException.class, () -> mutations.sendChat(game.getId(), "   ", alice.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> mutations.sendChat(game.getId(), "x".repeat(501), alice.getId()));
    }

    @Test
    void cannotReadAnotherPlayersHoleCards() {
        loginAs("mallory");
        assertThrows(AccessDeniedException.class, () -> queries.gameForPlayer(game.getId(), alice.getId()));
        verify(gameService, never()).getGameForPlayer(anyString(), anyString());

        loginAs("alice");
        queries.gameForPlayer(game.getId(), alice.getId());
        verify(gameService).getGameForPlayer(game.getId(), alice.getId());
    }

    @Test
    void privatePlayerStreamIsOwnerOnly() {
        // anonymous
        assertThrows(AccessDeniedException.class, () -> subscriptions.playerUpdated(game.getId(), alice.getId(), null));
        // another authenticated websocket user
        assertThrows(AccessDeniedException.class, () -> subscriptions.playerUpdated(game.getId(), alice.getId(), "mallory"));
        // the owner
        assertNotNull(subscriptions.playerUpdated(game.getId(), alice.getId(), "alice"));
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
