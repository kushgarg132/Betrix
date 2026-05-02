import React from 'react';
import { motion } from 'framer-motion';
import { cn, SUIT_SYMBOLS, RANK_DISPLAY, isSuitRed } from '@/lib/utils';

export default function PokerCard({ card, faceDown = false, delay = 0, className, small = false }) {
  const suit = card?.suit;
  const rank = card?.rank;
  const red = isSuitRed(suit);
  const symbol = SUIT_SYMBOLS[suit] || '';
  const rankStr = RANK_DISPLAY[rank] || rank || '';

  return (
    <motion.div
      initial={{ opacity: 0, rotateY: 90, scale: 0.8 }}
      animate={{ opacity: 1, rotateY: 0, scale: 1 }}
      transition={{ duration: 0.35, delay, type: 'spring', stiffness: 200, damping: 20 }}
      className={cn(
        'relative rounded-[var(--radius-sm)] border shadow-lg select-none overflow-hidden',
        small ? 'w-8 h-11' : 'w-14 h-20',
        faceDown
          ? 'border-blue-800 bg-gradient-to-br from-blue-900 to-blue-950'
          : 'border-gray-200 bg-white',
        className
      )}
      style={{ perspective: '600px' }}
    >
      {faceDown ? (
        <div className="absolute inset-1 rounded border border-blue-700/50 bg-blue-900/40 flex items-center justify-center">
          <div className="w-3 h-4 rounded-xs border border-blue-600/60 bg-blue-800/30" />
        </div>
      ) : (
        <>
          {/* Top-left pip */}
          <div className={cn(
            'absolute top-1 left-1.5 flex flex-col leading-none',
            small ? 'text-[9px]' : 'text-[11px]',
            red ? 'text-red-600' : 'text-gray-900'
          )}>
            <span className="font-bold">{rankStr}</span>
            <span>{symbol}</span>
          </div>
          {/* Center suit */}
          <div className={cn(
            'absolute inset-0 flex items-center justify-center',
            small ? 'text-base' : 'text-xl',
            red ? 'text-red-600' : 'text-gray-900'
          )}>
            {symbol}
          </div>
          {/* Bottom-right pip (rotated) */}
          <div className={cn(
            'absolute bottom-1 right-1.5 flex flex-col leading-none rotate-180',
            small ? 'text-[9px]' : 'text-[11px]',
            red ? 'text-red-600' : 'text-gray-900'
          )}>
            <span className="font-bold">{rankStr}</span>
            <span>{symbol}</span>
          </div>
        </>
      )}
    </motion.div>
  );
}
