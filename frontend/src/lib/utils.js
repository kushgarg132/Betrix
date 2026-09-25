import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

function compact(n, div, suffix) {
  return `${(n / div).toFixed(1).replace(/\.0$/, '')}${suffix}`;
}

// Play money: plain chip counts, never a currency sign.
export function formatChips(amount) {
  if (amount == null) return '—';
  if (amount >= 1_000_000) return compact(amount, 1_000_000, 'M');
  if (amount >= 1_000)     return compact(amount, 1_000, 'K');
  return amount.toLocaleString();
}

export function formatBlinds(small, big) {
  return `${small}/${big}`;
}

export const SUIT_SYMBOLS = {
  SPADES:   '♠',
  HEARTS:   '♥',
  DIAMONDS: '♦',
  CLUBS:    '♣',
};

export const RANK_DISPLAY = {
  ACE: 'A', KING: 'K', QUEEN: 'Q', JACK: 'J', TEN: '10',
  NINE: '9', EIGHT: '8', SEVEN: '7', SIX: '6', FIVE: '5',
  FOUR: '4', THREE: '3', TWO: '2',
};

export function isSuitRed(suit) {
  return suit === 'HEARTS' || suit === 'DIAMONDS';
}

export function getPlayerInitials(name) {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
}

export const STATUS_LABELS = {
  WAITING:          'Waiting for players',
  STARTING:         'Starting…',
  PRE_FLOP_BETTING: 'Pre-Flop',
  FLOP_BETTING:     'Flop',
  TURN_BETTING:     'Turn',
  RIVER_BETTING:    'River',
  SHOWDOWN:         'Showdown',
  FINISHED:         'Finished',
  ENDED:            'Hand Complete',
};

// One source of truth for status -> badge variant, used by GameCard and AdminPanel. They used to
// each keep their own copy; AdminPanel's had drifted and used the wrong (pre-_BETTING) status
// names, so real in-progress games fell back to the plain 'surface' badge.
export const STATUS_BADGE_VARIANT = {
  WAITING:          'waiting',
  STARTING:         'waiting',
  PRE_FLOP_BETTING: 'active',
  FLOP_BETTING:     'active',
  TURN_BETTING:     'active',
  RIVER_BETTING:    'active',
  SHOWDOWN:         'active',
  FINISHED:         'completed',
  ENDED:            'completed',
};

export const ACTION_LABELS = {
  NONE:       '',
  SMALL_BLIND:'Small Blind',
  BIG_BLIND:  'Big Blind',
  FOLD:       'Fold',
  CALL:       'Call',
  RAISE:      'Raise',
  CHECK:      'Check',
  ALL_IN:     'All In',
  AUTO_FOLD:  'Auto Fold',
};
