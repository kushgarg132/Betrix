package com.example.gamblingsitebackend.Logics;

import java.util.*;

public class PokerWinnerLogic {

    // Enum for poker hand rankings
    enum HandRank {
        HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_A_KIND,
        STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH, ROYAL_FLUSH
    }

    public static class Player {
        public String name;
        public List<String> hand;

        public Player(String name, List<String> hand) {
            this.name = name;
            this.hand = hand;
        }
    }

    public static Player determineWinner(List<Player> players) {
        Player bestPlayer = players.get(0);
        HandRank bestRank = evaluateHand(bestPlayer.hand);

        for (Player player : players) {
            HandRank rank = evaluateHand(player.hand);
            if (rank.ordinal() > bestRank.ordinal()) {
                bestPlayer = player;
                bestRank = rank;
            }
        }

        return bestPlayer;
    }

    public static HandRank evaluateHand(List<String> hand) {
        if (isRoyalFlush(hand)) return HandRank.ROYAL_FLUSH;
        if (isStraightFlush(hand)) return HandRank.STRAIGHT_FLUSH;
        if (isFourOfAKind(hand)) return HandRank.FOUR_OF_A_KIND;
        return HandRank.HIGH_CARD; // Default hand rank
    }

    private static boolean isRoyalFlush(List<String> hand) {
        return hand.contains("AS") && hand.contains("KS") && hand.contains("QS") &&
                hand.contains("JS") && hand.contains("10S");
    }

    private static boolean isStraightFlush(List<String> hand) {
        // Add logic for Straight Flush
        return false;
    }

    private static boolean isFourOfAKind(List<String> hand) {
        // Add logic for Four of a Kind
        return false;
    }
}