import { describe, expect, it } from 'vitest';
import { cardKey, HAND_RANK_LABEL, showdownHighlight, winnerIds } from '../showdown';

const flush = [
  { rank: 'ACE', suit: 'HEARTS' }, { rank: 'TEN', suit: 'HEARTS' }, { rank: 'EIGHT', suit: 'HEARTS' },
  { rank: 'FIVE', suit: 'HEARTS' }, { rank: 'TWO', suit: 'HEARTS' },
];

describe('showdown helpers', () => {
  it('highlights exactly the best-hand cards', () => {
    const set = showdownHighlight({ bestHand: { rank: 'FLUSH', highCards: flush }, winners: [] });
    expect([...set].sort()).toEqual(flush.map(cardKey).sort());
    expect(set.has('KING-SPADES')).toBe(false);
  });

  it('highlights nothing when the hand ended without a showdown', () => {
    expect(showdownHighlight({ bestHand: null, winners: [{ id: 'p1' }] })).toBeNull();
    expect(showdownHighlight(null)).toBeNull();
  });

  it('labels every hand rank', () => {
    for (const r of ['HIGH_CARD', 'ONE_PAIR', 'TWO_PAIR', 'THREE_OF_A_KIND', 'STRAIGHT', 'FLUSH',
      'FULL_HOUSE', 'FOUR_OF_A_KIND', 'STRAIGHT_FLUSH', 'ROYAL_FLUSH']) {
      expect(HAND_RANK_LABEL[r]).toMatch(/^[A-Z]/);
    }
  });

  it('collects winner ids', () => {
    expect([...winnerIds({ winners: [{ id: 'p1' }, { id: 'p3' }] })]).toEqual(['p1', 'p3']);
    expect(winnerIds(null).size).toBe(0);
  });
});
