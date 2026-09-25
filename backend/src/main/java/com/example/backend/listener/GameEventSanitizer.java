package com.example.backend.listener;

import com.example.backend.entity.Game;
import com.example.backend.event.CardsDealtEvent;
import com.example.backend.event.GameEndedEvent;
import com.example.backend.event.GameEvent;
import com.example.backend.event.GameStartedEvent;
import com.example.backend.event.PlayerActionEvent;
import com.example.backend.event.PlayerJoinedEvent;
import com.example.backend.event.RoundStartedEvent;
import com.example.backend.model.Card;
import com.example.backend.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an internal game event into the version that may be stored or broadcast: no deck, and
 * hole cards only where Game.showdownRevealsHands allows. Events carry live game state, so the
 * event log used to hold the whole deck and every player's cards.
 * Fails closed: an event type not handled here is not stored.
 */
public final class GameEventSanitizer {
    private static final Logger logger = LoggerFactory.getLogger(GameEventSanitizer.class);

    private GameEventSanitizer() {
    }

    /** A redacted copy of the event, or null when the type is unknown. The input is never modified. */
    public static GameEvent sanitize(GameEvent event) {
        GameEvent out = switch (event) {
            case GameStartedEvent e -> new GameStartedEvent(e.getGameId(), publicGame(e.getGame()));
            case RoundStartedEvent e -> new RoundStartedEvent(e.getGameId(), publicGame(e.getGame()), e.getRoundType());
            case PlayerActionEvent e -> new PlayerActionEvent(e.getGameId(), hidden(e.getPlayer()),
                    e.getActionType(), e.getAmount(), publicGame(e.getGameState()));
            case PlayerJoinedEvent e -> new PlayerJoinedEvent(e.getGameId(), hidden(e.getPlayer()));
            case GameEndedEvent e -> ended(e);
            case CardsDealtEvent e -> dealt(e);
            default -> {
                logger.warn("Not storing unknown event type {}: add it to GameEventSanitizer", event.getClass().getSimpleName());
                yield null;
            }
        };
        if (out != null) {
            out.setTimestamp(event.getTimestamp());
        }
        return out;
    }

    private static Game publicGame(Game game) {
        return game == null ? null : game.publicCopy();
    }

    private static Player hidden(Player player) {
        if (player == null) {
            return null;
        }
        Player copy = new Player(player);
        copy.hideDetails();
        return copy;
    }

    private static GameEndedEvent ended(GameEndedEvent e) {
        boolean revealed = e.getGame() != null && e.getGame().showdownRevealsHands();
        List<Player> winners = e.getWinners() == null ? null : e.getWinners().stream()
                .map(w -> revealed ? new Player(w) : hidden(w))
                .toList();
        // a winner who won because everyone folded is not obliged to show, so no best hand either
        return new GameEndedEvent(e.getGameId(), publicGame(e.getGame()), winners, revealed ? e.getBestHand() : null);
    }

    private static CardsDealtEvent dealt(CardsDealtEvent e) {
        CardsDealtEvent out = new CardsDealtEvent(e.getGameId(), e.getDealType(), e.getCommunityCards());
        if (e.getPlayerCards() != null) {
            // keep who was dealt in, never what they were dealt
            Map<String, List<Card>> dealtTo = new HashMap<>();
            e.getPlayerCards().keySet().forEach(id -> dealtTo.put(id, List.of()));
            out.setPlayerCards(dealtTo);
        }
        return out;
    }
}
