import React, { memo } from 'react';
import './BettingControls.css';

const BettingControls = memo(({ 
  isMyTurn, 
  game, 
  currentPlayer, 
  toggleRaiseSlider, 
  placeBet, 
  check, 
  fold 
}) => {
  if (!isMyTurn || !game) return null;

  // Calculate call amount just once
  const currentPlayerBet = game.currentBettingRound?.playerBets[currentPlayer.username] || 0;
  const callAmount = game.currentBet - currentPlayerBet;
  const canCheck = game.currentBet === currentPlayerBet;
  
  return (
    <div className="betting-controls-wrapper">
      <div className="betting-controls-container">
        {canCheck && (
          <button className="check-button" onClick={check}>
            <span className="action-text">Check</span>
          </button>
        )}
        
        {!canCheck && (
          <button 
            className="call-button" 
            onClick={() => placeBet(callAmount)}
          >
            <span className="action-text">Call</span>
            <span className="action-amount">
              ${callAmount}
            </span>
          </button>
        )}

        <button 
          className="raise-button" 
          onClick={toggleRaiseSlider}
        >
          <span className="action-text">Raise</span>
          <span className="action-amount">Min ${Math.max(game.currentBet + 1, 1)}</span>
        </button>

        <button className="fold-button" onClick={fold}>
          <span className="action-text">Fold</span>
        </button>
      </div>
    </div>
  );
});

BettingControls.displayName = 'BettingControls';

export default BettingControls; 