import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Trophy } from 'lucide-react';
import { formatChips } from '@/lib/utils';
import PokerCard from './PokerCard';

export default function WinnerOverlay({ game, visible }) {
  if (!visible || !game) return null;

  const winners = game.winners || [];
  const hasWinners = winners.length > 0;

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="absolute inset-0 z-30 flex items-center justify-center"
          style={{ background: 'rgba(0,0,0,0.75)', backdropFilter: 'blur(4px)' }}
        >
          <motion.div
            initial={{ scale: 0.7, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.7, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 200, damping: 20 }}
            className="bg-surface border border-gold/50 rounded-2xl p-8 text-center max-w-sm w-full mx-4 winner-glow"
          >
            <motion.div
              animate={{ rotate: [0, -10, 10, -10, 10, 0] }}
              transition={{ duration: 0.6, delay: 0.3 }}
              className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gold/20 border-2 border-gold mb-4"
            >
              <Trophy className="text-gold" size={28} />
            </motion.div>

            <h2 className="font-serif text-2xl font-bold text-text mb-1">
              {hasWinners ? `${winners[0]?.username || 'Winner'} wins!` : 'Hand Complete'}
            </h2>

            {hasWinners && winners[0]?.lastWinAmount && (
              <motion.p
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="text-gold font-bold text-xl mb-4"
              >
                {formatChips(winners[0].lastWinAmount)}
              </motion.p>
            )}

            {/* Best hand display */}
            {winners[0]?.bestHand && (
              <div className="mt-3">
                <p className="text-text-muted text-xs uppercase tracking-wider mb-2">
                  {winners[0].bestHand.rank?.replace(/_/g, ' ')}
                </p>
                <div className="flex justify-center gap-1.5">
                  {(winners[0].bestHand.highCards || []).map((card, i) => (
                    <PokerCard key={i} card={card} delay={i * 0.07} small />
                  ))}
                </div>
              </div>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
