import React from 'react';
import { motion } from 'framer-motion';
import { cn, SUIT_SYMBOLS, RANK_DISPLAY, isSuitRed } from '@/lib/utils';

export default function PokerCard({
  card,
  faceDown = false,
  delay = 0,
  className,
  small = false,
  highlighted = false,
  dimmed = false,
}) {
  const suit = card?.suit;
  const rank = card?.rank;
  const red = isSuitRed(suit);
  const symbol = SUIT_SYMBOLS[suit] || '';
  const rankStr = RANK_DISPLAY[rank] || rank || '';
  const pipSize = small ? 'text-xs' : 'text-sm';

  return (
    <motion.div
      initial={{ opacity: 0, rotateY: 90, scale: 0.8 }}
      animate={{ opacity: 1, rotateY: 0, scale: 1 }}
      transition={{ duration: 0.35, delay, type: 'spring', stiffness: 200, damping: 20 }}
      className={cn(
        'relative rounded-[var(--radius-sm)] border shadow-lg select-none overflow-hidden transition-transform',
        small ? 'w-8 h-11' : 'w-14 h-20',
        faceDown
          ? 'border-neon-cyan/40 bg-surface-overlay'
          : 'border-border-strong bg-text',
        highlighted && '-translate-y-2 glow-magenta ring-2 ring-neon-magenta',
        dimmed && 'opacity-40',
        className
      )}
      style={{
        perspective: '600px',
        ...(faceDown ? {
          backgroundImage:
            'repeating-linear-gradient(45deg, color-mix(in oklab, var(--color-neon-cyan) 12%, transparent) 0 4px, transparent 4px 8px)',
        } : null),
      }}
    >
      {!faceDown && (
        <>
          {/* Top-left pip */}
          <div className={cn(
            'absolute top-1 left-1.5 flex flex-col leading-none',
            pipSize,
            red ? 'text-danger' : 'text-background'
          )}>
            <span className="font-bold">{rankStr}</span>
            <span>{symbol}</span>
          </div>
          {/* Center suit — decorative duplicate, hidden from screen readers */}
          <div
            aria-hidden="true"
            className={cn(
              'absolute inset-0 flex items-center justify-center',
              small ? 'text-base' : 'text-xl',
              red ? 'text-danger' : 'text-background'
            )}
          >
            {symbol}
          </div>
          {/* Bottom-right pip (rotated) — decorative duplicate */}
          <div
            aria-hidden="true"
            className={cn(
              'absolute bottom-1 right-1.5 flex flex-col leading-none rotate-180',
              pipSize,
              red ? 'text-danger' : 'text-background'
            )}
          >
            <span className="font-bold">{rankStr}</span>
            <span>{symbol}</span>
          </div>
        </>
      )}
    </motion.div>
  );
}
