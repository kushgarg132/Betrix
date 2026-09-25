package com.example.backend.scheduler;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import com.example.backend.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Timeout timers are captured, not run on a clock, so each test decides exactly when a timer "fires". */
class GameSchedulerTimeoutTest {

    private GameService gameService;
    private TaskScheduler taskScheduler;
    private GameScheduler scheduler;
    private Game game;
    private Player x;
    private Player y;

    @BeforeEach
    void setUp() {
        GameRepository repo = mock(GameRepository.class);
        gameService = mock(GameService.class);
        taskScheduler = mock(TaskScheduler.class);
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(mock(ScheduledFuture.class));
        scheduler = new GameScheduler(repo, gameService, taskScheduler);
        scheduler.initMetrics();

        x = new Player("x", "x", 1000);
        y = new Player("y", "y", 1000);
        x.setTimeBankMs(0);
        y.setTimeBankMs(0);
        game = new Game(10, 20);
        game.getPlayers().addAll(List.of(x, y));
        game.setStatus(Game.GameStatus.FLOP_BETTING);
        when(repo.findById(game.getId())).thenReturn(Optional.of(game));
    }

    private List<Runnable> scheduledRunnables(int expected) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler, times(expected)).schedule(captor.capture(), any(Instant.class));
        return captor.getAllValues();
    }

    @Test
    void timeoutFiresAndFoldsThePlayerWhoseTurnItIs() {
        game.setCurrentPlayerIndex(1);
        scheduler.schedulePlayerTimeout(game.getId(), y.getId());

        scheduledRunnables(1).get(0).run();

        verify(gameService).fold(game.getId(), y.getId());
    }

    /** Timer for y is scheduled, then the street changes and a new timer is scheduled for x. y's old timer must be dead. */
    @Test
    void staleTimerFromAnEarlierTurnDoesNothing() {
        scheduler.schedulePlayerTimeout(game.getId(), y.getId());
        scheduler.schedulePlayerTimeout(game.getId(), x.getId());
        game.setCurrentPlayerIndex(1); // turn has legitimately come round to y

        scheduledRunnables(2).get(0).run(); // y's stale timer fires

        verify(gameService, never()).fold(anyString(), anyString());
    }

    /** cancel() cannot stop a timer that has already started running; the epoch check must. */
    @Test
    void cancelledTimerThatStillRunsDoesNothing() {
        game.setCurrentPlayerIndex(1);
        scheduler.schedulePlayerTimeout(game.getId(), y.getId());
        scheduler.cancelPlayerTimeout(game.getId(), y.getId());

        scheduledRunnables(1).get(0).run();

        verify(gameService, never()).fold(anyString(), anyString());
    }

    @Test
    void cancelGameTimeoutKillsAPendingTimerForAnyPlayer() {
        game.setCurrentPlayerIndex(1);
        scheduler.schedulePlayerTimeout(game.getId(), y.getId());
        scheduler.cancelGameTimeout(game.getId());

        scheduledRunnables(1).get(0).run();

        verify(gameService, never()).fold(anyString(), anyString());
    }
}
