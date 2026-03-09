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

  // Generate initials from username
  const initials = useMemo(() => {
    const name = player.username || '';
    const parts = name.split(/[\s._-]+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }, [player.username]);

  const isWinner = game?.winners?.some(w => w.username === player.username) || false;
  const isDealer = blindStatus === 'small-blind';

  // Avatar class: gold if active turn or dealer, else gray
  const avatarClass = (isCurrentTurn || isDealer) ? 'avatar-active' : 'avatar-default';

  // Cards to show
  const cardsToShow = isCurrentPlayer ? currentHand : (isWinner ? player.hand : null);
  const showHiddenCards = !player.hasFolded && !isCurrentPlayer && !isWinner;

  return (
    <div
      className={`player ${player.isSittingOut ? 'sitting-out' : ''}
                 ${isCurrentTurn ? 'current-turn' : ''}
                 ${isWinner ? 'winner' : ''}
                 ${isCurrentPlayer ? 'is-user' : ''}`}
      data-position={displayPosition}
    >
      {/* Bet chip above player */}
      {playerBet > 0 && (
        <div className="player-bet">${playerBet.toLocaleString()}</div>
      )}

      {/* Dark Panel */}
      <div className="player-panel">
        <div className={`player-avatar ${avatarClass}`}>
          {initials}
        </div>
        <div className="player-info">
          <div className="player-name">{player.username}</div>
          <div className="player-chips">${player.chips?.toLocaleString() || 0}</div>
        </div>
      </div>

      {/* Cards Below Panel */}
      <div className="player-cards-row">
        {/* Show actual cards for current player or winner */}
        {cardsToShow?.map((card, idx) => (
          <Card
            key={`${player.username}-card-${idx}`}
            card={card}
            cardContext="player"
          />
        ))}

        {/* Show hidden cards for opponents */}
        {showHiddenCards && (
          <>
            <Card hidden={true} cardContext="opponent" />
            <Card hidden={true} cardContext="opponent" />
          </>
        )}
      </div>
    </div>
  );
});

Player.displayName = 'Player';
export default Player;