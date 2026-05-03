import React, { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Trophy, X } from 'lucide-react';
import { formatChips } from '@/lib/utils';
import PokerCard from './PokerCard';

const AUTO_DISMISS_MS = 5000;

export default function WinnerOverlay({ game, visible }) {
  const [dismissed, setDismissed] = useState(false);

  // Reset dismissed state whenever a new hand ends
  useEffect(() => {
    if (visible) {
      setDismissed(false);
      const id = setTimeout(() => setDismissed(true), AUTO_DISMISS_MS);
      return () => clearTimeout(id);
    }
  }, [visible, game?.winners]);

  const show = visible && !dismissed;
  if (!show || !game) return null;

  const winners = game.winners || [];
  const hasWinners = winners.length > 0;

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          // Covers only the table oval, not the full page
          className="absolute inset-0 z-20 flex items-center justify-center rounded-full overflow-hidden"
          style={{ background: 'rgba(0,0,0,0.72)', backdropFilter: 'blur(3px)' }}
          onClick={() => setDismissed(true)}
        >
          <motion.div
            initial={{ scale: 0.7, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.7, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 200, damping: 20 }}
            className="bg-surface border border-gold/50 rounded-2xl p-6 text-center max-w-[260px] w-full mx-4 winner-glow relative"
            onClick={e => e.stopPropagation()}
          >
            {/* Dismiss button */}
            <button
              onClick={() => setDismissed(true)}
              className="absolute top-2 right-2 text-text-dim hover:text-text transition-colors"
              aria-label="Dismiss"
            >
              <X size={14} />
            </button>

            <motion.div
              animate={{ rotate: [0, -10, 10, -10, 10, 0] }}
              transition={{ duration: 0.6, delay: 0.3 }}
              className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-gold/20 border-2 border-gold mb-3"
            >
              <Trophy className="text-gold" size={22} />
            </motion.div>

            <h2 className="font-serif text-xl font-bold text-text mb-1">
              {hasWinners ? `${winners[0]?.username || 'Winner'} wins!` : 'Hand Complete'}
            </h2>

            {hasWinners && winners[0]?.lastWinAmount > 0 && (
              <motion.p
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="text-gold font-bold text-lg mb-3"
              >
                {formatChips(winners[0].lastWinAmount)}
              </motion.p>
            )}

            {/* Best hand */}
            {winners[0]?.bestHand && (
              <div className="mt-2">
                <p className="text-text-muted text-[10px] uppercase tracking-wider mb-1.5">
                  {winners[0].bestHand.rank?.replace(/_/g, ' ')}
                </p>
                <div className="flex justify-center gap-1">
                  {(winners[0].bestHand.highCards || []).slice(0, 5).map((card, i) => (
                    <PokerCard key={i} card={card} delay={i * 0.07} small />
                  ))}
                </div>
              </div>
            )}

            <p className="text-text-dim text-[10px] mt-3">
              Click anywhere to continue
            </p>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
