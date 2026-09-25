package com.example.backend.service;

import java.util.function.LongSupplier;

/** At most {@code perMinute} acquisitions per rolling one-minute window; caps what the bots can spend on an external API. */
public class CallBudget {

    private static final long WINDOW_MS = 60_000;

    private final int perMinute;
    private final LongSupplier clock;
    private long windowStart;
    private int used;

    public CallBudget(int perMinute) {
        this(perMinute, System::currentTimeMillis);
    }

    CallBudget(int perMinute, LongSupplier clock) {
        this.perMinute = perMinute;
        this.clock = clock;
        this.windowStart = clock.getAsLong();
    }

    public synchronized boolean tryAcquire() {
        long now = clock.getAsLong();
        if (now - windowStart >= WINDOW_MS) {
            windowStart = now;
            used = 0;
        }
        if (used >= perMinute) {
            return false;
        }
        used++;
        return true;
    }
}
