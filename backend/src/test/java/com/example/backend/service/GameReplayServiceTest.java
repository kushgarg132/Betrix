package com.example.backend.service;

import com.example.backend.entity.Game;
import com.example.backend.entity.GameEvent;
import com.example.backend.event.GameStartedEvent;
import com.example.backend.event.PlayerJoinedEvent;
import com.example.backend.model.Player;
import com.example.backend.repository.GameEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Spring Data Mongo hands eventData back as a Map (a field typed Object, not the concrete event
 * class), never as JSON text. The old code called .toString() on that Map and parsed the result
 * as JSON, which a Map's toString() never is.
 */
class GameReplayServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private GameEvent stored(String type, Object event) {
        GameEvent e = new GameEvent();
        e.setId(type + "-1");
        e.setGameId("g1");
        e.setEventType(type);
        // realistic shape: what a Mongo document looks like once read back without a target type
        e.setEventData(new LinkedHashMap<>(objectMapper.convertValue(event, Map.class)));
        return e;
    }

    @Test
    void replaysAStoredGameStartedEventFollowedByAPlayerJoined() {
        Game started = new Game(10, 20);
        started.setId("g1");
        started.setDeck(null); // GameEventSanitizer strips the deck before anything is stored
        started.getPlayers().add(new Player("Alice", "alice", 1000));

        Player bob = new Player("Bob", "bob", 1000);

        GameEventRepository repo = mock(GameEventRepository.class);
        when(repo.findByGameIdOrderByTimestampAsc("g1")).thenReturn(List.of(
                stored("GameStartedEvent", new GameStartedEvent("g1", started)),
                stored("PlayerJoinedEvent", new PlayerJoinedEvent("g1", bob))));

        GameReplayService replay = new GameReplayService(repo, objectMapper);
        Game result = replay.replayGame("g1");

        assertNotNull(result);
        assertEquals(2, result.getPlayers().size());
        assertEquals("bob", result.getPlayers().get(1).getUsername());
    }
}
