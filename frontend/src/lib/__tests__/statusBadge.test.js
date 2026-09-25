import { describe, expect, it } from 'vitest';
import { STATUS_LABELS, STATUS_BADGE_VARIANT } from '../utils';

describe('STATUS_BADGE_VARIANT', () => {
  it('covers every status STATUS_LABELS knows about, so no real game status silently falls back to the default badge', () => {
    for (const status of Object.keys(STATUS_LABELS)) {
      expect(STATUS_BADGE_VARIANT, `missing entry for ${status}`).toHaveProperty(status);
    }
  });

  it('recognises every real betting-round status as active, not the generic fallback', () => {
    for (const status of ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING', 'SHOWDOWN']) {
      expect(STATUS_BADGE_VARIANT[status]).toBe('active');
    }
  });
});
