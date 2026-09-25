package com.example.backend.service;

import com.example.backend.model.Card;
import com.example.backend.model.Card.Rank;
import com.example.backend.model.Card.Suit;
import com.example.backend.model.HandResult;
import com.example.backend.model.HandResult.HandRank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandEvaluatorTest {

    private final HandEvaluator evaluator = new HandEvaluator();

    private static Card c(Rank r, Suit s) {
        return new Card(s, r);
    }

    private HandResult eval(List<Card> hole, List<Card> board) {
        return evaluator.evaluateHand(hole, board);
    }

    /** true when hand a strictly beats hand b (rank first, then high cards in order). */
    private static boolean beats(HandResult a, HandResult b) {
        if (a.getRank() != b.getRank()) {
            return a.getRank().ordinal() > b.getRank().ordinal();
        }
        for (int i = 0; i < a.getHighCards().size(); i++) {
            int x = a.getHighCards().get(i).getRank().getValue();
            int y = b.getHighCards().get(i).getRank().getValue();
            if (x != y) {
                return x > y;
            }
        }
        return false;
    }

    @Test
    void royalFlushIsRoyalFlush() {
        HandResult r = eval(
            List.of(c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.SPADES)),
            List.of(c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES), c(Rank.TEN, Suit.SPADES),
                    c(Rank.TWO, Suit.HEARTS), c(Rank.NINE, Suit.CLUBS)));
        assertEquals(HandRank.ROYAL_FLUSH, r.getRank());
    }

    @Test
    void royalFlushBeatsFullHouse() {
        HandResult royal = eval(
            List.of(c(Rank.ACE, Suit.SPADES), c(Rank.KING, Suit.SPADES)),
            List.of(c(Rank.QUEEN, Suit.SPADES), c(Rank.JACK, Suit.SPADES), c(Rank.TEN, Suit.SPADES)));
        HandResult fullHouse = eval(
            List.of(c(Rank.NINE, Suit.HEARTS), c(Rank.NINE, Suit.CLUBS)),
            List.of(c(Rank.NINE, Suit.DIAMONDS), c(Rank.FOUR, Suit.HEARTS), c(Rank.FOUR, Suit.CLUBS)));
        assertTrue(beats(royal, fullHouse));
    }

    @Test
    void wheelStraightLosesToSixHighStraight() {
        HandResult wheel = eval(
            List.of(c(Rank.ACE, Suit.HEARTS), c(Rank.TWO, Suit.CLUBS)),
            List.of(c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.SPADES), c(Rank.FIVE, Suit.HEARTS)));
        HandResult sixHigh = eval(
            List.of(c(Rank.SIX, Suit.HEARTS), c(Rank.TWO, Suit.CLUBS)),
            List.of(c(Rank.THREE, Suit.DIAMONDS), c(Rank.FOUR, Suit.SPADES), c(Rank.FIVE, Suit.HEARTS)));
        assertEquals(HandRank.STRAIGHT, wheel.getRank());
        assertTrue(beats(sixHigh, wheel));
    }

    @Test
    void wheelStraightFlushIsNotRoyal() {
        HandResult r = eval(
            List.of(c(Rank.ACE, Suit.CLUBS), c(Rank.TWO, Suit.CLUBS)),
            List.of(c(Rank.THREE, Suit.CLUBS), c(Rank.FOUR, Suit.CLUBS), c(Rank.FIVE, Suit.CLUBS)));
        assertEquals(HandRank.STRAIGHT_FLUSH, r.getRank());
    }

    @Test
    void wheelStraightFlushLosesToSixHighStraightFlush() {
        HandResult wheel = eval(
            List.of(c(Rank.ACE, Suit.CLUBS), c(Rank.TWO, Suit.CLUBS)),
            List.of(c(Rank.THREE, Suit.CLUBS), c(Rank.FOUR, Suit.CLUBS), c(Rank.FIVE, Suit.CLUBS)));
        HandResult sixHigh = eval(
            List.of(c(Rank.SIX, Suit.HEARTS), c(Rank.TWO, Suit.HEARTS)),
            List.of(c(Rank.THREE, Suit.HEARTS), c(Rank.FOUR, Suit.HEARTS), c(Rank.FIVE, Suit.HEARTS)));
        assertTrue(beats(sixHigh, wheel));
    }

    @Test
    void ordinaryHandsStillRankCorrectly() {
        assertEquals(HandRank.FULL_HOUSE, eval(
            List.of(c(Rank.NINE, Suit.HEARTS), c(Rank.NINE, Suit.CLUBS)),
            List.of(c(Rank.NINE, Suit.DIAMONDS), c(Rank.FOUR, Suit.HEARTS), c(Rank.FOUR, Suit.CLUBS))).getRank());
        assertEquals(HandRank.FLUSH, eval(
            List.of(c(Rank.ACE, Suit.HEARTS), c(Rank.NINE, Suit.HEARTS)),
            List.of(c(Rank.FOUR, Suit.HEARTS), c(Rank.SEVEN, Suit.HEARTS), c(Rank.TWO, Suit.HEARTS))).getRank());
        assertEquals(HandRank.STRAIGHT, eval(
            List.of(c(Rank.TEN, Suit.HEARTS), c(Rank.NINE, Suit.CLUBS)),
            List.of(c(Rank.EIGHT, Suit.DIAMONDS), c(Rank.SEVEN, Suit.HEARTS), c(Rank.SIX, Suit.CLUBS))).getRank());
    }
}
