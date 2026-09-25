package com.example.backend.resolver;

import com.example.backend.config.WebSocketAuthInterceptor;
import com.example.backend.entity.Game;
import com.example.backend.model.GameUpdate;
import com.example.backend.security.CurrentUser;
import com.example.backend.service.GameValidatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
public class SubscriptionResolver {

    private final GameValidatorService gameValidatorService;

    // Game-level sinks: one per gameId
    private static final Map<String, Sinks.Many<GameUpdate>> gameSinks = new ConcurrentHashMap<>();

    // Player-level sinks: key = "gameId:playerId"
    private static final Map<String, Sinks.Many<GameUpdate>> playerSinks = new ConcurrentHashMap<>();

    @SubscriptionMapping
    public Flux<GameUpdate> gameUpdated(@Argument String gameId) {
        return getOrCreateGameSink(gameId).asFlux();
    }

    @SubscriptionMapping
    public Flux<GameUpdate> playerUpdated(@Argument String gameId,
            @ContextValue(name = WebSocketAuthInterceptor.USERNAME, required = false) String wsUsername) {
        // private stream (hole cards): only the seat's own owner may listen, identified from
        // whichever side actually carries auth for this transport (see WebSocketAuthInterceptor)
        String caller = wsUsername != null ? wsUsername : CurrentUser.username();
        Game game = gameValidatorService.validateGameExists(gameId);
        String playerId = gameValidatorService.requireOwnPlayer(game, caller).getId();
        String key = gameId + ":" + playerId;
        return getOrCreatePlayerSink(key).asFlux();
    }

    // directBestEffort: unlike onBackpressureBuffer() it is not cancelled when the last subscriber leaves
    // (a page refresh did that, and the game's stream then stayed dead), and it does not hold early
    // updates to replay to whoever subscribes next.
    private static Sinks.Many<GameUpdate> newSink() {
        return Sinks.many().multicast().directBestEffort();
    }

    // Emitting from two threads at once fails with FAIL_NON_SERIALIZED and drops the update.
    private static void emit(Sinks.Many<GameUpdate> sink, GameUpdate update) {
        synchronized (sink) {
            sink.tryEmitNext(update);
        }
    }

    public static Sinks.Many<GameUpdate> getOrCreateGameSink(String gameId) {
        return gameSinks.computeIfAbsent(gameId,
                k -> newSink());
    }

    public static Sinks.Many<GameUpdate> getOrCreatePlayerSink(String key) {
        return playerSinks.computeIfAbsent(key,
                k -> newSink());
    }

    public static void publishGameUpdate(String gameId, GameUpdate update) {
        Sinks.Many<GameUpdate> sink = gameSinks.get(gameId);
        if (sink != null) {
            emit(sink, update);
        }
    }

    public static void publishPlayerUpdate(String gameId, String playerId, GameUpdate update) {
        String key = gameId + ":" + playerId;
        Sinks.Many<GameUpdate> sink = playerSinks.get(key);
        if (sink != null) {
            emit(sink, update);
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
