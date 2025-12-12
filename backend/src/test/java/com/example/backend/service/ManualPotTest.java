package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.BettingRound;
import com.example.backend.model.Player;
import com.example.backend.model.Pot;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual Test for logic which doesn't require Spring Context
 */
public class ManualPotTest {

    @Test
    public void testPotDistribution() {
        System.out.println("Running Pot Distribution Test...");
        
        // Setup Mocks (nulls as we don't need them for updatePotAmounts logic)
        BettingManager manager = new BettingManager(null, null, null);
        
        // Setup Game
        Game game = new Game(10, 20); // Small, Big Blinds
        
        // Setup Players
        Player p1 = new Player("Player1", "p1", 100);  // Short stack
        Player p2 = new Player("Player2", "p2", 1000); // Deep stack
        Player p3 = new Player("Player3", "p3", 1000); // Deep stack
        
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);
        game.getPlayers().add(p3);
        
        // --- Scenario 1: Pre-Flop All-In (100 each) ---
        game.setupNextRound(); // PRE_FLOP
        
        // Manually set bets
        game.getCurrentBettingRound().getPlayerBets().put("p1", 100.0);
        p1.setHasFolded(false);
        p1.setChips(0); // All in
        p1.setActive(true);
        
        game.getCurrentBettingRound().getPlayerBets().put("p2", 100.0);
        p2.setHasFolded(false);
        p2.setChips(900);
        p2.setActive(true);
        
        game.getCurrentBettingRound().getPlayerBets().put("p3", 100.0);
        p3.setHasFolded(false);
        p3.setChips(900);
        p3.setActive(true);
        
        // Invoke updatedPotAmounts via reflection
        try {
            java.lang.reflect.Method method = BettingManager.class.getDeclaredMethod("updatePotAmounts", Game.class);
            method.setAccessible(true);
            method.invoke(manager, game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Expectation: 1 Pot of 300, Eligible: p1, p2, p3
        assertEquals(1, game.getPots().size());
        assertEquals(300.0, game.getPots().get(0).getAmount(), 0.01);
        
        // --- Scenario 2: Flop - P2 bets 200, P3 calls 200 ---
        game.setupNextRound(); 
        
        game.getCurrentBettingRound().getPlayerBets().put("p2", 200.0);
        game.getCurrentBettingRound().getPlayerBets().put("p3", 200.0);
        game.getCurrentBettingRound().getPlayerBets().put("p1", 0.0);

        try {
            java.lang.reflect.Method method = BettingManager.class.getDeclaredMethod("updatePotAmounts", Game.class);
            method.setAccessible(true);
            method.invoke(manager, game);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Expectation:
        // Pot 0 (Main): 300. Eligible: p1, p2, p3.
        // Pot 1 (Side): 400. Eligible: p2, p3.
        
        assertEquals(2, game.getPots().size());
        assertEquals(300.0, game.getPots().get(0).getAmount(), 0.01);
        assertEquals(400.0, game.getPots().get(1).getAmount(), 0.01);
        
        assertTrue(game.getPots().get(1).getEligiblePlayerIds().contains(p2.getId()));
        assertTrue(game.getPots().get(1).getEligiblePlayerIds().contains(p3.getId()));
        
        System.out.println("Tests Passed!");
    }
}
