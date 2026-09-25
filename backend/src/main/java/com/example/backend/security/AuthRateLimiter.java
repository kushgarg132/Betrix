package com.example.backend.security;

import graphql.ErrorClassification;
import graphql.GraphqlErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Fixed-window limit on login / register / guestLogin per client IP. Counted per resolved field,
 * so aliasing many logins into one GraphQL request does not get around it.
 * ponytail: in-memory, single instance; move to a shared store if the backend is ever scaled out.
 */
@Component
public class AuthRateLimiter {

    private static final long WINDOW_MS = 60_000;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxPerWindow;
    private final LongSupplier clock;
    private final Map<String, long[]> windows = new ConcurrentHashMap<>(); // key -> {windowStart, count}

    @Autowired
    public AuthRateLimiter(@Value("${app.rate-limit.auth-per-minute:20}") int maxPerWindow) {
        this(maxPerWindow, System::currentTimeMillis);
    }

    AuthRateLimiter(int maxPerWindow, LongSupplier clock) {
        this.maxPerWindow = maxPerWindow;
        this.clock = clock;
    }

    /** A null key (client address unknown) is not limited: sharing one bucket would let anyone lock everyone out. */
    public void check(String key) {
        if (key == null) {
            return;
        }
        long now = clock.getAsLong();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w[0] >= WINDOW_MS);
        }
        long[] w = windows.compute(key, (k, cur) -> {
            if (cur == null || now - cur[0] >= WINDOW_MS) {
                return new long[] {now, 1};
            }
            cur[1]++;
            return cur;
        });
        if (w[1] > maxPerWindow) {
            throw GraphqlErrorException.newErrorException()
                    .message("Too many attempts. Try again in a minute.")
                    .errorClassification(ErrorClassification.errorClassification("TOO_MANY_REQUESTS"))
                    .build();
        }
    }
}
