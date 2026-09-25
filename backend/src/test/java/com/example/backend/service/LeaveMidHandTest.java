package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.model.Card;
import com.example.backend.model.Card.Rank;
import com.example.backend.model.Card.Suit;
import com.example.backend.model.Player;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.scheduler.GameScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Real services over an in-memory game: leaving in the middle of a hand must not lose or create chips. */
class LeaveMidHandTest {

    private GameLifecycleService lifecycle;
    private GameActionService actions;
    private Game game;
    private Player a;
    private Player b;
    private Player c;

    private static Card card(Rank r, Suit s) {
        return new Card(s, r);
    }

    private static Player seat(String name, Card x, Card y) {
        Player p = new Player(name, name, 1000);
        p.setHand(new ArrayList<>(List.of(x, y)));
        return p;
    }

    @BeforeEach
    void setUp() {
        GameRepository repo = mock(GameRepository.class);
        UserRepository users = mock(UserRepository.class);
        GameScheduler scheduler = mock(GameScheduler.class);
        GameEventPublisher publisher = mock(GameEventPublisher.class);
        GameValidatorService validator = new GameValidatorService(repo, users);
        BettingManager betting = new BettingManager(publisher, new HandEvaluator(), scheduler);
        actions = new GameActionService(repo, validator, betting, publisher, scheduler);
        lifecycle = new GameLifecycleService(users, repo, validator, publisher, actions);

        // b beats c; a is weakest
        a = seat("a", card(Rank.TWO, Suit.CLUBS), card(Rank.THREE, Suit.DIAMONDS));
        b = seat("b", card(Rank.ACE, Suit.SPADES), card(Rank.ACE, Suit.HEARTS));
        c = seat("c", card(Rank.SEVEN, Suit.SPADES), card(Rank.EIGHT, Suit.HEARTS));
        game = new Game(10, 20);
        game.getPlayers().addAll(List.of(a, b, c));
        game.setCommunityCards(new ArrayList<>(List.of(
                card(Rank.NINE, Suit.CLUBS), card(Rank.JACK, Suit.DIAMONDS), card(Rank.KING, Suit.HEARTS),
                card(Rank.FOUR, Suit.SPADES), card(Rank.SIX, Suit.CLUBS))));
        game.setStatus(Game.GameStatus.RIVER_BETTING);
        game.setCurrentPlayerIndex(0);
        game.setDealerPosition(2);
        game.getPlayers().forEach(p -> game.getLastActions().put(p.getUsername(), Game.PlayerAction.NONE));
        when(repo.findById(game.getId())).thenAnswer(i -> Optional.of(game));
    }

    @Test
    void chipsAreConservedWhenAPlayerWhoAlreadyBetLeavesOutOfTurn() {
        actions.placeBet(game.getId(), a.getId(), 100); // a bets, then it is b's turn
        actions.placeBet(game.getId(), b.getId(), 100);
        lifecycle.leaveGame(game.getId(), a.getId());   // c is to act; a is not on turn and walks away
        actions.placeBet(game.getId(), c.getId(), 100); // c calls; a folded, so b vs c goes to showdown

        // pot was 300 (a's 100 included) and b wins it all
        assertEquals(1200, b.getChips(), 0.001);
        assertEquals(900, c.getChips(), 0.001);
        assertEquals(900, a.getChips(), 0.001);
        assertEquals(3000, a.getChips() + b.getChips() + c.getChips(), 0.001, "chips in must equal chips out");
    }

    @Test
    void aLeavingPlayerStaysSeatedAndFoldedUntilTheHandEndsThenIsRemoved() {
        actions.placeBet(game.getId(), a.getId(), 100);
        actions.placeBet(game.getId(), b.getId(), 100);
        lifecycle.leaveGame(game.getId(), a.getId());

        assertEquals(3, game.getPlayers().size(), "a's bet is still live in this round, so a cannot be removed yet");
        assertTrue(a.isHasFolded());

        actions.placeBet(game.getId(), c.getId(), 100); // hand ends

        assertEquals(2, game.getPlayers().size());
        assertFalse(game.getPlayers().stream().anyMatch(p -> p.getId().equals(a.getId())));
    }

    @Test
    void leavingBetweenHandsRemovesThePlayerImmediately() {
        game.setStatus(Game.GameStatus.WAITING);

        lifecycle.leaveGame(game.getId(), a.getId());

        assertEquals(2, game.getPlayers().size());
    }

    @Test
    void ifEveryoneElseLeavesTheLastPlayerTakesThePot() {
        actions.placeBet(game.getId(), a.getId(), 100);
        actions.placeBet(game.getId(), b.getId(), 100);
        lifecycle.leaveGame(game.getId(), a.getId());
        lifecycle.leaveGame(game.getId(), c.getId()); // c is on turn: folds, only b is left, b takes 200 + 0

        assertEquals(1100, b.getChips(), 0.001); // 900 left + the 200 that a and b put in
    }
}
