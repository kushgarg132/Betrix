package com.example.backend.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * One lock per game. Every action loads the whole game, changes it and saves it, and user requests,
 * turn timers, bots and the hand starter all do that from different threads; without this two of them
 * overwrite each other's changes. Reentrant, so a service can call another service for the same game.
 * ponytail: in-process, so single instance only. Running two backend instances needs a shared lock
 * (or a version field on Game plus retry).
 */
@Component
public class GameLocks {

    // one small entry per game id ever locked; games are deleted when empty, so this stays tiny
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T run(String gameId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(gameId, id -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    public void run(String gameId, Runnable action) {
        run(gameId, () -> {
            action.run();
            return null;
        });
    }
}
