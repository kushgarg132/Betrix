import React, { memo } from 'react';
import Card from './Card';
import { ChipStack } from './PokerChip';
import './Player.css';

// Use memo to prevent unnecessary re-renders
const Player = memo(({ 
  player, 
  displayPosition, 
  blindStatus, 
  playerBet, 
  isCurrentTurn, 
  isCurrentPlayer, 
  playerCount,
  currentHand
}) => {
  // Don't render anything if player is null or undefined
  if (!player) return null;
  
  // Special template for current player - more compact with optimized layout
  if (isCurrentPlayer) {
    return (
      <div
        className={`player ${blindStatus ? blindStatus : ''} 
                   ${playerCount}-players 
                   ${isCurrentTurn ? 'current-turn' : ''}
                   current-player`}
        data-position={displayPosition}
        style={{
          opacity: player.hasFolded ? 0.5 : 1
        }}
      >
        {/* Bet display at top right for current player */}
        {playerBet > 0 && (
          <div className="opponent-bet-display current-player-bet">${playerBet}</div>
        )}
        
        <div className="player-avatar">
          <div className="player-name">{player.username}</div>
        </div>
        
        <div className="player-info-container">
          <div className="chips">${player.chips}</div>
        </div>
        
        <div className="player-cards-container">
          <div className="player-cards">
            {currentHand?.map((card, idx) => (
              <Card key={`player-card-${idx}`} card={card} />
            ))}
          </div>
        </div>
        
        {blindStatus && (
          <div className={`blind-indicator ${blindStatus}`}>
            {blindStatus === 'big-blind' ? 'BB' : 'SB'}
          </div>
        )}
        
        {playerBet > 0 && (
          <div className="bet-chips">
            <ChipStack amount={playerBet} />
          </div>
        )}
      </div>
    );
  }

  // Regular template for other players
  return (
    <div
      className={`player ${blindStatus ? blindStatus : ''} 
                 ${playerCount}-players 
                 ${isCurrentTurn ? 'current-turn' : ''}
                 ${isCurrentPlayer ? 'current-player' : ''}`}
      data-position={displayPosition}
      style={{
        opacity: player.hasFolded ? 0.5 : 1
      }}
    >
      {/* Bet display at top right */}
      {playerBet > 0 && (
        <div className="opponent-bet-display">${playerBet}</div>
      )}
      
      <div className="player-avatar">
        <div className="player-name">{player.username}</div>
      </div>
      
      <div className="player-cards-container">
        {/* Show hidden cards for opponents unless they've folded */}
        {!player.hasFolded && (
          <div className="opponent-cards">
            <div className="card hidden"></div>
            <div className="card hidden"></div>
          </div>
        )}
      </div>
      
      <div className="player-info-container">
        <div className="chips">${player.chips}</div>
        {/* Only show text bet info on smaller screens */}
        <div className="player-bet-small-screen">
          {playerBet > 0 && `Bet: $${playerBet}`}
        </div>
        
        {blindStatus && (
          <div className={`blind-indicator ${blindStatus}`}>
            {blindStatus === 'big-blind' ? 'BB' : 'SB'}
          </div>
        )}
        
        {playerBet > 0 && (
          <div className="bet-chips">
            <ChipStack amount={playerBet} />
          </div>
        )}
      </div>
    </div>
  );
});

Player.displayName = 'Player'; // For debugging in React DevTools

export default Player; 