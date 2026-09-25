package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.scheduler.GameScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameActionServiceCheckTest {

    private GameActionService service;
    private Game game;
    private Player a;
    private Player b;

    @BeforeEach
    void setUp() {
        GameRepository repo = mock(GameRepository.class);
        GameScheduler scheduler = mock(GameScheduler.class);
        GameEventPublisher publisher = mock(GameEventPublisher.class);
        BettingManager betting = new BettingManager(publisher, new HandEvaluator(), scheduler);
        GameValidatorService validator = new GameValidatorService(repo, mock(UserRepository.class));
        service = new GameActionService(repo, validator, betting, publisher, scheduler);

        a = new Player("a", "a", 1000);
        b = new Player("b", "b", 1000);
        game = new Game(10, 20);
        game.getPlayers().add(a);
        game.getPlayers().add(b);
        game.setStatus(Game.GameStatus.FLOP_BETTING);
        game.setCurrentPlayerIndex(0);
        when(repo.findById(game.getId())).thenReturn(Optional.of(game));
    }

    @Test
    void checkFacingABetIsRejectedAndLeavesTurnUnchanged() {
        game.setCurrentBet(100);
        game.getCurrentBettingRound().getBets().put(b.getId(), 100L);

        assertThrows(RuntimeException.class, () -> service.check(game.getId(), a.getId()));

        assertEquals(0, game.getCurrentPlayerIndex());
        assertEquals(1000, a.getChips(), 0.001);
    }

    @Test
    void checkWithNothingToCallMovesTurn() {
        service.check(game.getId(), a.getId());

        assertEquals(1, game.getCurrentPlayerIndex());
    }
}
