import React from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn, formatBlinds, STATUS_LABELS, STATUS_BADGE_VARIANT } from '@/lib/utils';

export default function GameCard({ game, onJoin }) {
  const { id, status, playerCount, maxPlayers, smallBlindAmount, bigBlindAmount, isYourGame } = game;
  const isFull = playerCount >= maxPlayers;
  const label = isYourGame ? 'Rejoin' : isFull ? 'Full' : 'Join';
  const variant = isYourGame ? 'default' : isFull ? 'surface' : 'outline';

  return (
    <div
      className={cn(
        'flex items-center gap-3 p-3 rounded-xl bg-surface border border-border',
        isYourGame && 'border-neon-cyan/40'
      )}
    >
      <div className="min-w-0 flex-1">
        <div className="font-display text-sm text-text truncate">Table #{id?.slice(-4)}</div>
        <div className="flex items-center gap-2 mt-1">
          <span className="font-mono tabular-nums text-sm text-text-muted">
            {formatBlinds(smallBlindAmount, bigBlindAmount)}
          </span>
          <Badge variant={STATUS_BADGE_VARIANT[status] || 'surface'}>
            {STATUS_LABELS[status] ?? status}
          </Badge>
        </div>
      </div>

      <div
        className="flex items-center gap-1 shrink-0"
        aria-label={`${playerCount} of ${maxPlayers} seats taken`}
      >
        {Array.from({ length: maxPlayers }).map((_, i) => (
          <span
            key={i}
            data-testid="seat-dot"
            data-filled={i < playerCount}
            className={cn('h-2 w-2 rounded-full', i < playerCount ? 'bg-neon-cyan' : 'bg-border-strong')}
          />
        ))}
      </div>

      {/* Touch target stays >=44px tall (global constraint) even though it's a compact row —
          only the horizontal padding is trimmed to fit the row on a 375px screen. */}
      <Button
        variant={variant}
        disabled={isFull && !isYourGame}
        onClick={onJoin}
        className="shrink-0 px-3"
      >
        {label}
      </Button>
    </div>
  );
}
