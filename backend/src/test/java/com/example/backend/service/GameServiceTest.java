package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Deck;
import com.example.backend.model.Player;
import com.example.backend.model.Pot;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.scheduler.GameScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GameServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private BettingManager bettingManager;

    @Mock
    private GameValidatorService gameValidatorService;

    @Mock
    private GameEventPublisher eventPublisher;

    @Mock
    private GameScheduler gameScheduler;

    @InjectMocks
    @Spy // Spy to allow partial mocking of methods within the same class (like
         // leaveGame)
    private GameServiceImpl gameService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSitOut() {
        String gameId = "game-1";
        String playerId = "player-1";
        Game game = new Game(10, 20);
        Player player = new Player("Player1", "p1", 1000);
        player.setId(playerId);
        game.getPlayers().add(player);

        when(gameValidatorService.validateGameExists(gameId)).thenReturn(game);

        gameService.sitOut(gameId, playerId);

        assertTrue(player.isSittingOut());
        verify(gameRepository, times(1)).save(game);
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    public void testSitIn() {
        String gameId = "game-1";
        String playerId = "player-1";
        Game game = new Game(10, 20);
        Player player = new Player("Player1", "p1", 1000);
        player.setId(playerId);
        player.setSittingOut(true); // Initially sitting out
        game.getPlayers().add(player);

        when(gameValidatorService.validateGameExists(gameId)).thenReturn(game);

        gameService.sitIn(gameId, playerId);

        assertFalse(player.isSittingOut());
        verify(gameRepository, times(1)).save(game);
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    public void testAutoKickInsufficientFunds() {
        String gameId = "game-1";
        Game game = new Game(10, 20); // BB = 20
        game.setStatus(Game.GameStatus.WAITING);

        Player p1 = new Player("P1", "p1", 1000);
        p1.setId("p1");

        Player p2 = new Player("P2", "p2", 1000);
        p2.setId("p2");

        Player p3 = new Player("P3", "p3", 10); // Chips < BB
        p3.setId("p3");

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);
        game.getPlayers().add(p3);

        game.setDeck(new Deck()); // Ensure desk exists

        when(gameValidatorService.validateGameExists(gameId)).thenReturn(game);

        // Mock leaveGame to actually remove the player or do nothing (since we are
        // testing startNewHand logic calling leaveGame)
        // Since we Spy on gameService, the real leaveGame will be called unless
        // stubbed.
        // The real leaveGame calls gameRepository, etc. We should let it run or verify
        // the call.
        // Real leaveGame removes player from list.

        // However, startNewHand iterates over a COPY or original list?
        // Logic: for (Player player : game.getPlayers()) ... toKick.add.. then loop
        // toKick and call leaveGame.
        // This is safe.

        gameService.startNewHand(gameId);

        // Verify p3 was kicked (leaveGame called)
        verify(gameService, times(1)).leaveGame(gameId, "p3");

        // Since we are mocking dependencies, strict real behavior of leaveGame might
        // not fully execute if we don't mock findPlayer correctly in leaveGame?
        // leaveGame uses gameValidatorService again. which is mocked to return game.
        // It should work.

        assertFalse(game.getPlayers().contains(p3), "Player 3 should be removed from game");
        assertTrue(game.getPlayers().contains(p1));
        assertTrue(game.getPlayers().contains(p2));
    }

    @Test
    public void testSitOutSkipsDeal() {
        String gameId = "game-1";
        Game game = new Game(10, 20);
        game.setStatus(Game.GameStatus.WAITING);

        Player p1 = new Player("P1", "p1", 1000);
        p1.setId("p1");

        Player p2 = new Player("P2", "p2", 1000);
        p2.setId("p2");
        p2.setSittingOut(true); // Sitting out

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        game.setDeck(new Deck());

        when(gameValidatorService.validateGameExists(gameId)).thenReturn(game);

        // We need 2 ACTIVE players for start a game usually?
        // Logic: if (activePlayersCount < 2) throw...
        // Here only P1 is active. So it should THROW exception.

        assertThrows(RuntimeException.class, () -> {
            gameService.startNewHand(gameId);
        });

        // Now lets add P3 active
        Player p3 = new Player("P3", "p3", 1000);
        p3.setId("p3");
        game.getPlayers().add(p3);

        // Now 2 active, 1 sitting out. Should pass.
        gameService.startNewHand(gameId);

        assertEquals(2, p1.getHand().size());
        assertEquals(2, p3.getHand().size());
        assertEquals(0, p2.getHand().size(), "Sitting out player should not get cards");
        assertTrue(p1.isActive());
        assertTrue(p3.isActive());
        assertFalse(p2.isActive());
    }
}
