package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Player;
import com.example.backend.repository.GameRepository;
import com.example.backend.scheduler.GameScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Turn timers, the bot registry and the all-in sequence live in memory; after a restart hands in progress must resume. */
class GameRecoveryTest {

    private GameRepository repo;
    private GameScheduler scheduler;
    private BotService bots;
    private GameRecovery recovery;

    @BeforeEach
    void setUp() {
        repo = mock(GameRepository.class);
        scheduler = mock(GameScheduler.class);
        bots = mock(BotService.class);
        recovery = new GameRecovery(repo, scheduler, bots, new GameLocks());
    }

    private Game game(Game.GameStatus status, int currentIndex, Player... players) {
        Game g = new Game(10, 20);
        g.getPlayers().addAll(List.of(players));
        g.setStatus(status);
        g.setCurrentPlayerIndex(currentIndex);
        when(repo.findById(g.getId())).thenReturn(Optional.of(g));
        return g;
    }

    private void recoverAll(Game... games) {
        when(repo.findAll()).thenReturn(List.of(games));
        recovery.recover();
    }

    @Test
    void aHandInProgressGetsItsTurnTimerBack() {
        Player a = new Player("a", "a", 1000);
        Player b = new Player("b", "b", 1000);
        Game g = game(Game.GameStatus.FLOP_BETTING, 1, a, b);

        recoverAll(g);

        verify(scheduler).schedulePlayerTimeout(g.getId(), b.getId());
    }

    @Test
    void aWaitingGameNeedsNoTimer() {
        Game g = game(Game.GameStatus.WAITING, -1, new Player("a", "a", 1000), new Player("b", "b", 1000));

        recoverAll(g);

        verify(scheduler, never()).schedulePlayerTimeout(anyString(), anyString());
        verify(scheduler, never()).scheduleAllInAction(anyString());
    }

    @Test
    void whenEveryoneLeftIsAllInTheRunOutContinuesInsteadOfWaitingForATimeout() {
        Player a = new Player("a", "a", 0);
        Player b = new Player("b", "b", 0);
        Game g = game(Game.GameStatus.FLOP_BETTING, 0, a, b);

        recoverAll(g);

        verify(scheduler).scheduleAllInAction(g.getId());
        verify(scheduler, never()).schedulePlayerTimeout(anyString(), anyString());
    }

    @Test
    void botsAreRegisteredAgainAndABotWhoseTurnItIsActs() {
        Player human = new Player("h", "h", 1000);
        Player bot = new Player("Bot", "bot-1", 1000);
        bot.setBot(true);
        Game g = game(Game.GameStatus.TURN_BETTING, 1, human, bot);

        recoverAll(g);

        verify(bots).registerExistingBots(g);
        verify(bots).onGameUpdate(g.getId(), g);
    }

    @Test
    void oneBrokenGameDoesNotStopTheOthersFromRecovering() {
        Game broken = game(Game.GameStatus.FLOP_BETTING, 0, new Player("a", "a", 1000), new Player("b", "b", 1000));
        when(repo.findById(broken.getId())).thenThrow(new RuntimeException("bad document"));
        Player c = new Player("c", "c", 1000);
        Game fine = game(Game.GameStatus.FLOP_BETTING, 0, c, new Player("d", "d", 1000));

        recoverAll(broken, fine);

        verify(scheduler).schedulePlayerTimeout(fine.getId(), c.getId());
    }

    @Test
    void aDatabaseThatIsDownAtStartupDoesNotCrashTheApp() {
        when(repo.findAll()).thenThrow(new RuntimeException("no Atlas"));

        recovery.recover(); // must not throw
    }
}
