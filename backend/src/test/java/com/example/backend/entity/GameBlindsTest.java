package com.example.backend.entity;

import com.example.backend.model.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GameBlindsTest {

    private static Game table(String... names) {
        Game g = new Game(10, 20);
        for (String n : names) {
            g.getPlayers().add(new Player(n, n, 1000));
        }
        return g;
    }

    private static String dealer(Game g) {
        return g.getPlayers().get(g.getDealerPosition()).getUsername();
    }

    @Test
    void theButtonAndBlindsMoveOneDealtInSeatAtATime() {
        Game g = table("a", "b", "c", "d");
        g.setDealerPosition(3); // d had the button, so a gets it next

        g.resetForNewHand();
        assertEquals("a", dealer(g));
        assertEquals("b", g.getSmallBlindUserId());
        assertEquals("c", g.getBigBlindUserId());

        g.resetForNewHand();
        assertEquals("b", dealer(g));
        assertEquals("c", g.getSmallBlindUserId());
        assertEquals("d", g.getBigBlindUserId());
    }

    @Test
    void aSittingOutPlayerNeverGetsTheButtonOrABlind() {
        Game g = table("a", "b", "c", "d");
        g.getPlayers().get(1).setSittingOut(true); // b
        g.setDealerPosition(3);

        for (int hand = 0; hand < 6; hand++) {
            g.resetForNewHand();
            assertNotEquals("b", dealer(g), "hand " + hand + " button");
            assertNotEquals("b", g.getSmallBlindUserId(), "hand " + hand + " small blind");
            assertNotEquals("b", g.getBigBlindUserId(), "hand " + hand + " big blind");
            assertNotEquals(g.getSmallBlindUserId(), g.getBigBlindUserId());
        }
    }

    @Test
    void aBrokePlayerIsSkippedToo() {
        Game g = table("a", "b", "c");
        g.getPlayers().get(2).setChips(0); // c
        g.setDealerPosition(1);

        g.resetForNewHand();

        assertNotEquals("c", dealer(g));
        assertNotEquals("c", g.getSmallBlindUserId());
        assertNotEquals("c", g.getBigBlindUserId());
    }

    /** Heads-up the button posts the small blind and the other player the big blind. */
    @Test
    void headsUpTheButtonPostsTheSmallBlind() {
        Game g = table("a", "b");
        g.setDealerPosition(1);

        g.resetForNewHand();
        assertEquals("a", dealer(g));
        assertEquals("a", g.getSmallBlindUserId());
        assertEquals("b", g.getBigBlindUserId());

        g.resetForNewHand();
        assertEquals("b", dealer(g));
        assertEquals("b", g.getSmallBlindUserId());
        assertEquals("a", g.getBigBlindUserId());
    }

    @Test
    void headsUpBecauseOthersSitOutBehavesTheSameWay() {
        Game g = table("a", "b", "c");
        g.getPlayers().get(1).setSittingOut(true);
        g.setDealerPosition(2);

        g.resetForNewHand();

        assertEquals("a", dealer(g));
        assertEquals("a", g.getSmallBlindUserId());
        assertEquals("c", g.getBigBlindUserId());
    }

    @Test
    void theSmallBlindSeatIsTheFirstToPostSoTheBlindPostingFlowsIntoTheRightStartingPlayer() {
        Game g = table("a", "b", "c", "d");
        g.setDealerPosition(3);

        g.resetForNewHand();

        assertEquals("b", g.getPlayers().get(g.getCurrentPlayerIndex()).getUsername());
    }
}
