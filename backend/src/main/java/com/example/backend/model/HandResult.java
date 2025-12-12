package com.example.backend.model;

import com.example.backend.service.HandEvaluator;
import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandResult {
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

    private HandRank rank;
    private List<Card> highCards;

    public HandResult(HandResult bestResult) {
        this.highCards = new ArrayList<>(bestResult.highCards);
        this.rank = bestResult.rank;
    }

    public List<Card> getHighCards() {
        return Collections.unmodifiableList(highCards);
    }

    @Override
    public String toString() {
        return "HandResult{rank=" + rank + ", highCards=" + highCards + "}";
    }
}