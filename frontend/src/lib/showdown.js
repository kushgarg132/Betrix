export const cardKey = (c) => `${c.rank}-${c.suit}`;

export const HAND_RANK_LABEL = {
  HIGH_CARD: 'High Card', ONE_PAIR: 'Pair', TWO_PAIR: 'Two Pair', THREE_OF_A_KIND: 'Three of a Kind',
  STRAIGHT: 'Straight', FLUSH: 'Flush', FULL_HOUSE: 'Full House', FOUR_OF_A_KIND: 'Four of a Kind',
  STRAIGHT_FLUSH: 'Straight Flush', ROYAL_FLUSH: 'Royal Flush',
};

/** The cards to lift. Null when everyone else folded: the backend sends no bestHand then. */
export function showdownHighlight(showdown) {
  const cards = showdown?.bestHand?.highCards;
  return cards?.length ? new Set(cards.map(cardKey)) : null;
}

export function winnerIds(showdown) {
  return new Set((showdown?.winners || []).map((w) => w.id));
}
