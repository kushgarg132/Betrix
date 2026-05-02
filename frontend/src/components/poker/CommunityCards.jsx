import React from 'react';
import PokerCard from './PokerCard';

export default function CommunityCards({ cards = [] }) {
  const slots = Array.from({ length: 5 }, (_, i) => cards[i] || null);

  return (
    <div className="flex items-center justify-center gap-2">
      {slots.map((card, i) => (
        card ? (
          <PokerCard key={`${card.suit}-${card.rank}-${i}`} card={card} delay={i * 0.1} />
        ) : (
          <div
            key={i}
            className="w-14 h-20 rounded-[var(--radius-sm)] border border-dashed border-white/15 bg-white/5"
          />
        )
      ))}
    </div>
  );
}
