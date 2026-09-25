import { describe, expect, it } from 'vitest';
import { formatBlinds, formatChips } from '../utils';

describe('formatChips', () => {
  it('shows play-money chips without a currency sign', () => {
    expect(formatChips(950)).toBe('950');
    expect(formatChips(12500)).toBe('12.5K');
    expect(formatChips(10000)).toBe('10K');
    expect(formatChips(2000000)).toBe('2M');
    expect(formatChips(0)).toBe('0');
    expect(formatChips(null)).toBe('—');
  });

  it('formats blinds without a currency sign', () => {
    expect(formatBlinds(10, 20)).toBe('10/20');
  });
});
