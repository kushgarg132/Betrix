package com.example.backend.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DeckTest {

    @Test
    void aFreshDeckHas52DistinctCards() {
        Deck deck = new Deck();
        Set<Card> seen = new HashSet<>();
        for (int i = 0; i < Deck.STANDARD_DECK_SIZE; i++) {
            assertEquals(true, seen.add(deck.drawCard()));
        }
        assertEquals(52, seen.size());
    }

    @Test
    void twoFreshDecksAreShuffledDifferently() {
        // Not a proof of randomness, but a sanity check that shuffle() is doing something and
        // that it is not deterministic across instances.
        assertNotEquals(new Deck().getCards(), new Deck().getCards());
    }
}
