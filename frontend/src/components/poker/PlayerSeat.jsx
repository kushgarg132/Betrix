import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn, formatChips, getPlayerInitials } from '@/lib/utils';
import { Badge } from '@/components/ui/badge';
import PokerCard from './PokerCard';
import PokerChip from './PokerChip';
import TurnRing from './TurnRing';

const BLIND_LABELS = {
  'small-blind': 'SB',
  'big-blind':   'BB',
};

const BOT_BADGE_VARIANT = { HARD: 'danger', EASY: 'success', MEDIUM: 'warning' };

export default function PlayerSeat({ player, isHero, isTurn, isDealer, isWinner, blind, hand, revealedHand, bet, highlight, deadline, timeoutSeconds, seatAngle = Math.PI / 2 }) {
  if (!player) return null;

  const hasFolded = player.hasFolded;
  const isAllIn = player.isAllIn;
  const isSittingOut = player.isSittingOut;
  const isBot = player.isBot;
  const initials = getPlayerInitials(player.name);
  // Hero's own cards while playing; an opponent's cards only once the showdown reveals them.
  const visibleHand = isHero ? hand : (revealedHand !== undefined ? revealedHand : null);
  // Mid-hand, still in it, nothing revealed yet: show the back of two cards at the seat.
  const showFaceDownPlaceholder = !isHero && revealedHand === undefined && !hasFolded;
  // Pull the bet chip toward the table center rather than a flat south offset — a bottom-half
  // seat (hero included) has the pot *above* it, so the chip anchors off the top edge instead of
  // the bottom there; every seat also gets a small sideways nudge from its own angle on the
  // ellipse (seatLayout.js) so a left/right seat's chip leans center-ward too.
  const betPullUp = Math.sin(seatAngle) > 0;
  const betX = `calc(-50% + ${-Math.cos(seatAngle) * 22}px)`;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      className={cn('relative flex flex-col items-center gap-1.5', hasFolded && 'opacity-40 grayscale')}
    >
      {/* Avatar — relative, so TurnRing can overlay it absolutely */}
      <div className="relative">
        {isTurn && <TurnRing deadline={deadline} timeoutSeconds={timeoutSeconds} isHero={isHero} />}
        <div className={cn(
          'w-12 h-12 rounded-full flex items-center justify-center border-2 bg-surface-elevated overflow-hidden',
          isWinner ? 'border-neon-magenta glow-magenta'
            : isHero ? 'border-neon-cyan glow-cyan' : 'border-border-strong'
        )}>
          <span className={cn('font-bold text-sm', isHero ? 'text-neon-cyan' : 'text-text')}>
            {initials}
          </span>
        </div>

        {/* Dealer button */}
        {isDealer && (
          <div className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-text text-background border border-border-strong flex items-center justify-center">
            <span className="text-xs font-bold leading-none">D</span>
          </div>
        )}

        {/* Blind marker */}
        {blind && (
          <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold bg-neon-cyan/20 text-neon-cyan border border-neon-cyan/40">
            {BLIND_LABELS[blind]}
          </div>
        )}
      </div>

      {/* Name + chips */}
      <div className="px-2.5 py-1 rounded-full bg-surface-overlay/80 border border-border text-center min-w-[80px] max-w-[110px]">
        <div className="flex items-center justify-center gap-1">
          <span title={player.name} className={cn('text-xs font-semibold truncate', isHero ? 'text-neon-cyan' : 'text-text')}>
            {player.name}
          </span>
          {isBot && (
            <Badge variant={BOT_BADGE_VARIANT[player.botDifficulty] || 'warning'} className="px-1 py-0">
              BOT
            </Badge>
          )}
        </div>
        <motion.div
          key={player.chips}
          initial={{ scale: 1.25 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.35 }}
          className="font-mono tabular text-xs text-text-muted"
        >
          {formatChips(player.chips)}
        </motion.div>
      </div>

      {/* Status tags */}
      {isAllIn && !hasFolded && (
        <Badge className="border-neon-magenta/40 bg-neon-magenta/15 text-neon-magenta">ALL IN</Badge>
      )}
      {isSittingOut && !hasFolded && (
        <Badge variant="surface">AWAY</Badge>
      )}

      {/* Current bet — sits toward the table center, direction from the seat's angle */}
      <AnimatePresence>
        {bet > 0 && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            className={cn('absolute left-1/2', betPullUp ? '-top-10' : '-bottom-9')}
            style={{ x: betX }}
          >
            <PokerChip amount={bet} size="sm" animate />
          </motion.div>
        )}
      </AnimatePresence>

      {/* Hero's hole cards, or an opponent's once the showdown reveals them */}
      {visibleHand && visibleHand.length > 0 && (
        <div className="flex gap-1 -mt-1">
          {visibleHand.map((card, i) => {
            const key = card ? `${card.rank}-${card.suit}` : null;
            return (
              <PokerCard
                key={i}
                card={card}
                small
                highlighted={!!key && !!highlight?.has(key)}
                dimmed={!!highlight && highlight.size > 0 && !!key && !highlight.has(key)}
              />
            );
          })}
        </div>
      )}

      {/* Opponent still in the hand, nothing revealed yet: face-down placeholder */}
      {showFaceDownPlaceholder && (
        <div className="flex gap-1 -mt-1">
          <PokerCard small faceDown />
          <PokerCard small faceDown />
        </div>
      )}
    </motion.div>
  );
}
