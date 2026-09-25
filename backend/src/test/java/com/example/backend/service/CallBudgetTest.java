package com.example.backend.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallBudgetTest {

    @Test
    void allowsTheBudgetThenRefusesUntilTheMinuteRollsOver() {
        AtomicLong now = new AtomicLong(0);
        CallBudget budget = new CallBudget(2, now::get);

        assertTrue(budget.tryAcquire());
        assertTrue(budget.tryAcquire());
        assertFalse(budget.tryAcquire());

        now.addAndGet(60_000);
        assertTrue(budget.tryAcquire());
    }

    @Test
    void aBudgetOfZeroNeverAllows() {
        assertFalse(new CallBudget(0, () -> 0).tryAcquire());
    }
}
