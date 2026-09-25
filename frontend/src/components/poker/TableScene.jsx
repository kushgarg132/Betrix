import React from 'react';
import PlayerSeat from './PlayerSeat';
import CommunityCards from './CommunityCards';
import PotDisplay from './PotDisplay';
import { seatLayout } from '@/lib/seatLayout';

/**
 * The felt + seats. `heroIndex`/`hand` come from useGame (Task 10); `highlight` (a Set of
 * "RANK-SUIT" keys, or null) lifts the hero's winning cards at showdown (wired in Task 14).
 */
export default function TableScene({ game, heroIndex, hand, highlight = null, orientation = 'portrait' }) {
  const players = game?.players || [];
  const seats = seatLayout(players.length, heroIndex, orientation);

  return (
    <div className="relative w-full aspect-[3/4] lg:aspect-[16/10]">
      {/* Table floor */}
      <div
        className="absolute inset-[8%] rounded-[50%]"
        style={{
          background:
            'radial-gradient(ellipse at center, color-mix(in oklab, var(--color-neon-cyan) 8%, var(--color-table-floor)) 0%, var(--color-table-floor) 70%)',
          border: '2px solid var(--color-table-rail)',
          boxShadow: 'var(--glow-md), inset 0 0 60px rgb(0 0 0 / 0.6)',
        }}
      />

      {/* Pot + community cards */}
      <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 pointer-events-none px-4">
        <PotDisplay pot={game?.pot ?? 0} pots={game?.pots} />
        <CommunityCards cards={game?.communityCards || []} />
      </div>

      {/* Seats — only seated players, positioned hero-relative */}
      {players.map((player, actualIndex) => {
        if (!player) return null;
        const pos = seats[actualIndex];
        if (!pos) return null;
        const isHero = actualIndex === heroIndex;
        const blind =
          game?.bigBlindUserId === player.username ? 'big-blind'
          : game?.smallBlindUserId === player.username ? 'small-blind'
          : null;

        return (
          <div
            key={player.id || actualIndex}
            className="absolute"
            style={{ left: `${pos.left}%`, top: `${pos.top}%`, transform: 'translate(-50%, -50%)' }}
          >
            <PlayerSeat
              player={player}
              isHero={isHero}
              isTurn={game?.currentPlayerIndex === actualIndex}
              isDealer={game?.dealerPosition === actualIndex}
              blind={blind}
              hand={isHero ? hand : null}
              bet={game?.currentBettingRound?.bets?.[player.id] ?? 0}
              highlight={highlight}
              deadline={game?.currentPlayerActionDeadline}
              timeoutSeconds={game?.playerActionTimeoutSeconds}
            />
          </div>
        );
      })}
    </div>
  );
}
