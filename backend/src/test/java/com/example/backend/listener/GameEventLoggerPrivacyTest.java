package com.example.backend.listener;

import com.example.backend.entity.Game;
import com.example.backend.entity.GameEvent;
import com.example.backend.event.CardsDealtEvent;
import com.example.backend.event.GameEndedEvent;
import com.example.backend.event.PlayerActionEvent;
import com.example.backend.event.RoundStartedEvent;
import com.example.backend.model.BettingRound;
import com.example.backend.model.Card;
import com.example.backend.model.Card.Rank;
import com.example.backend.model.Card.Suit;
import com.example.backend.model.GameUpdate;
import com.example.backend.model.Player;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.BotService;
import com.example.backend.service.GameNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * What the event log stores, and what the GAME_ENDED broadcast carries, must not show cards a
 * player is not entitled to see: the deck, anyone's hole cards before a showdown, or the hole
 * cards of a player who folded.
 */
class GameEventLoggerPrivacyTest {

    private MongoTemplate mongo;
    private GameNotificationService notifications;
    private GameEventLogger logger;
    private Player alice;
    private Player bob;
    private Player carol;

    @BeforeEach
    void setUp() {
        mongo = mock(MongoTemplate.class);
        notifications = mock(GameNotificationService.class);
        logger = new GameEventLogger(mongo, new ObjectMapper(), notifications, mock(BotService.class),
                mock(UserRepository.class));
        alice = seat("alice", Rank.SEVEN, Rank.EIGHT);
        bob = seat("bob", Rank.NINE, Rank.TEN);
        carol = seat("carol", Rank.JACK, Rank.QUEEN);
    }

    private static Player seat(String name, Rank a, Rank b) {
        Player p = new Player(name, name, 1000);
        p.setHand(new ArrayList<>(List.of(new Card(Suit.SPADES, a), new Card(Suit.HEARTS, b))));
        return p;
    }

    private Game game(Game.GameStatus status) {
        Game g = new Game(10, 20);
        g.getPlayers().addAll(List.of(alice, bob, carol));
        g.setStatus(status);
        g.setCommunityCards(new ArrayList<>(List.of(new Card(Suit.CLUBS, Rank.TWO), new Card(Suit.CLUBS, Rank.THREE),
                new Card(Suit.CLUBS, Rank.FOUR))));
        return g;
    }

    private Object savedEventData() {
        ArgumentCaptor<GameEvent> saved = ArgumentCaptor.forClass(GameEvent.class);
        verify(mongo).save(saved.capture(), eq("game_events"));
        return saved.getValue().getEventData();
    }

    private static void assertNoHands(Game g) {
        assertNull(g.getDeck(), "deck must not be stored");
        g.getPlayers().forEach(p -> assertTrue(p.getHand().isEmpty(), p.getUsername() + " hand leaked"));
    }

    @Test
    void roundStartedStoresNeitherTheDeckNorAnyHand() {
        Game live = game(Game.GameStatus.FLOP_BETTING);
        logger.logGameEvent(new RoundStartedEvent(live.getId(), live, BettingRound.RoundType.FLOP));

        assertNoHands(((RoundStartedEvent) savedEventData()).getGame());
    }

    @Test
    void theLiveGameIsNotAlteredByLogging() {
        Game live = game(Game.GameStatus.FLOP_BETTING);
        logger.logGameEvent(new RoundStartedEvent(live.getId(), live, BettingRound.RoundType.FLOP));

        assertNotNull(live.getDeck());
        assertEquals(2, alice.getHand().size());
    }

    @Test
    void privateDealStoresWhoWasDealtButNotWhatTheyHold() {
        CardsDealtEvent deal = new CardsDealtEvent("g1", Map.of(alice.getId(), alice.getHand(), bob.getId(), bob.getHand()));
        logger.logGameEvent(deal);

        CardsDealtEvent stored = (CardsDealtEvent) savedEventData();
        assertEquals(2, stored.getPlayerCards().size());
        stored.getPlayerCards().values().forEach(cards -> assertTrue(cards.isEmpty()));
    }

    @Test
    void communityCardsAreStoredBecauseTheyArePublic() {
        List<Card> board = List.of(new Card(Suit.CLUBS, Rank.TWO));
        logger.logGameEvent(new CardsDealtEvent("g1", CardsDealtEvent.DealType.FLOP, board));

        assertEquals(board, ((CardsDealtEvent) savedEventData()).getCommunityCards());
    }

    @Test
    void playerActionStoresNoHands() {
        Game live = game(Game.GameStatus.FLOP_BETTING);
        logger.logGameEvent(new PlayerActionEvent(live.getId(), alice, PlayerActionEvent.ActionType.CHECK, 0L, new Game(live)));

        PlayerActionEvent stored = (PlayerActionEvent) savedEventData();
        assertTrue(stored.getPlayer().getHand().isEmpty());
        assertNoHands(stored.getGameState());
    }

    @Test
    void showdownRevealsOnlyPlayersStillInTheHand() {
        carol.setHasFolded(true);
        Game atShowdown = new Game(game(Game.GameStatus.SHOWDOWN));
        alice.setBestHand(null);
        logger.logGameEvent(new GameEndedEvent(atShowdown.getId(), atShowdown, List.of(alice), null));

        Game stored = ((GameEndedEvent) savedEventData()).getGame();
        assertNull(stored.getDeck());
        assertEquals(2, stored.getPlayers().get(0).getHand().size(), "alice showed down");
        assertEquals(2, stored.getPlayers().get(1).getHand().size(), "bob showed down");
        assertTrue(stored.getPlayers().get(2).getHand().isEmpty(), "carol folded, her cards stay hidden");
    }

    @Test
    void winningByEveryoneFoldingRevealsNothing() {
        bob.setHasFolded(true);
        carol.setHasFolded(true);
        Game atShowdown = new Game(game(Game.GameStatus.SHOWDOWN));
        GameEndedEvent ended = new GameEndedEvent(atShowdown.getId(), atShowdown, List.of(alice), null);
        logger.logGameEvent(ended); // both listeners see every event in the real app
        logger.onGameEnded(ended);

        // stored copy
        assertNoHands(((GameEndedEvent) savedEventData()).getGame());
        // live broadcast: the winner's hole cards were being sent to the whole table
        ArgumentCaptor<GameUpdate> update = ArgumentCaptor.forClass(GameUpdate.class);
        verify(notifications).notifyGameUpdate(update.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) update.getValue().getPayload();
        @SuppressWarnings("unchecked")
        List<Player> winners = (List<Player>) payload.get("winners");
        assertTrue(winners.get(0).getHand().isEmpty(), "winner's hole cards leaked");
        assertNull(payload.get("bestHand"));
    }
}
