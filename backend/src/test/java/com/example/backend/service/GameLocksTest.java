package com.example.backend.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLocksTest {

    private final GameLocks locks = new GameLocks();

    @Test
    void actionsOnTheSameGameNeverOverlap() throws Exception {
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<java.util.concurrent.Future<?>> done = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            done.add(pool.submit(() -> locks.run("g1", () -> {
                maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
                try { Thread.sleep(20); } catch (InterruptedException ignored) { }
                inside.decrementAndGet();
            })));
        }
        for (var f : done) f.get(5, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, maxInside.get());
    }

    @Test
    void differentGamesDoNotWaitForEachOther() throws Exception {
        CountDownLatch bothInside = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        var a = pool.submit(() -> locks.run("g1", () -> await(bothInside)));
        var b = pool.submit(() -> locks.run("g2", () -> await(bothInside)));

        a.get(5, TimeUnit.SECONDS); // would time out if g2 had to wait for g1
        b.get(5, TimeUnit.SECONDS);
        pool.shutdown();
    }

    private static void await(CountDownLatch latch) {
        latch.countDown();
        try {
            assertTrue(latch.await(3, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void nestedCallsOnTheSameGameDoNotDeadlock() {
        String result = locks.run("g1", () -> locks.run("g1", () -> "inner"));

        assertEquals("inner", result);
    }

    @Test
    void aFailingActionReleasesTheLock() {
        assertThrows(IllegalStateException.class, () -> locks.run("g1", () -> { throw new IllegalStateException("boom"); }));

        assertEquals("ok", locks.run("g1", () -> "ok")); // would hang forever if the lock leaked
    }
}
