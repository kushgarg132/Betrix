package com.example.backend.service;

import com.example.backend.model.Card;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HandEvaluator {
    
    public enum HandRank {
        HIGH_CARD,
        ONE_PAIR,
        TWO_PAIR,
        THREE_OF_A_KIND,
        STRAIGHT,
        FLUSH,
        FULL_HOUSE,
        FOUR_OF_A_KIND,
        STRAIGHT_FLUSH,
        ROYAL_FLUSH
    }
    
    public HandResult evaluateHand(List<Card> playerCards, List<Card> communityCards) {
        if (playerCards == null || playerCards.size() != 2) {
            throw new IllegalArgumentException("Player must have exactly 2 cards");
        }
        
        if (communityCards == null || communityCards.size() > 5) {
            throw new IllegalArgumentException("Community cards must be 5 or fewer");
        }
        
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(playerCards);
        allCards.addAll(communityCards);
        
        return findBestHand(allCards);
    }
    
    private HandResult findBestHand(List<Card> allCards) {
        List<List<Card>> combinations = generateCombinations(allCards, 5);
        
        HandResult bestHand = null;
        
        for (List<Card> combo : combinations) {
            HandResult current = evaluate5CardHand(combo);
            
            if (bestHand == null || current.getRank().ordinal() > bestHand.getRank().ordinal()) {
                bestHand = current;
            } else if (current.getRank().ordinal() == bestHand.getRank().ordinal()) {
                // Compare high cards
                for (int i = 0; i < current.getHighCards().size(); i++) {
                    int currentValue = current.getHighCards().get(i).getRank().getValue();
                    int bestValue = bestHand.getHighCards().get(i).getRank().getValue();
                    
                    if (currentValue > bestValue) {
                        bestHand = current;
                        break;
                    } else if (currentValue < bestValue) {
                        break;
                    }
                }
            }
        }
        
        return bestHand;
    }
    
    private List<List<Card>> generateCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        generateCombinationsHelper(cards, k, 0, new ArrayList<>(), result);
        return result;
    }
    
    private void generateCombinationsHelper(List<Card> cards, int k, int start, 
                                          List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinationsHelper(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
    private HandResult evaluate5CardHand(List<Card> hand) {
        if (hand.size() != 5) {
            throw new IllegalArgumentException("Hand must contain exactly 5 cards");
        }
        
        // Sort by rank in descending order
        hand.sort(Comparator.comparing(card -> card.getRank().getValue(), Comparator.reverseOrder()));
        
        boolean isFlush = isFlush(hand);
        boolean isStraight = isStraight(hand);
        
        // Count occurrences of each rank
        Map<Card.Rank, Integer> rankCounts = new HashMap<>();
        for (Card card : hand) {
            rankCounts.put(card.getRank(), rankCounts.getOrDefault(card.getRank(), 0) + 1);
        }
        
        List<Map.Entry<Card.Rank, Integer>> sortedCounts = rankCounts.entrySet().stream()
            .sorted((e1, e2) -> {
                int countComparison = e2.getValue().compareTo(e1.getValue());
                return countComparison != 0 ? countComparison : 
                    e2.getKey().getValue() - e1.getKey().getValue();
            })
            .collect(Collectors.toList());
        
        // Royal flush
        if (isFlush && isStraight && hand.get(0).getRank() == Card.Rank.ACE) {
            return new HandResult(HandRank.ROYAL_FLUSH, hand);
        }
        
        // Straight flush
        if (isFlush && isStraight) {
            return new HandResult(HandRank.STRAIGHT_FLUSH, hand);
        }
        
        // Four of a kind
        if (sortedCounts.get(0).getValue() == 4) {
            List<Card> highCards = new ArrayList<>();
            
            // Add the four of a kind cards first
            Card.Rank fourOfAKindRank = sortedCounts.get(0).getKey();
            for (Card card : hand) {
                if (card.getRank() == fourOfAKindRank) {
                    highCards.add(card);
                }
            }
            
            // Add the kicker
            for (Card card : hand) {
                if (card.getRank() != fourOfAKindRank) {
                    highCards.add(card);
                }
            }
            
            return new HandResult(HandRank.FOUR_OF_A_KIND, highCards);
        }
        
        // Full house
        if (sortedCounts.get(0).getValue() == 3 && sortedCounts.get(1).getValue() == 2) {
            List<Card> highCards = new ArrayList<>();
            
            // Add the three of a kind cards first
            Card.Rank threeOfAKindRank = sortedCounts.get(0).getKey();
            for (Card card : hand) {
                if (card.getRank() == threeOfAKindRank) {
                    highCards.add(card);
                }
            }
            
            // Add the pair cards
            Card.Rank pairRank = sortedCounts.get(1).getKey();
            for (Card card : hand) {
                if (card.getRank() == pairRank) {
                    highCards.add(card);
                }
            }
            
            return new HandResult(HandRank.FULL_HOUSE, highCards);
        }
        
        // Flush
        if (isFlush) {
            return new HandResult(HandRank.FLUSH, hand);
        }
        
        // Straight
        if (isStraight) {
            return new HandResult(HandRank.STRAIGHT, hand);
        }
        
        // Three of a kind
        if (sortedCounts.get(0).getValue() == 3) {
            List<Card> highCards = new ArrayList<>();
            
            // Add the three of a kind cards first
            Card.Rank threeOfAKindRank = sortedCounts.get(0).getKey();
            for (Card card : hand) {
                if (card.getRank() == threeOfAKindRank) {
                    highCards.add(card);
                }
            }
            
            // Add the remaining cards in order
            for (Card card : hand) {
                if (card.getRank() != threeOfAKindRank) {
                    highCards.add(card);
                }
            }
            
            return new HandResult(HandRank.THREE_OF_A_KIND, highCards);
        }
        
        // Two pair
        if (sortedCounts.get(0).getValue() == 2 && sortedCounts.get(1).getValue() == 2) {
            List<Card> highCards = new ArrayList<>();
            
            // Add the higher pair first
            Card.Rank firstPairRank = sortedCounts.get(0).getKey();
            for (Card card : hand) {
                if (card.getRank() == firstPairRank) {
                    highCards.add(card);
                }
            }
            
            // Add the lower pair
            Card.Rank secondPairRank = sortedCounts.get(1).getKey();
            for (Card card : hand) {
                if (card.getRank() == secondPairRank) {
                    highCards.add(card);
                }
            }
            
            // Add the kicker
            for (Card card : hand) {
                if (card.getRank() != firstPairRank && card.getRank() != secondPairRank) {
                    highCards.add(card);
                }
            }
            
            return new HandResult(HandRank.TWO_PAIR, highCards);
        }
        
        // One pair
        if (sortedCounts.get(0).getValue() == 2) {
            List<Card> highCards = new ArrayList<>();
            
            // Add the pair first
            Card.Rank pairRank = sortedCounts.get(0).getKey();
            for (Card card : hand) {
                if (card.getRank() == pairRank) {
                    highCards.add(card);
                }
            }
            
            // Add the remaining cards in order
            for (Card card : hand) {
                if (card.getRank() != pairRank) {
                    highCards.add(card);
                }
            }
            
            return new HandResult(HandRank.ONE_PAIR, highCards);
        }
        
        // High card
        return new HandResult(HandRank.HIGH_CARD, hand);
    }
    
    private boolean isFlush(List<Card> hand) {
        Card.Suit suit = hand.get(0).getSuit();
        for (Card card : hand) {
            if (card.getSuit() != suit) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isStraight(List<Card> hand) {
        // Special case: A-5-4-3-2
        if (hand.get(0).getRank() == Card.Rank.ACE && 
            hand.get(1).getRank() == Card.Rank.FIVE &&
            hand.get(2).getRank() == Card.Rank.FOUR &&
            hand.get(3).getRank() == Card.Rank.THREE &&
            hand.get(4).getRank() == Card.Rank.TWO) {
            return true;
        }
        
        // Regular case: check for consecutive ranks
        for (int i = 0; i < hand.size() - 1; i++) {
            if (hand.get(i).getRank().getValue() != hand.get(i + 1).getRank().getValue() + 1) {
                return false;
            }
        }
        
        return true;
    }
    
    public static class HandResult {
        private final HandRank rank;
        private final List<Card> highCards;
        
        public HandResult(HandRank rank, List<Card> highCards) {
            this.rank = rank;
            this.highCards = new ArrayList<>(highCards);
        }
        
        public HandRank getRank() {
            return rank;
        }
        
        public List<Card> getHighCards() {
            return Collections.unmodifiableList(highCards);
        }
        
        @Override
        public String toString() {
            return "HandResult{rank=" + rank + ", highCards=" + highCards + "}";
        }
    }
}