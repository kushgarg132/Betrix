import React from 'react';
import PokerCard from './PokerCard';

export default function CommunityCards({ cards = [], highlight = null }) {
  const slots = Array.from({ length: 5 }, (_, i) => cards[i] || null);

  return (
    <div className="flex items-center justify-center gap-2">
      {slots.map((card, i) => {
        const key = card ? `${card.rank}-${card.suit}` : null;
        return card ? (
          <PokerCard
            key={`${card.suit}-${card.rank}-${i}`}
            card={card}
            delay={i * 0.1}
            highlighted={!!key && !!highlight?.has(key)}
            dimmed={!!highlight && highlight.size > 0 && !!key && !highlight.has(key)}
          />
        ) : (
          <div
            key={i}
            className="w-14 h-20 rounded-[var(--radius-sm)] border border-dashed border-border-strong bg-surface-elevated/30"
          />
        );
      })}
    </div>
  );
}
