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

/** Chip conservation through showdown: chips in must equal chips out. */
class BettingManagerPotTest {

    private BettingManager manager;

    @BeforeEach
    void setUp() {
        manager = new BettingManager(mock(GameEventPublisher.class), new HandEvaluator(), mock(GameScheduler.class));
    }

    private static Card c(Rank r, Suit s) {
        return new Card(s, r);
    }

    private static Player player(String name, double chips, Card a, Card b) {
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
}
