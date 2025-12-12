package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.BettingRound;
import com.example.backend.model.Player;
import com.example.backend.scheduler.GameScheduler;
import com.example.backend.publisher.GameEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AllInFlowTest {

    @Test
    public void testAllInSequenceScheduling() {
        // Mocks
        GameEventPublisher eventPublisher = mock(GameEventPublisher.class);
        HandEvaluator handEvaluator = mock(HandEvaluator.class);
        GameScheduler gameScheduler = mock(GameScheduler.class);

        BettingManager bettingManager = new BettingManager(eventPublisher, handEvaluator, gameScheduler);

        // Setup Game
        Game game = new Game(10, 20);
        game.setId("test-game-1");

        // Setup scenarios to pass basic checks
        // We need to bypass some BettingManager internal checks or ensure state is
        // valid
        // initiateAllInSequence is private, so we test trigger point:
        // handleCurrentBettingRound

        // Scenario: PreFlop, Everyone All-In
        game.setupNextRound(); // PreFlop

        Player p1 = new Player("p1", "p1", 100);
        p1.setChips(0);
        p1.setAllIn(true);
        p1.setActive(true);

        Player p2 = new Player("p2", "p2", 100);
        p2.setChips(0);
        p2.setAllIn(true);
        p2.setActive(true);

        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        // Mock game state for "Are all players active all in"
        // BettingManager.areAllActivePlayersAllIn uses stream filters.

        // Trigger the end of round logic
        // We simulate that the round needs to close
        // handleCurrentBettingRound calls isBettingRoundComplete -> true (because p1,
        // p2 matched bets)
        // Then calls areAllActivePlayersAllIn -> true

        // We need to ensure isBettingRoundComplete returns true
        game.getCurrentBettingRound().getPlayerBets().put("p1", 100.0);
        game.getCurrentBettingRound().getPlayerBets().put("p2", 100.0);
        game.setCurrentBet(100.0);
        game.getLastActions().put("p1", Game.PlayerAction.ALL_IN);
        game.getLastActions().put("p2", Game.PlayerAction.ALL_IN);

        // Run
        bettingManager.handleCurrentBettingRound(game, "p2");

        // Verify:
        // 1. Should NOT call evaluateHandAndAwardPot immediately (status should not be
        // SHOWDOWN)
        // 2. Should call gameScheduler.scheduleAllInAction

        assert (game.getStatus() != Game.GameStatus.SHOWDOWN);
        verify(gameScheduler, times(1)).scheduleAllInAction(eq("test-game-1"));

        // -- Phase 2: Callback execution --
        // Simulate scheduler calling executeAllInAction ->
        // bettingManager.processAllInRound
        // Current status is PRE_FLOP or FLOP_BETTING (depending on logic)
        // Actually, initiateAllInSequence just schedules cleanly. It doesn't advance
        // round yet.

        // Call processAllInRound manually to simulate next step
        bettingManager.processAllInRound(game);

        // Now it should have advanced to FLOP and scheduled again
        assert (game.getStatus() == Game.GameStatus.FLOP_BETTING);
        verify(gameScheduler, times(2)).scheduleAllInAction(eq("test-game-1"));

        // Call again -> POST FLOP (TURN)
        bettingManager.processAllInRound(game);
        assert (game.getStatus() == Game.GameStatus.TURN_BETTING);

        // Call again -> POST TURN (RIVER)
        bettingManager.processAllInRound(game);
        assert (game.getStatus() == Game.GameStatus.RIVER_BETTING);

        // Call again -> POST RIVER (SHOWDOWN)
        bettingManager.processAllInRound(game);

        // Should verify evaluate was called.
        // We can verify game status changed to WAIT or similar, or check logs.
        // Actually evaluateHandAndAwardPot sets status to SHOWDOWN/WAITING.

        // Since we mocked dependencies, evaluateHandAndAwardPot might crash or be NO-OP
        // if not carefully mocked.
        // BettingManager calls handEvaluator.
        // We can just check status.

        assert (game.getStatus() == Game.GameStatus.WAITING || game.getStatus() == Game.GameStatus.SHOWDOWN);

        System.out.println("Test Passed: Sequence scheduled correctly.");
    }
}
