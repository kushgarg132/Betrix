import { describe, expect, it } from 'vitest';
import { turnProgress } from '../turnProgress';

const now = Date.parse('2026-09-25T10:00:00Z');

describe('turnProgress', () => {
  it('is full at the start of a turn', () => {
    const p = turnProgress('2026-09-25T10:00:30Z', 30, now);
    expect(p.remainingMs).toBe(30000);
    expect(p.fraction).toBe(1);
    expect(p.urgent).toBe(false);
  });

  it('is urgent at five seconds left', () => {
    const p = turnProgress('2026-09-25T10:00:05Z', 30, now);
    expect(p.fraction).toBeCloseTo(5 / 30);
    expect(p.urgent).toBe(true);
  });

  it('clamps past the deadline and beyond the timeout', () => {
    expect(turnProgress('2026-09-25T09:59:50Z', 30, now)).toEqual({ remainingMs: 0, fraction: 0, urgent: true });
    expect(turnProgress('2026-09-25T10:01:00Z', 30, now).fraction).toBe(1);
  });

  it('has no progress without a deadline', () => {
    expect(turnProgress(null, 30, now)).toEqual({ remainingMs: null, fraction: 1, urgent: false });
  });
});
