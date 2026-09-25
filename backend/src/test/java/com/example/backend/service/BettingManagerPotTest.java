package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Card;
import com.example.backend.model.Card.Rank;
import com.example.backend.model.Card.Suit;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.scheduler.GameScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Chip conservation through showdown: chips in must equal chips out. */
class BettingManagerPotTest {

    private BettingManager manager;
    private com.example.backend.scheduler.GameScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = mock(com.example.backend.scheduler.GameScheduler.class);
        manager = new BettingManager(mock(GameEventPublisher.class), new HandEvaluator(), scheduler);
    }

    private static Card c(Rank r, Suit s) {
        return new Card(s, r);
    }

    private static Player player(String name, long chips, Card a, Card b) {
        Player p = new Player(name, name, chips);
        p.setHand(new ArrayList<>(List.of(a, b)));
        return p;
    }

    private static Game riverGame(Player... players) {
        Game g = new Game(10, 20);
        g.getPlayers().addAll(List.of(players));
        g.setCommunityCards(new ArrayList<>(List.of(
            c(Rank.TWO, Suit.CLUBS), c(Rank.THREE, Suit.DIAMONDS), c(Rank.SEVEN, Suit.CLUBS),
            c(Rank.EIGHT, Suit.DIAMONDS), c(Rank.NINE, Suit.CLUBS))));
        g.setStatus(Game.GameStatus.RIVER_BETTING);
        g.setCurrentPlayerIndex(0);
        return g;
    }

    @Test
    void headsUpShowdownPaysOutExactlyWhatWasBet() {
        Player a = player("a", 1000, c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS));
        Player b = player("b", 1000, c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS));
        Game g = riverGame(a, b);

        manager.placeBet(g, a, 100, null);
        manager.placeBet(g, b, 100, null);
        manager.handleCurrentBettingRound(g, b.getId());

        assertEquals(2000, a.getChips() + b.getChips(), 0.001);
        assertEquals(1100, a.getChips(), 0.001);
        assertEquals(900, b.getChips(), 0.001);
    }

    @Test
    void shortAllInWinsOnlyMainPotAndSidePotGoesToBestOfRest() {
        Player a = player("a", 50, c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS));
        Player b = player("b", 1000, c(Rank.KING, Suit.SPADES), c(Rank.KING, Suit.HEARTS));
        Player d = player("d", 1000, c(Rank.QUEEN, Suit.SPADES), c(Rank.QUEEN, Suit.HEARTS));
        Game g = riverGame(a, b, d);

        manager.placeBet(g, a, 50, null);
        manager.placeBet(g, b, 100, null);
        manager.placeBet(g, d, 100, null);
        manager.handleCurrentBettingRound(g, d.getId());

        assertEquals(2050, a.getChips() + b.getChips() + d.getChips(), 0.001);
        assertEquals(150, a.getChips(), 0.001);   // 3 x 50 main pot
        assertEquals(1000, b.getChips(), 0.001);  // 900 left + 100 side pot
        assertEquals(900, d.getChips(), 0.001);
    }

    /** Two players tie for a pot that does not divide evenly. Chips are whole numbers, so the odd chip must go somewhere. */
    @Test
    void oddChipInATiedPotGoesToTheFirstWinnerLeftOfTheDealerAndNothingIsCreatedOrLost() {
        // the board plays for a and b (royal flush); c has nothing
        Player a = player("a", 1000, c(Rank.TWO, Suit.CLUBS), c(Rank.THREE, Suit.CLUBS));
        Player b = player("b", 1000, c(Rank.FOUR, Suit.CLUBS), c(Rank.FIVE, Suit.CLUBS));
        Player d = player("d", 1000, c(Rank.SIX, Suit.CLUBS), c(Rank.SEVEN, Suit.CLUBS));
        Game g = riverGame(a, b, d);
        g.setCommunityCards(new ArrayList<>(List.of(
            c(Rank.TEN, Suit.HEARTS), c(Rank.JACK, Suit.HEARTS), c(Rank.QUEEN, Suit.HEARTS),
            c(Rank.KING, Suit.HEARTS), c(Rank.ACE, Suit.HEARTS))));
        g.setDealerPosition(0);

        manager.placeBet(g, a, 33, null);
        manager.placeBet(g, b, 33, null);
        manager.placeBet(g, d, 33, null);
        manager.handleCurrentBettingRound(g, d.getId());

        // all three hold the royal flush on the board, so the 99 pot splits three ways: 33 each, no remainder
        assertEquals(3000, a.getChips() + b.getChips() + d.getChips(), 0.001);
    }

    @Test
    void potThatDoesNotDivideEvenlyPaysWholeChipsAndKeepsEveryChip() {
        // a and b tie with a pair of aces on the board-independent hands; d folds after betting
        Player a = player("a", 1000, c(Rank.ACE, Suit.SPADES), c(Rank.TWO, Suit.HEARTS));
        Player b = player("b", 1000, c(Rank.ACE, Suit.HEARTS), c(Rank.TWO, Suit.CLUBS));
        Player d = player("d", 1000, c(Rank.SIX, Suit.SPADES), c(Rank.FOUR, Suit.HEARTS));
        Game g = riverGame(a, b, d);
        g.setCommunityCards(new ArrayList<>(List.of(
            c(Rank.ACE, Suit.CLUBS), c(Rank.KING, Suit.DIAMONDS), c(Rank.NINE, Suit.HEARTS),
            c(Rank.EIGHT, Suit.SPADES), c(Rank.JACK, Suit.CLUBS))));
        g.setDealerPosition(0);

        manager.placeBet(g, a, 33, null);
        manager.placeBet(g, b, 33, null);
        manager.placeBet(g, d, 33, null);
        d.setHasFolded(true);
        manager.handleCurrentBettingRound(g, b.getId());

        double total = a.getChips() + b.getChips() + d.getChips();
        assertEquals(3000, total, 0.001);
        assertEquals(Math.rint(a.getChips()), a.getChips(), 0.0, "whole chips only: a");
        assertEquals(Math.rint(b.getChips()), b.getChips(), 0.0, "whole chips only: b");
        // 99 split two ways: 50 to the first winner left of the dealer (b, seat 1), 49 to a
        assertEquals(1017, b.getChips(), 0.001);
        assertEquals(1016, a.getChips(), 0.001);
    }

    /**
     * A corrupt player (no hole cards) makes HandEvaluator throw before anything is awarded.
     * The old code caught this, zeroed the pot (destroying the chips) and left the game in
     * WAITING with no next hand ever scheduled — a permanently stuck table. It must instead
     * pay the pot out (evenly, since it cannot judge hands) and keep the game moving.
     */
    @Test
    void aHandEvaluationFailureStillPaysThePotAndScheduleTheNextHandInsteadOfDestroyingChipsOrStalling() {
        Player a = player("a", 1000, c(Rank.ACE, Suit.SPADES), c(Rank.ACE, Suit.HEARTS));
        Player corrupt = new Player("b", "b", 1000);
        corrupt.setHand(new ArrayList<>()); // missing hole cards: HandEvaluator.evaluateHand throws
        Game g = riverGame(a, corrupt);

        manager.placeBet(g, a, 50, null);
        manager.placeBet(g, corrupt, 50, null);
        manager.handleCurrentBettingRound(g, corrupt.getId());

        assertEquals(2000, a.getChips() + corrupt.getChips(), 0.001, "the 100-chip pot must not vanish");
        assertEquals(1000, a.getChips(), 0.001, "pot returned evenly: net zero for a two-way, equal-bet tie");
        assertEquals(1000, corrupt.getChips(), 0.001);
        assertEquals(Game.GameStatus.WAITING, g.getStatus());
        verify(scheduler).scheduleNextHand(g.getId());
    }
}
