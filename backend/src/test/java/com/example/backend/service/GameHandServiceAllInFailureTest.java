package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.scheduler.GameScheduler;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A failure mid all-in run-out used to leave the table stuck forever with nothing scheduled. */
class GameHandServiceAllInFailureTest {

    @Test
    void aFailureDuringTheAllInRunOutStillMovesTheGameBackToWaitingAndSchedulesTheNextHand() {
        GameRepository repo = mock(GameRepository.class);
        BettingManager betting = mock(BettingManager.class);
        GameScheduler scheduler = mock(GameScheduler.class);
        GameValidatorService validator = new GameValidatorService(repo, mock(UserRepository.class));
        GameHandService hand = new GameHandService(repo, validator, betting,
                mock(GameEventPublisher.class), mock(GameLifecycleService.class), scheduler);

        Game game = new Game(10, 20);
        game.getPlayers().add(new Player("a", "a", 1000));
        game.getPlayers().add(new Player("b", "b", 1000));
        game.setStatus(Game.GameStatus.RIVER_BETTING);
        when(repo.findById(game.getId())).thenReturn(Optional.of(game));
        doThrow(new RuntimeException("evaluation blew up")).when(betting).processAllInRound(any());

        hand.executeAllInAction(game.getId());

        assertEquals(Game.GameStatus.WAITING, game.getStatus());
        verify(repo).save(game);
        verify(scheduler).scheduleNextHand(game.getId());
    }
}
