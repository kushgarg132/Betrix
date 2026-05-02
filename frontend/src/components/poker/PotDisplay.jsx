import React from 'react';
import { motion } from 'framer-motion';
import { formatChips } from '@/lib/utils';

export default function PotDisplay({ pot, pots }) {
  const hasSidePots = pots && pots.length > 1;

  return (
    <div className="flex flex-col items-center gap-1">
      <motion.div
        key={pot}
        initial={{ scale: 1.15, opacity: 0.7 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.25 }}
        className="px-4 py-1.5 rounded-full bg-black/40 border border-white/10 text-center"
      >
        <span className="text-xs text-white/60 uppercase tracking-widest mr-1.5">Pot</span>
        <span className="font-bold text-gold text-sm">{formatChips(pot)}</span>
      </motion.div>

      {hasSidePots && pots.map((p, i) => (
        <div key={i} className="text-[10px] text-white/40">
          Side pot {i + 1}: {formatChips(p.amount)}
        </div>
      ))}
    </div>
  );
}
