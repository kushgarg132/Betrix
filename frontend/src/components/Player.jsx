import React, { memo, useState, useEffect } from 'react';
import Card from './Card';
import { ChipStack } from './PokerChip';
import './Player.css';
import './PlayerCyberpunk.css'; // Neon Cyberpunk theme

// Use memo to prevent unnecessary re-renders
const Player = memo(({
  player,
  displayPosition,
  blindStatus,
  playerBet,
  isCurrentTurn,
  isCurrentPlayer,
  playerCount,
  currentHand,
  actionDeadline,
  game // Add game prop to access winners information
}) => {
  // State to track the remaining time percentage for the border animation
  const [borderProgress, setBorderProgress] = useState(100);

  // Get the winning cards from the best hand if available
  const isWinner = game?.winners?.some(winner => winner.username === player.username);

  // Get ALL winning cards (combine highCards and bestFiveCards)
  let winningCards = [];
  if (isWinner) {
    const winnerData = game.winners.find(w => w.username === player.username);
    if (winnerData?.bestHand) {
      // Get highCards (important cards)
      const highCards = winnerData.bestHand.highCards || [];
      // Combine them (we'll remove duplicates later)
      winningCards = [...highCards];
    }
  }

  // Create a map of winning cards for O(1) lookup
  const winningCardMap = {};
  if (winningCards.length > 0) {
    winningCards.forEach(card => {
      winningCardMap[`${card.rank}-${card.suit}`] = true;
    });
  }

  // Effect to calculate and update the border progress based on action deadline
  useEffect(() => {
    if (!player || !actionDeadline || !isCurrentTurn) {
      setBorderProgress(100);
      return;
    }

    // Store the initial values when the effect first runs
    const startTime = new Date().getTime();
    const deadline = new Date(actionDeadline).getTime();

    // Calculate the total duration from when this effect runs until the deadline
    const totalDuration = deadline - startTime;

    if (totalDuration <= 0) {
      setBorderProgress(0);
      return;
    }

    const updateBorderProgress = () => {
      const now = new Date().getTime();
      const remaining = deadline - now;

      if (remaining <= 0) {
        setBorderProgress(0);
        return;
      }

      // Calculate percentage of time remaining
      const remainingPercentage = Math.max(0, Math.min(100, (remaining / totalDuration) * 100));
      setBorderProgress(remainingPercentage);
    };

    // Initial update
    updateBorderProgress();

    // Set up interval to update the border progress
    const intervalId = setInterval(updateBorderProgress, 100);

    return () => clearInterval(intervalId);
  }, [actionDeadline, isCurrentTurn, player]);

  // Calculate the border style based on the remaining time
  const getBorderStyle = () => {
    if (!isCurrentTurn || !actionDeadline) return {};

    // When time is up, remove the border completely
    if (borderProgress <= 0) {
      return { border: 'none' };
    }

    // Create a gradient that starts from top right (0deg) and moves counterclockwise
    // This will make the border disappear from top right to top left as time passes
    return {
      borderImage: `conic-gradient(
      from 0deg at 50% 50%,
      transparent 0deg ${360 - (borderProgress * 3.6)}deg,
      ${isCurrentTurn ? '#f1c40f' : 'rgba(52, 152, 219, 0.3)'} ${360 - (borderProgress * 3.6)}deg 360deg
    ) 1 stretch`,
      borderStyle: 'solid',
      borderWidth: '2px',
    };
  };

  // Don't render anything if player is null or undefined
  if (!player) return null;

  // Refactored Player Component for Premium Look
  return (
    <div
      className={`player ${player.isSittingOut ? 'sitting-out' : ''} 
                 ${isCurrentTurn ? 'current-turn' : ''}
                 ${isWinner ? 'winner' : ''}
                 ${isCurrentPlayer ? 'is-user' : ''}`}
      data-position={displayPosition}
      style={{ opacity: player.hasFolded ? 0.4 : 1 }}
    >
      {/* Bet Display Above Player */}
      {playerBet > 0 && (
        <div className="opponent-bet-display">${playerBet}</div>
      )}

      <div className="player-avatar">
        {/* SVG Progress Ring */}
        {isCurrentTurn && actionDeadline && (
          <svg className="turn-timer-ring" viewBox="0 0 100 100">
            <circle
              cx="50" cy="50" r="46"
              fill="none"
              stroke="var(--primary)"
              strokeWidth="4"
              strokeDasharray="290"
              strokeDashoffset={290 - (borderProgress * 2.9)}
              strokeLinecap="round"
              style={{ transition: 'stroke-dashoffset 0.1s linear', filter: 'drop-shadow(0 0 5px var(--primary-glow))' }}
            />
          </svg>
        )}

        {/* Placeholder for actual avatar image or initials */}
        <div className="avatar-placeholder">
          {player.username.charAt(0).toUpperCase()}
        </div>

        {/* Turn Timer Text Overlay */}
        {isCurrentTurn && actionDeadline && (
          <div className="turn-timer">
            {Math.max(0, Math.ceil((new Date(actionDeadline).getTime() - Date.now()) / 1000))}s
          </div>
        )}

        {/* Blind Indicator */}
        {blindStatus && (
          <div className={`blind-indicator ${blindStatus}`}>
            {blindStatus === 'big-blind' ? 'B' : 'S'}
          </div>
        )}

        {/* Sitting Out Indicator */}
        {player.isSittingOut && (
          <div className="sitting-out-indicator">OFF</div>
        )}
      </div>

      {/* Cards Display */}
      {!player.hasFolded && (
        <div className="player-cards-container">
          <div className="player-cards">
            {isCurrentPlayer || isWinner ? (
              (isCurrentPlayer ? currentHand : player.hand).map((card, idx) => (
                <Card
                  key={`${player.username}-card-${idx}`}
                  card={card}
                  cardContext={winningCardMap[`${card.rank}-${card.suit}`] ? "winning-player" : "player"}
                />
              ))
            ) : (
              <>
                <Card hidden={true} cardContext="opponent" />
                <Card hidden={true} cardContext="opponent" />
              </>
            )}
          </div>
        </div>
      )}

      {/* Player Label Info */}
      <div className="player-info-container">
        <div className="player-name">{player.username}</div>
        <div className="chips">${player.chips.toLocaleString()}</div>
      </div>
    </div>
  );
});

Player.displayName = 'Player';
export default Player; 