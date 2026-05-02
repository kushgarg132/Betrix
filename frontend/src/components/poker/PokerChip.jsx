import React from 'react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';

const CHIP_COLORS = [
  { min: 0,    max: 4,     bg: '#f5f5f0', border: '#ccc',   text: '#333',   label: 'W' },
  { min: 5,    max: 24,    bg: '#dc2626', border: '#b91c1c', text: '#fff',   label: 'R' },
  { min: 25,   max: 99,    bg: '#2563eb', border: '#1d4ed8', text: '#fff',   label: 'B' },
  { min: 100,  max: 499,   bg: '#16a34a', border: '#15803d', text: '#fff',   label: 'G' },
  { min: 500,  max: 999,   bg: '#7c3aed', border: '#6d28d9', text: '#fff',   label: 'P' },
  { min: 1000, max: Infinity, bg: '#1c1c1c', border: '#d4a843', text: '#d4a843', label: 'K' },
];

function getChipColor(amount) {
  return CHIP_COLORS.find(c => amount >= c.min && amount <= c.max) || CHIP_COLORS[0];
}

export default function PokerChip({ amount, size = 'md', animate = false, className }) {
  const chip = getChipColor(amount);
  const sizes = { sm: 'w-8 h-8 text-[9px]', md: 'w-12 h-12 text-xs', lg: 'w-16 h-16 text-sm' };

  const chipEl = (
    <div
      className={cn(
        'relative rounded-full flex items-center justify-center font-bold select-none',
        'chip-shine',
        sizes[size],
        className
      )}
      style={{
        background: chip.bg,
        border: `3px solid ${chip.border}`,
        boxShadow: `0 2px 8px rgba(0,0,0,0.5), inset 0 1px 2px rgba(255,255,255,0.2)`,
        color: chip.text,
      }}
      title={`$${amount}`}
    >
      {/* Inner ring pattern */}
      <div
        className="absolute inset-1 rounded-full border opacity-30"
        style={{ borderColor: chip.text, borderStyle: 'dashed', borderWidth: '1px' }}
      />
      <span className="relative z-10 font-bold">${amount >= 1000 ? `${Math.floor(amount/1000)}K` : amount}</span>
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
