import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn, formatChips, getPlayerInitials } from '@/lib/utils';
import PokerCard from './PokerCard';
import PokerChip from './PokerChip';

const BLIND_LABELS = {
  'small-blind': 'SB',
  'big-blind':   'BB',
};

/* Circuit pattern SVG for bot avatars */
function CircuitPattern() {
  return (
    <svg
      className="absolute inset-0 w-full h-full opacity-25 pointer-events-none"
      viewBox="0 0 56 56"
      fill="none"
    >
      <line x1="14" y1="28" x2="28" y2="28" stroke="#d4a843" strokeWidth="1"/>
      <line x1="28" y1="28" x2="42" y2="28" stroke="#d4a843" strokeWidth="1"/>
      <line x1="28" y1="14" x2="28" y2="42" stroke="#d4a843" strokeWidth="1"/>
      <circle cx="28" cy="28" r="2" fill="#d4a843"/>
      <circle cx="14" cy="28" r="1.5" fill="#d4a843"/>
      <circle cx="42" cy="28" r="1.5" fill="#d4a843"/>
      <circle cx="28" cy="14" r="1.5" fill="#d4a843"/>
      <circle cx="28" cy="42" r="1.5" fill="#d4a843"/>
      <line x1="14" y1="18" x2="14" y2="28" stroke="#d4a843" strokeWidth="1"/>
      <line x1="42" y1="18" x2="42" y2="28" stroke="#d4a843" strokeWidth="1"/>
    </svg>
  );
}

/* [BOT] badge */
function BotBadge({ difficulty }) {
  const color = difficulty === 'HARD'
    ? 'bg-red-500/20 text-red-400 border-red-500/30'
    : difficulty === 'EASY'
      ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
      : 'bg-amber-500/20 text-amber-400 border-amber-500/30';
  return (
    <span className={cn('text-[8px] font-bold px-1 py-0.5 rounded border leading-none', color)}>
      BOT
    </span>
  );
}

export default function PlayerSeat({
  player,
  isCurrentTurn,
  isCurrentPlayer,
  isDealer,
  blindStatus,
  currentHand,
  playerBet,
  displayPosition,
}) {
  if (!player) {
    return (
      <div className="flex flex-col items-center gap-1.5 opacity-30">
        <div className="w-14 h-14 rounded-full border-2 border-dashed border-white/20 flex items-center justify-center">
          <span className="text-white/30 text-lg">+</span>
        </div>
        <span className="text-white/20 text-xs">Empty</span>
      </div>
    );
  }

  const showOwnCards = isCurrentPlayer && currentHand && currentHand.length > 0;
  const hasFolded  = player.hasFolded;
  const isAllIn    = player.isAllIn;
  const isSittingOut = player.isSittingOut;
  const isBot      = player.isBot;

  const initials = getPlayerInitials(player.username || player.name);

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: hasFolded ? 0.4 : 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      className={cn('flex flex-col items-center gap-1.5 relative', hasFolded && 'opacity-40')}
    >
      {/* Avatar with turn ring */}
      <div className={cn(
        'relative rounded-full transition-all duration-300',
        isCurrentTurn && !hasFolded ? 'seat-pulse' : ''
      )}>
        <div className={cn(
          'w-14 h-14 rounded-full flex items-center justify-center border-2 transition-all duration-300 overflow-hidden relative',
          isCurrentPlayer
            ? 'border-gold bg-gold/20 shadow-[0_0_12px_rgba(212,168,67,0.5)]'
            : isCurrentTurn
              ? 'border-gold/70 bg-surface-elevated'
              : isBot
                ? 'border-amber-600/60 bg-amber-900/20'
                : 'border-border bg-surface-elevated',
          hasFolded && 'grayscale'
        )}>
          {isBot && <CircuitPattern />}
          <span className={cn(
            'font-bold text-sm relative z-10',
            isCurrentPlayer ? 'text-gold' : isBot ? 'text-amber-400' : 'text-text'
          )}>
            {initials}
          </span>
        </div>

        {/* Dealer button */}
        {isDealer && (
          <div className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-white border border-gray-300 flex items-center justify-center">
            <span className="text-[8px] font-bold text-gray-800">D</span>
          </div>
        )}

        {/* Blind badge */}
        {blindStatus && (
          <div className={cn(
            'absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center text-[8px] font-bold',
            blindStatus === 'small-blind'
              ? 'bg-blue-600 text-white'
              : 'bg-red-600 text-white'
          )}>
            {BLIND_LABELS[blindStatus]}
          </div>
        )}

        {/* Status overlays */}
        {isSittingOut && !hasFolded && (
          <div className="absolute inset-0 rounded-full bg-black/60 flex items-center justify-center">
            <span className="text-[9px] font-bold text-white/70">OUT</span>
          </div>
        )}
        {isAllIn && !hasFolded && (
          <div className="absolute inset-0 rounded-full bg-gold/20 border border-gold rounded-full flex items-center justify-center">
            <span className="text-[9px] font-bold text-gold">ALL IN</span>
          </div>
        )}
      </div>

      {/* Player info */}
      <div className={cn(
        'px-2.5 py-1 rounded-full text-center min-w-[80px]',
        'bg-black/60 border',
        isCurrentPlayer ? 'border-gold/40' : 'border-white/10',
      )}>
        <div className="flex items-center justify-center gap-1">
          <span
            title={player.username || player.name}
            className={cn(
              'text-xs font-semibold truncate max-w-[90px]',
              isCurrentPlayer ? 'text-gold' : isBot ? 'text-amber-400' : 'text-white'
            )}
          >
            {player.username || player.name}
          </span>
          {isBot && <BotBadge difficulty={player.botDifficulty} />}
        </div>
        <div className="text-[10px] text-white/60 font-medium">
          {formatChips(player.chips)}
        </div>
      </div>

      {/* Current bet */}
      <AnimatePresence>
        {playerBet > 0 && (
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.8 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            className="absolute -bottom-10"
          >
            <PokerChip amount={playerBet} size="sm" animate />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Hole cards */}
      <div className="flex gap-1 -mt-1">
        {showOwnCards ? (
          currentHand.map((card, i) => (
            <PokerCard key={i} card={card} delay={i * 0.1} small />
          ))
        ) : player.hand && player.hand.length > 0 ? (
          [0, 1].map(i => (
            <PokerCard key={i} faceDown delay={i * 0.1} small />
          ))
        ) : null}
      </div>
    </motion.div>
  );
}
