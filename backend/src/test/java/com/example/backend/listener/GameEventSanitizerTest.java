package com.example.backend.listener;

import com.example.backend.entity.Game;
import com.example.backend.event.GameEvent;
import com.example.backend.event.PlayerJoinedEvent;
import com.example.backend.model.Card;
import com.example.backend.model.Card.Rank;
import com.example.backend.model.Card.Suit;
import com.example.backend.model.Player;
import com.example.backend.service.GameLifecycleService;
import com.example.backend.service.GameValidatorService;
import com.example.backend.repository.GameRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.publisher.GameEventPublisher;
import com.example.backend.service.GameActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GameEventSanitizerTest {

    private Player alice;
    private Player bob;
    private Player carol;

    @BeforeEach
    void setUp() {
        alice = seat("alice", Rank.SEVEN);
        bob = seat("bob", Rank.NINE);
        carol = seat("carol", Rank.JACK);
    }

    private static Player seat(String name, Rank r) {
        Player p = new Player(name, name, 1000);
        p.setHand(new ArrayList<>(List.of(new Card(Suit.SPADES, r), new Card(Suit.HEARTS, r))));
        return p;
    }

    private Game game(Game.GameStatus status) {
        Game g = new Game(10, 20);
        g.getPlayers().addAll(List.of(alice, bob, carol));
        g.setStatus(status);
        return g;
    }

    @Test
    void anEventTypeItDoesNotKnowIsNotStored() {
        GameEvent unknown = new GameEvent("g1") { };

        assertNull(GameEventSanitizer.sanitize(unknown));
    }

    @Test
    void theOriginalTimestampIsKeptSoTheLogStaysInOrder() {
        PlayerJoinedEvent joined = new PlayerJoinedEvent("g1", alice);
        OffsetDateTime earlier = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        joined.setTimestamp(earlier);

        GameEvent out = GameEventSanitizer.sanitize(joined);

        assertEquals(earlier, out.getTimestamp());
        assertTrue(((PlayerJoinedEvent) out).getPlayer().getHand().isEmpty());
        assertEquals(2, alice.getHand().size(), "the input is not modified");
    }

    @Test
    void publicCopyIsStableWhenAppliedTwice() {
        carol.setHasFolded(true);
        Game once = game(Game.GameStatus.SHOWDOWN).publicCopy();
        Game twice = once.publicCopy();

        for (int i = 0; i < 3; i++) {
            assertEquals(once.getPlayers().get(i).getHand().size(), twice.getPlayers().get(i).getHand().size());
        }
    }

    @Test
    void perPlayerViewShowsYourOwnCardsAndOthersOnlyAtARealShowdown() {
        GameLifecycleService service = new GameLifecycleService(mock(UserRepository.class), mock(GameRepository.class),
                mock(GameValidatorService.class), mock(GameEventPublisher.class), mock(GameActionService.class));

        // mid-hand: only alice's own cards
        Game midHand = service.getGameForPlayer(game(Game.GameStatus.FLOP_BETTING), alice.getId());
        assertEquals(2, midHand.getPlayers().get(0).getHand().size());
        assertTrue(midHand.getPlayers().get(1).getHand().isEmpty());
        assertTrue(midHand.getPlayers().get(2).getHand().isEmpty());
        assertNull(midHand.getDeck());
    }

    @Test
    void atShowdownYouStillSeeYourOwnFoldedCardsButNotOtherFoldedPlayers() {
        GameLifecycleService service = new GameLifecycleService(mock(UserRepository.class), mock(GameRepository.class),
                mock(GameValidatorService.class), mock(GameEventPublisher.class), mock(GameActionService.class));
        alice.setHasFolded(true);
        carol.setHasFolded(true);
        // bob is the only one left and alice + carol folded: nobody else is revealed to alice except herself
        Game view = service.getGameForPlayer(game(Game.GameStatus.SHOWDOWN), alice.getId());

        assertEquals(2, view.getPlayers().get(0).getHand().size(), "alice sees her own");
        assertTrue(view.getPlayers().get(1).getHand().isEmpty(), "bob won by fold, not shown");
        assertTrue(view.getPlayers().get(2).getHand().isEmpty(), "carol folded");
    }
}
