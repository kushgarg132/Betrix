import { describe, expect, it } from 'vitest';
import { seatLayout } from '../seatLayout';

describe('seatLayout', () => {
  for (let n = 2; n <= 9; n++) {
    for (let hero = 0; hero < n; hero++) {
      it(`puts the hero bottom-center with ${n} seats (hero ${hero})`, () => {
        const seats = seatLayout(n, hero);
        expect(seats).toHaveLength(n);
        expect(seats[hero].left).toBeCloseTo(50, 5);
        const maxTop = Math.max(...seats.map((s) => s.top));
        expect(seats[hero].top).toBeCloseTo(maxTop, 5);
      });
    }
  }

  it('places seats clockwise from the hero and keeps them inside the box', () => {
    const seats = seatLayout(6, 2);
    for (const s of seats) {
      expect(s.left).toBeGreaterThanOrEqual(0);
      expect(s.left).toBeLessThanOrEqual(100);
      expect(s.top).toBeGreaterThanOrEqual(0);
      expect(s.top).toBeLessThanOrEqual(100);
    }
    // next seat clockwise from bottom-center is to the left on screen (dealing order)
    expect(seats[3].left).toBeLessThan(50);
  });

  it('with no hero seat, index 0 takes the bottom', () => {
    expect(seatLayout(4, -1)[0].left).toBeCloseTo(50, 5);
  });

  it('is empty for no seats', () => {
    expect(seatLayout(0, -1)).toEqual([]);
  });
});
