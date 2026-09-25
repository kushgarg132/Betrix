package com.example.backend.resolver;

import com.example.backend.model.GameUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscriptionSinkTest {

    @AfterEach
    void cleanup() {
        SubscriptionResolver.cleanupGameSinks("g1");
    }

    private static GameUpdate update(String id) {
        return GameUpdate.builder().gameId(id).type(GameUpdate.GameUpdateType.PLAYER_ACTION).build();
    }

    /** A page refresh drops the only subscriber and then subscribes again; the stream must still work. */
    @Test
    void aGameStreamStillWorksAfterItsLastSubscriberLeft() {
        Disposable first = SubscriptionResolver.getOrCreateGameSink("g1").asFlux().subscribe();
        first.dispose();

        List<GameUpdate> received = new CopyOnWriteArrayList<>();
        SubscriptionResolver.getOrCreateGameSink("g1").asFlux().subscribe(received::add);
        SubscriptionResolver.publishGameUpdate("g1", update("g1"));

        assertEquals(1, received.size());
    }

    @Test
    void aNewSubscriberDoesNotReceiveUpdatesPublishedBeforeItArrived() {
        SubscriptionResolver.getOrCreateGameSink("g1");
        SubscriptionResolver.publishGameUpdate("g1", update("old"));

        List<GameUpdate> received = new CopyOnWriteArrayList<>();
        SubscriptionResolver.getOrCreateGameSink("g1").asFlux().subscribe(received::add);

        assertEquals(0, received.size());
    }

    @Test
    void concurrentPublishersDoNotDropUpdates() throws Exception {
        List<GameUpdate> received = new CopyOnWriteArrayList<>();
        SubscriptionResolver.getOrCreateGameSink("g1").asFlux().subscribe(received::add);

        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 50; i++) {
                    SubscriptionResolver.publishGameUpdate("g1", update("g1"));
                }
            });
            threads[t].start();
        }
        for (Thread t : threads) t.join();

        assertEquals(400, received.size());
    }
}
