
package com.example.backend.service;

import com.example.backend.model.Card;
import lombok.Data;
import java.util.*;
import java.util.stream.Collectors;

public class HandEvaluator {
    @Data
    public static class HandRank implements Comparable<HandRank> {
        private HandType type;
        private List<Card> cards;
        private int value;

        public enum HandType {
            HIGH_CARD(1),
            PAIR(2),
            TWO_PAIR(3),
            THREE_OF_A_KIND(4),
            STRAIGHT(5),
            FLUSH(6),
            FULL_HOUSE(7),
            FOUR_OF_A_KIND(8),
            STRAIGHT_FLUSH(9),
            ROYAL_FLUSH(10);

            private final int value;

            HandType(int value) {
                this.value = value;
            }
        }

        @Override
        public int compareTo(HandRank other) {
            if (this.type.value != other.type.value) {
                return Integer.compare(this.type.value, other.type.value);
            }
            return Integer.compare(this.value, other.value);
        }
    }

    public static HandRank evaluate(List<Card> playerCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(playerCards);
        allCards.addAll(communityCards);

        HandRank bestHand = new HandRank();
        bestHand.setType(HandRank.HandType.HIGH_CARD);
        bestHand.setCards(new ArrayList<>());

        // Check for flush
        Map<Card.Suit, List<Card>> suitGroups = allCards.stream()
                .collect(Collectors.groupingBy(Card::getSuit));
        
        for (List<Card> suitedCards : suitGroups.values()) {
            if (suitedCards.size() >= 5) {
                HandRank flushHand = checkForStraightOrRoyalFlush(suitedCards);
                if (flushHand != null) {
                    return flushHand;
                }
                bestHand.setType(HandRank.HandType.FLUSH);
                bestHand.setCards(suitedCards.stream()
                        .sorted((c1, c2) -> Integer.compare(c2.getRank().getValue(), c1.getRank().getValue()))
                        .limit(5)
                        .collect(Collectors.toList()));
            }
        }

        // Check for straight
        HandRank straightHand = checkForStraight(allCards);
        if (straightHand != null && straightHand.getType().value > bestHand.getType().value) {
            bestHand = straightHand;
        }

        // Check for pairs, three of a kind, etc.
        Map<Card.Rank, List<Card>> rankGroups = allCards.stream()
                .collect(Collectors.groupingBy(Card::getRank));
        
        HandRank groupedHand = evaluateGroupedCards(rankGroups);
        if (groupedHand.getType().value > bestHand.getType().value) {
            bestHand = groupedHand;
        }

        return bestHand;
    }

    private static HandRank checkForStraightOrRoyalFlush(List<Card> cards) {
        HandRank straightFlush = checkForStraight(cards);
        if (straightFlush != null) {
            if (straightFlush.getCards().get(0).getRank() == Card.Rank.ACE) {
                straightFlush.setType(HandRank.HandType.ROYAL_FLUSH);
            } else {
                straightFlush.setType(HandRank.HandType.STRAIGHT_FLUSH);
            }
            return straightFlush;
        }
        return null;
    }

    private static HandRank checkForStraight(List<Card> cards) {
        List<Card> sortedCards = cards.stream()
                .sorted((c1, c2) -> Integer.compare(c2.getRank().getValue(), c1.getRank().getValue()))
                .distinct()
                .collect(Collectors.toList());

        for (int i = 0; i <= sortedCards.size() - 5; i++) {
            if (isStraight(sortedCards.subList(i, i + 5))) {
                HandRank hand = new HandRank();
                hand.setType(HandRank.HandType.STRAIGHT);
                hand.setCards(new ArrayList<>(sortedCards.subList(i, i + 5)));
                hand.setValue(sortedCards.get(i).getRank().getValue());
                return hand;
            }
        }
        return null;
    }

    private static boolean isStraight(List<Card> cards) {
        for (int i = 0; i < cards.size() - 1; i++) {
            if (cards.get(i).getRank().getValue() != cards.get(i + 1).getRank().getValue() + 1) {
                return false;
            }
        }
        return true;
    }

    private static HandRank evaluateGroupedCards(Map<Card.Rank, List<Card>> rankGroups) {
        HandRank hand = new HandRank();
        List<Card> cards = new ArrayList<>();

        // Check for four of a kind
        Optional<Map.Entry<Card.Rank, List<Card>>> fourOfKind = rankGroups.entrySet().stream()
                .filter(entry -> entry.getValue().size() == 4)
                .findFirst();

        if (fourOfKind.isPresent()) {
            hand.setType(HandRank.HandType.FOUR_OF_A_KIND);
            cards.addAll(fourOfKind.get().getValue());
            hand.setValue(fourOfKind.get().getKey().getValue());
        } else {
            // Check for full house
            Optional<Map.Entry<Card.Rank, List<Card>>> threeOfKind = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 3)
                    .findFirst();

            Optional<Map.Entry<Card.Rank, List<Card>>> pair = rankGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().size() == 2)
                    .findFirst();

            if (threeOfKind.isPresent() && pair.isPresent()) {
                hand.setType(HandRank.HandType.FULL_HOUSE);
                cards.addAll(threeOfKind.get().getValue());
                cards.addAll(pair.get().getValue());
                hand.setValue(threeOfKind.get().getKey().getValue());
            } else if (threeOfKind.isPresent()) {
                hand.setType(HandRank.HandType.THREE_OF_A_KIND);
                cards.addAll(threeOfKind.get().getValue());
                hand.setValue(threeOfKind.get().getKey().getValue());
            } else {
                List<Map.Entry<Card.Rank, List<Card>>> pairs = rankGroups.entrySet().stream()
                        .filter(entry -> entry.getValue().size() == 2)
                        .collect(Collectors.toList());

                if (pairs.size() >= 2) {
                    hand.setType(HandRank.HandType.TWO_PAIR);
                    cards.addAll(pairs.get(0).getValue());
                    cards.addAll(pairs.get(1).getValue());
                    hand.setValue(Math.max(pairs.get(0).getKey().getValue(), 
                                        pairs.get(1).getKey().getValue()));
                } else if (pairs.size() == 1) {
                    hand.setType(HandRank.HandType.PAIR);
                    cards.addAll(pairs.get(0).getValue());
                    hand.setValue(pairs.get(0).getKey().getValue());
                } else {
                    hand.setType(HandRank.HandType.HIGH_CARD);
                    cards.add(rankGroups.entrySet().stream()
                            .max((e1, e2) -> Integer.compare(
                                    e1.getKey().getValue(),
                                    e2.getKey().getValue()))
                            .get().getValue().get(0));
                    hand.setValue(cards.get(0).getRank().getValue());
                }
            }
        }

        hand.setCards(cards);
        return hand;
    }
}