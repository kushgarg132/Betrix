package com.example.backend.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterInputValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private int violations(String username, String password) {
        return validator.validate(new RegisterInput("Alice", username, password, "alice@example.com")).size();
    }

    @Test
    void acceptsOrdinaryUsernames() {
        assertEquals(0, violations("alice", "secret1"));
        assertEquals(0, violations("Alice_99", "secret1"));
    }

    @Test
    void rejectsUsernamesThatCouldPassForSystemPlayersOrBreakDisplay() {
        for (String bad : new String[] {"guest-a1b2c3", "bot-1", "has space", "dot.name", "ab", "a".repeat(31), "<script>", "émile"}) {
            assertTrue(violations(bad, "secret1") > 0, bad);
        }
    }

    @Test
    void rejectsPasswordsBcryptWouldSilentlyTruncate() {
        assertTrue(violations("alice", "x".repeat(73)) > 0);
        assertEquals(0, violations("alice", "x".repeat(72)));
    }
}
