package com.example.backend.security;

import graphql.GraphqlErrorException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthRateLimiterTest {

    private final AtomicLong now = new AtomicLong(1_000_000);
    private final AuthRateLimiter limiter = new AuthRateLimiter(3, now::get);

    @Test
    void allowsUpToTheLimitThenBlocksWithATooManyRequestsError() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.2.3.4");
        }
        var e = assertThrows(GraphqlErrorException.class, () -> limiter.check("1.2.3.4"));
        assertEquals("TOO_MANY_REQUESTS", e.getErrorType().toString());
    }

    @Test
    void clientsAreCountedSeparately() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.2.3.4");
        }
        assertDoesNotThrow(() -> limiter.check("5.6.7.8"));
    }

    @Test
    void theWindowResetsAfterAMinute() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.2.3.4");
        }
        now.addAndGet(60_000);
        assertDoesNotThrow(() -> limiter.check("1.2.3.4"));
    }

    @Test
    void anUnknownClientIsNeverLimitedSoNobodyCanLockEveryoneOut() {
        for (int i = 0; i < 100; i++) {
            limiter.check(null);
        }
    }
}
