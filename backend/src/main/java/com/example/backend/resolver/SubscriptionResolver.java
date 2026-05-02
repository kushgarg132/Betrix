package com.example.backend.resolver;

import com.example.backend.model.GameUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class SubscriptionResolver {

    // Game-level sinks: one per gameId
    private static final Map<String, Sinks.Many<GameUpdate>> gameSinks = new ConcurrentHashMap<>();

    // Player-level sinks: key = "gameId:playerId"
    private static final Map<String, Sinks.Many<GameUpdate>> playerSinks = new ConcurrentHashMap<>();

    @SubscriptionMapping
    public Flux<GameUpdate> gameUpdated(@Argument String gameId) {
        return getOrCreateGameSink(gameId).asFlux();
    }

    @SubscriptionMapping
    public Flux<GameUpdate> playerUpdated(@Argument String gameId, @Argument String playerId) {
        String key = gameId + ":" + playerId;
        return getOrCreatePlayerSink(key).asFlux();
    }

    public static Sinks.Many<GameUpdate> getOrCreateGameSink(String gameId) {
        return gameSinks.computeIfAbsent(gameId,
                k -> Sinks.many().multicast().onBackpressureBuffer());
    }

    public static Sinks.Many<GameUpdate> getOrCreatePlayerSink(String key) {
        return playerSinks.computeIfAbsent(key,
                k -> Sinks.many().multicast().onBackpressureBuffer());
    }

    public static void publishGameUpdate(String gameId, GameUpdate update) {
        Sinks.Many<GameUpdate> sink = gameSinks.get(gameId);
        if (sink != null) {
            sink.tryEmitNext(update);
        }
    }

    public static void publishPlayerUpdate(String gameId, String playerId, GameUpdate update) {
        String key = gameId + ":" + playerId;
        Sinks.Many<GameUpdate> sink = playerSinks.get(key);
        if (sink != null) {
            sink.tryEmitNext(update);
        }
    }

    public static void removeGameSink(String gameId) {
        Sinks.Many<GameUpdate> sink = gameSinks.remove(gameId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    public static void cleanupGameSinks(String gameId) {
        removeGameSink(gameId);
        playerSinks.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(gameId + ":")) {
                e.getValue().tryEmitComplete();
                return true;
            }
            return false;
        });
    }
}
