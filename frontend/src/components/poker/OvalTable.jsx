import React from 'react';
import { cn } from '@/lib/utils';
import PlayerSeat from './PlayerSeat';
import CommunityCards from './CommunityCards';
import PotDisplay from './PotDisplay';
import WinnerOverlay from './WinnerOverlay';

/* Seat positions around an ellipse.
   The container is 100% × 100%.
   a = 43% (semi-major), b = 41% (semi-minor), center = (50%, 50%).
   Angles start from bottom (270°) and go clockwise. */
const SEAT_POSITIONS = [
  { left: '50%',   top: '93%'  }, // 0 — bottom (hero)
  { left: '80%',   top: '81%'  }, // 1 — bottom-right
  { left: '94%',   top: '50%'  }, // 2 — right
  { left: '80%',   top: '19%'  }, // 3 — top-right
  { left: '50%',   top: '7%'   }, // 4 — top
  { left: '20%',   top: '19%'  }, // 5 — top-left
  { left: '6%',    top: '50%'  }, // 6 — left
  { left: '20%',   top: '81%'  }, // 7 — bottom-left
];

export default function OvalTable({ game, currentPlayer, isMyTurn, getDisplayPosition, getBlindStatus }) {
  const players = game?.players || [];
  const communityCards = game?.communityCards || [];
  const isGameEnded = game?.status === 'ENDED';

  return (
    <div className="relative w-full" style={{ paddingTop: '55%' /* 11:6 aspect ratio */ }}>
      {/* Full container */}
      <div className="absolute inset-0">
        {/* ── Table rim (outermost) ── */}
        <div
          className="absolute inset-[6%] rounded-full"
          style={{
            background: 'linear-gradient(180deg, #8b6428 0%, #6b4c1e 40%, #4a3010 100%)',
            boxShadow: '0 8px 48px rgba(0,0,0,0.8), 0 0 0 3px #3d2800, inset 0 2px 4px rgba(255,255,255,0.1)',
          }}
        />

        {/* ── Felt surface ── */}
        <div
          className="absolute felt-surface rounded-full"
          style={{
            inset: 'calc(6% + 10px)',
            boxShadow: 'inset 0 0 80px rgba(0,0,0,0.4)',
          }}
        />

        {/* ── Felt inner pattern (subtle) ── */}
        <div
          className="absolute rounded-full pointer-events-none"
          style={{
            inset: 'calc(6% + 24px)',
            border: '2px solid rgba(255,255,255,0.05)',
          }}
        />

        {/* ── Center content: pot + community cards ── */}
        <div className="absolute inset-[20%] flex flex-col items-center justify-center gap-3 pointer-events-none">
          <PotDisplay pot={game?.pot ?? 0} pots={game?.pots} />
          <CommunityCards cards={communityCards} />
          {game?.currentBettingRound?.roundType && (
            <span className="text-white/30 text-[10px] uppercase tracking-widest">
              {game.currentBettingRound.roundType}
            </span>
          )}
        </div>

        {/* ── Player seats ── */}
        {players.map((player, actualIndex) => {
          if (!player) return null;
          const displayPos = getDisplayPosition(actualIndex);
          const pos = SEAT_POSITIONS[displayPos] || SEAT_POSITIONS[0];
          const blindStatus = getBlindStatus(player);
          const isDealer = game?.dealerPosition === actualIndex;

          return (
            <div
              key={player.id || actualIndex}
              className="absolute"
              style={{
                left: pos.left,
                top: pos.top,
                transform: 'translate(-50%, -50%)',
              }}
            >
              <PlayerSeat
                player={player}
                isCurrentTurn={game?.currentPlayerIndex === actualIndex}
                isCurrentPlayer={player.username === currentPlayer?.username}
                isDealer={isDealer}
                blindStatus={blindStatus}
                currentHand={currentPlayer?.hand}
                playerBet={game?.currentBettingRound?.bets?.[player.id] ?? 0}
                displayPosition={displayPos}
              />
            </div>
          );
        })}

        {/* Empty seats for unfilled spots */}
        {Array.from({ length: (game?.maxPlayers ?? 8) - players.length }, (_, i) => {
          const emptyPos = SEAT_POSITIONS[players.length + i];
          if (!emptyPos) return null;
          return (
            <div
              key={`empty-${i}`}
              className="absolute"
              style={{ left: emptyPos.left, top: emptyPos.top, transform: 'translate(-50%, -50%)' }}
            >
              <PlayerSeat player={null} />
            </div>
          );
        })}

        {/* Winner overlay */}
        <WinnerOverlay game={game} visible={isGameEnded} />
      </div>
    </div>
  );
}
