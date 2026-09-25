import React, { useEffect } from 'react';
import { motion } from 'framer-motion';
import { formatChips } from '@/lib/utils';
import { HAND_RANK_LABEL } from '@/lib/showdown';

/**
 * Banner over the table center (rendered inside TableScene's box, not a modal). Auto-dismisses
 * after 3s or on tap; `showdown` is null-guarded by the caller so this only mounts while live.
 */
export default function ShowdownReveal({ showdown, onDone }) {
  useEffect(() => {
    const id = setTimeout(onDone, 3000);
    return () => clearTimeout(id);
  }, [showdown, onDone]);

  const winners = showdown?.winners || [];
  const bestHand = showdown?.bestHand;
  const total = winners.reduce((sum, w) => sum + (w.lastWinAmount || 0), 0);
  const winnerNames = winners.map((w) => w.name).join(' & ') || 'No one';
  const sentence = bestHand ? `${winnerNames} wins ${formatChips(total)}` : 'Everyone else folded';

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.85 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
      className="absolute inset-0 z-30 flex items-center justify-center pointer-events-none"
    >
      <button
        type="button"
        onClick={onDone}
        aria-label="Dismiss result"
        className="pointer-events-auto flex flex-col items-center gap-1.5 px-6 py-4 rounded-2xl bg-surface-overlay/90 border border-neon-magenta/40 glow-magenta text-center"
      >
        <span className="text-sm font-semibold text-text">{sentence}</span>
        {bestHand && (
          <span
            className="font-display text-2xl text-neon-magenta"
            style={{ textShadow: '0 0 14px var(--color-neon-magenta)' }}
          >
            {HAND_RANK_LABEL[bestHand.rank]}
          </span>
        )}
      </button>
      <div aria-live="polite" className="sr-only">{sentence}</div>
    </motion.div>
  );
}
