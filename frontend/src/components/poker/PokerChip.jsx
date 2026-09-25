import React from 'react';
import { motion } from 'framer-motion';
import { cn, formatChips } from '@/lib/utils';

// Denomination -> chip token. Literal class names (not built from a template string) so
// Tailwind's scanner can see them.
const CHIP_TIERS = [
  { min: 0,   className: 'bg-chip-1 border-chip-1/50' },
  { min: 25,  className: 'bg-chip-2 border-chip-2/50' },
  { min: 100, className: 'bg-chip-3 border-chip-3/50' },
  { min: 500, className: 'bg-chip-4 border-chip-4/50' },
];

function chipClassName(amount) {
  let cls = CHIP_TIERS[0].className;
  for (const tier of CHIP_TIERS) if (amount >= tier.min) cls = tier.className;
  return cls;
}

const SIZES = { sm: 'w-8 h-8 text-xs', md: 'w-12 h-12 text-xs', lg: 'w-16 h-16 text-sm' };

export default function PokerChip({ amount, size = 'md', animate = false, className }) {
  const label = formatChips(amount);

  const chipEl = (
    <div
      className={cn(
        'relative rounded-full flex items-center justify-center font-bold select-none border-2 text-background shadow-lg',
        chipClassName(amount),
        SIZES[size],
        className
      )}
      title={label}
    >
      <div className="absolute inset-1 rounded-full border border-dashed border-background/40" />
      <span className="relative z-10 font-mono tabular">{label}</span>
    </div>
  );

  if (animate) {
    return (
      <motion.div
        initial={{ y: -20, opacity: 0, scale: 1.2 }}
        animate={{ y: 0, opacity: 1, scale: 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
      >
        {chipEl}
      </motion.div>
    );
  }

  return chipEl;
}
