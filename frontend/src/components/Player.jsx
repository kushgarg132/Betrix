import React, { memo, useMemo } from 'react';
import './Player.css';
import Card from './Card';

const Player = memo(({
  player,
  displayPosition,
  blindStatus,
  playerBet,
  isCurrentTurn,
  isCurrentPlayer,
  currentHand,
  actionDeadline,
  game
}) => {
  if (!player) return null;

  // Calculate visual progress for turn ring
  const borderProgress = useMemo(() => {
    if (!isCurrentTurn || !actionDeadline) return 0;
    try {
      const now = Date.now();
      const deadline = new Date(actionDeadline).getTime();
      const totalTime = 12 * 1000; // 12s standard
      return Math.max(0, Math.min(100, ((deadline - now) / totalTime) * 100));
    } catch (e) {
      return 0;
    }
  }, [isCurrentTurn, actionDeadline]);

  const isWinner = game?.winners?.some(w => w.username === player.username) || false;

  return (
    <div
      className={`player ${player.isSittingOut ? 'sitting-out' : ''} 
                 ${isCurrentTurn ? 'current-turn' : ''}
                 ${isWinner ? 'winner' : ''}
                 ${isCurrentPlayer ? 'is-user' : ''}`}
      data-position={displayPosition}
    >
      {/* Bet Display Above Player (Mockup Chip Stacks) */}
      {playerBet > 0 && (
        <div className="bet-display">
          <div className="chip-stack">
            {[...Array(Math.min(5, Math.ceil(playerBet / 100)))].map((_, i) => (
              <div key={`chip-${i}`} className="chip-icon" />
            ))}
          </div>
          <div className="bet-amount">${playerBet?.toLocaleString()}</div>
        </div>
      )}

      {/* Integrated Avatar & Label */}
      <div className="player-container">
        <div className="player-avatar">
          {isCurrentTurn && actionDeadline && (
            <svg className="turn-timer-ring" viewBox="0 0 100 100">
              <circle
                cx="50" cy="50" r="48"
                fill="none"
                stroke="var(--gold)"
                strokeWidth="4"
                strokeDasharray="301"
                strokeDashoffset={301 - (borderProgress * 3.01)}
                strokeLinecap="round"
                style={{ transition: 'stroke-dashoffset 0.1s linear' }}
              />
            </svg>
          )}

          <div className="avatar-placeholder" style={{
            backgroundImage: `url(https://i.pravatar.cc/150?u=${player.username})`
          }} />

          {isCurrentTurn && actionDeadline && (
            <div className="turn-timer">
              {Math.max(0, Math.ceil((new Date(actionDeadline).getTime() - Date.now()) / 1000))}s
            </div>
          )}
        </div>

        <div className="player-info-container">
          <div className="player-name">{player.username}</div>
          <div className="role-text">{blindStatus === 'small-blind' ? 'Dealer' : 'Player'}</div>
          <div className="chips">${player.chips?.toLocaleString() || 0}</div>
        </div>

        {/* Blind/Special Status Badges */}
        {blindStatus && (
          <div className={`blind-indicator ${blindStatus}`}>
            <div className="blind-badge">D</div>
          </div>
        )}
      </div>

      {/* Perspective Hand Display for Opponents (Mockup Style) */}
      {!player.hasFolded && !isCurrentPlayer && (
        <div className="player-cards-container">
          <div className="player-cards">
            <Card hidden={true} cardContext="opponent" />
            <Card hidden={true} cardContext="opponent" />
          </div>
        </div>
      )}

      {/* Cards for User or Winner */}
      {!player.hasFolded && (isCurrentPlayer || isWinner) && (
        <div className="player-cards-container current-user-hand">
          <div className="player-cards">
            {(isCurrentPlayer ? currentHand : player.hand)?.map((card, idx) => (
              <Card
                key={`${player.username}-card-${idx}`}
                card={card}
                cardContext="player"
              />
            ))}
          </div>
        </div>
      )}
    </div>
  );
});

Player.displayName = 'Player';
export default Player;