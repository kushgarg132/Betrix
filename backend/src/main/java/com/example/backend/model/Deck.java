package com.example.backend.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Data
public class Deck {
    private List<Card> cards;
    public static final int STANDARD_DECK_SIZE = 52;

    public Deck() {
        cards = new ArrayList<>(STANDARD_DECK_SIZE);
        initializeDeck();
    }

    private void initializeDeck() {
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards, ThreadLocalRandom.current());
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Deck is empty");
        }
        return cards.remove(0);
    }
    
    public int remainingCards() {
        return cards.size();
    }
    
    public void reset() {
        cards.clear();
        initializeDeck();
    }
}