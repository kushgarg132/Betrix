package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.entity.User;
import com.example.backend.model.BlindPayload;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Play money: everyone sits down with the same table buy-in, whatever their account says. */
class GameLifecycleServiceJoinTest {

    private GameRepository repo;
    private UserRepository users;
    private GameLifecycleService service;
    private Game game;

    @BeforeEach
    void setUp() {
        repo = mock(GameRepository.class);
        users = mock(UserRepository.class);
        service = new GameLifecycleService(users, repo, new GameValidatorService(repo, users),
                mock(GameEventPublisher.class), mock(GameActionService.class));
        ReflectionTestUtils.setField(service, "buyIn", 5000L);
        game = new Game(10, 20);
        when(repo.findById(game.getId())).thenReturn(Optional.of(game));
    }

    private static Player seat(Game g, String username) {
        return g.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElseThrow();
    }

    @Test
    void registeredUsersAndGuestsSitDownWithTheSameBuyIn() {
        User alice = new User();
        alice.setUsername("alice");
        alice.setName("Alice");
        alice.setBalance(1000); // legacy field, must not matter
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

        service.joinGame(game.getId(), "alice");
        service.joinGame(game.getId(), "guest-abc123");

        assertEquals(5000, seat(game, "alice").getChips(), 0.001);
        assertEquals(5000, seat(game, "guest-abc123").getChips(), 0.001);
    }

    @Test
    void createGameRejectsNonsenseBlinds() {
        int[][] bad = {{0, 20}, {-5, 10}, {20, 10}, {1, 2_000_000_000}};
        for (int[] b : bad) {
            BlindPayload payload = new BlindPayload();
            payload.setSmallBlindAmount(b[0]);
            payload.setBigBlindAmount(b[1]);
            assertThrows(IllegalArgumentException.class, () -> service.createGame(payload), b[0] + "/" + b[1]);
        }
        verify(repo, never()).save(any());
    }

    @Test
    void createGameAcceptsSensibleBlinds() {
        BlindPayload payload = new BlindPayload();
        payload.setSmallBlindAmount(10);
        payload.setBigBlindAmount(20);

        service.createGame(payload);

        verify(repo).save(any(Game.class));
    }
}
