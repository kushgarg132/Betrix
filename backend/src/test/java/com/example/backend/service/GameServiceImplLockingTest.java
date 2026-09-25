package com.example.backend.service;

import com.example.backend.entity.Game;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Every user action, timer, and bot goes through this facade. Concurrent calls for one game must run
 * one at a time, or two load-mutate-save cycles overwrite each other's changes.
 */
class GameServiceImplLockingTest {

    private final AtomicInteger inside = new AtomicInteger();
    private final AtomicInteger maxInside = new AtomicInteger();

    private void enter() {
        maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
        try { Thread.sleep(15); } catch (InterruptedException ignored) { }
        inside.decrementAndGet();
    }

    @Test
    void everyMutatingOperationOnOneGameIsSerialized() throws Exception {
        GameLifecycleService lifecycle = mock(GameLifecycleService.class);
        GameHandService hand = mock(GameHandService.class);
        GameActionService action = mock(GameActionService.class);
        when(lifecycle.joinGame(anyString(), anyString())).thenAnswer(i -> { enter(); return new Game(1, 2); });
        when(lifecycle.deleteGame(anyString())).thenAnswer(i -> { enter(); return true; });
        doAnswer(i -> { enter(); return null; }).when(lifecycle).leaveGame(anyString(), anyString());
        doAnswer(i -> { enter(); return null; }).when(lifecycle).sitOut(anyString(), anyString());
        doAnswer(i -> { enter(); return null; }).when(lifecycle).sitIn(anyString(), anyString());
        doAnswer(i -> { enter(); return null; }).when(hand).startNewHand(anyString());
        doAnswer(i -> { enter(); return null; }).when(hand).executeAllInAction(anyString());
        doAnswer(i -> { enter(); return null; }).when(action).placeBet(anyString(), anyString(), anyLong());
        doAnswer(i -> { enter(); return null; }).when(action).check(anyString(), anyString());
        doAnswer(i -> { enter(); return null; }).when(action).fold(anyString(), anyString());
        GameServiceImpl service = new GameServiceImpl(lifecycle, hand, action, new GameLocks());

        List<Consumer<GameServiceImpl>> operations = List.of(
                s -> s.joinGame("g", "u"), s -> s.leaveGame("g", "p"), s -> s.sitOut("g", "p"), s -> s.sitIn("g", "p"),
                s -> s.deleteGame("g"), s -> s.startNewHand("g"), s -> s.executeAllInAction("g"),
                s -> s.placeBet("g", "p", 10), s -> s.check("g", "p"), s -> s.fold("g", "p"));

        ExecutorService pool = Executors.newFixedThreadPool(operations.size());
        List<Future<?>> done = operations.stream().<Future<?>>map(op -> pool.submit(() -> op.accept(service))).toList();
        for (Future<?> f : done) f.get(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, maxInside.get(), "two operations on the same game ran at once");
    }
}
