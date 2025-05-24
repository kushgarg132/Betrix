import React, { memo, useState, useEffect } from 'react';
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
    
    // Create a gradient that starts from top right (0deg) and moves counterclockwise
    // This will make the border disappear from top right to top left
    return {
      borderImage: `conic-gradient(
        from 0deg at 50% 50%,
        transparent 0deg ${360 - (borderProgress * 3.6)}deg,
        ${isCurrentTurn ? '#f1c40f' : 'rgba(52, 152, 219, 0.3)'} ${360 - (borderProgress * 3.6)}deg 360deg
      ) 1 stretch`,
      borderStyle: 'solid',
      borderWidth: '2px'
    };
  };
  
  // Don't render anything if player is null or undefined
  if (!player) return null;
  
  // Special template for current player - more compact with optimized layout
  if (isCurrentPlayer) {
    return (
      <div
        className={`player ${blindStatus ? blindStatus : ''} 
                   ${playerCount}-players 
                   ${isCurrentTurn ? 'current-turn' : ''}
                   ${isWinner ? 'winner' : ''}
                   current-player`}
        data-position={displayPosition}
        style={{
          opacity: player.hasFolded ? 0.5 : 1,
          ...getBorderStyle()
        }}
      >
        {/* Bet display at top right for current player */}
        {playerBet > 0 && (
          <div className="opponent-bet-display current-player-bet">${playerBet}</div>
        )}
        
        <div className="current-player-content">
          <div className="player-avatar">
            <div className="player-name">{player.username}</div>
          </div>
          
          <div className="player-info-container">
            <div className="chips">${player.chips}</div>
          </div>
        </div>
        
        <div className="player-cards-container">
          <div className="player-cards">
            {currentHand && currentHand.length > 0 ? (
              currentHand.map((card, idx) => {
                // Check if this card is part of the winning hand
                const isWinningCard = winningCardMap[`${card.rank}-${card.suit}`];
                return (
                  <Card 
                    key={`player-card-${idx}-${card.suit}-${card.rank}`} 
                    card={card} 
                    cardContext={isWinningCard ? "winning-player" : "player"} 
                  />
                );
              })
            ) : (
              <>
                <Card hidden={true} cardContext="player" />
                <Card hidden={true} cardContext="player" />
              </>
            )}
          </div>
        </div>
        
        {isWinner && player.bestHand && (
          <div className="winner-badge">
            {player.bestHand.rank.replace(/_/g, ' ')}
          </div>
        )}
        
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
                 ${isWinner ? 'winner' : ''}
                 ${isCurrentPlayer ? 'current-player' : ''}`}
      data-position={displayPosition}
      style={{
        opacity: player.hasFolded ? 0.5 : 1,
        ...getBorderStyle()
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
            {isWinner && player.hand && player.hand.length > 0 ? (
              player.hand.map((card, idx) => {
                // Check if this card is part of the winning hand
                const isWinningCard = winningCardMap[`${card.rank}-${card.suit}`];
                return (
                  <Card 
                    key={`opponent-card-${idx}-${card.suit}-${card.rank}`} 
                    card={card} 
                    hidden={false}
                    cardContext={isWinningCard ? "winning-opponent" : "opponent"} 
                  />
                );
              })
            ) : (
              <>
                <Card hidden={true} cardContext="opponent" />
                <Card hidden={true} cardContext="opponent" />
              </>
            )}
          </div>
        )}
      </div>
      
      {isWinner && player.bestHand && (
        <div className="winner-badge">
          {player.bestHand.rank.replace(/_/g, ' ')}
        </div>
      )}
      
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