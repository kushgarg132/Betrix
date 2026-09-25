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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Starting a hand through the real services: who is charged, and who acts first. */
class GameHandServiceBlindsTest {

    private GameHandService hands;
    private GameRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(GameRepository.class);
        UserRepository users = mock(UserRepository.class);
        GameEventPublisher publisher = mock(GameEventPublisher.class);
        GameScheduler scheduler = mock(GameScheduler.class);
        GameValidatorService validator = new GameValidatorService(repo, users);
        BettingManager betting = new BettingManager(publisher, new HandEvaluator(), scheduler);
        GameActionService actions = new GameActionService(repo, validator, betting, publisher, scheduler);
        GameLifecycleService lifecycle = new GameLifecycleService(users, repo, validator, publisher, actions);
        hands = new GameHandService(repo, validator, betting, publisher, lifecycle, scheduler);
    }

    private Game table(String... names) {
        Game g = new Game(10, 20);
        for (String n : names) {
            g.getPlayers().add(new Player(n, n, 1000));
        }
        when(repo.findById(g.getId())).thenReturn(Optional.of(g));
        return g;
    }

    private static Player p(Game g, String name) {
        return g.getPlayers().stream().filter(x -> x.getUsername().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void blindsAreChargedToDealtInPlayersAndTheSeatAfterTheBigBlindActsFirst() {
        Game g = table("a", "b", "c", "d", "e");
        p(g, "b").setSittingOut(true);
        g.setDealerPosition(4); // e had the button; a gets it

        hands.startNewHand(g.getId());

        // a = button, small blind = c (b sits out), big blind = d, first to act = e
        assertEquals(1000, p(g, "a").getChips());
        assertEquals(1000, p(g, "b").getChips(), "sitting out: no blind, no cards");
        assertEquals(990, p(g, "c").getChips());
        assertEquals(980, p(g, "d").getChips());
        assertEquals("e", g.getPlayers().get(g.getCurrentPlayerIndex()).getUsername());
        assertEquals(30, g.getPot());
    }

    @Test
    void headsUpTheButtonPostsTheSmallBlindAndActsFirstBeforeTheFlop() {
        Game g = table("a", "b");
        g.setDealerPosition(1); // b had it; a gets it

        hands.startNewHand(g.getId());

        assertEquals(990, p(g, "a").getChips(), "button posts the small blind");
        assertEquals(980, p(g, "b").getChips());
        assertEquals("a", g.getPlayers().get(g.getCurrentPlayerIndex()).getUsername());
    }
}
