import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs) {
  return twMerge(clsx(inputs));
}

export function formatChips(amount) {
  if (!amount && amount !== 0) return '—';
  if (amount >= 1_000_000) return `$${(amount / 1_000_000).toFixed(1)}M`;
  if (amount >= 1_000)     return `$${(amount / 1_000).toFixed(1)}K`;
  return `$${amount.toLocaleString()}`;
}

export function formatBlinds(small, big) {
  return `$${small}/$${big}`;
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
