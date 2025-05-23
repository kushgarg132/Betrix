import React, { memo, useState, useEffect, useCallback, useMemo } from 'react';
import './BettingControls.css';

const BettingControls = memo(({ 
  isMyTurn, 
  game, 
  currentPlayer, 
  placeBet, 
  check, 
  fold 
}) => {
  const [showRaiseSlider, setShowRaiseSlider] = useState(false);
  const [raiseAmount, setRaiseAmount] = useState(1);
  const [raiseAmountColor, setRaiseAmountColor] = useState('#ffb300');

  // Calculate values using useMemo to prevent recalculations on every render
  const { callAmount, canCheck, minRaise, maxRaise, isCallAllIn, isForceAllIn } = useMemo(() => {
    if (!game || !currentPlayer) {
      return { 
        currentPlayerBet: 0, 
        callAmount: 0, 
        canCheck: true, 
        minRaise: 1, 
        maxRaise: 1000, 
        isCallAllIn: false,
        isForceAllIn: false 
      };
    }
    
    const currentPlayerBet = game.currentBettingRound?.playerBets[currentPlayer.username] || 0;
    const callAmount = game.currentBet - currentPlayerBet;
    const playerChips = game.players[currentPlayer.index].chips;
    const canCheck = callAmount === 0;
    const isCallAllIn = callAmount >= playerChips;
    const isForceAllIn = game.currentBet > playerChips; // Force all-in when current bet exceeds chips
    const minRaise = Math.max(callAmount + 1, 1);
    const maxRaise = playerChips;
    
    return { callAmount, canCheck, minRaise, maxRaise, isCallAllIn, isForceAllIn };
  }, [game, currentPlayer]);

  // Set initial raise amount when min/max changes or when opening the slider
  useEffect(() => {
    if (showRaiseSlider) {
      setRaiseAmount(minRaise);
    }
  }, [showRaiseSlider, minRaise]);

  // Memoize handlers to prevent recreation on every render
  const handleRaiseChange = useCallback((value) => {
    const newValue = parseInt(value);
    setRaiseAmount(newValue);
    
    // Calculate color based on slider position
    if (minRaise && maxRaise) {
      const percentage = (newValue - minRaise) / (maxRaise - minRaise);
      if (percentage <= 0.33) {
        // Red to amber gradient for lower third
        const r = Math.round(229 + (255 - 229) * (percentage * 3));
        const g = Math.round(57 + (179 - 57) * (percentage * 3));
        const b = Math.round(53 + (0 - 53) * (percentage * 3));
        setRaiseAmountColor(`rgb(${r}, ${g}, ${b})`);
      } else if (percentage <= 0.66) {
        // Amber to green gradient for upper two thirds
        const adjustedPercentage = (percentage - 0.33) * 3;
        const r = Math.round(255 - (255 - 67) * adjustedPercentage);
        const g = Math.round(179 + (160 - 179) * adjustedPercentage);
        const b = Math.round(0 + (71 - 0) * adjustedPercentage);
        setRaiseAmountColor(`rgb(${r}, ${g}, ${b})`);
      } else {
        // Full green for upper third
        setRaiseAmountColor('#43a047');
      }
    }
  }, [minRaise, maxRaise]);

  const handleOpenRaiseSlider = useCallback(() => {
    setShowRaiseSlider(true);
  }, []);

  const handleCloseRaiseSlider = useCallback(() => {
    setShowRaiseSlider(false);
  }, []);

  const cancelRaise = useCallback(() => {
    setShowRaiseSlider(false);
  }, []);

  const confirmRaise = useCallback(() => {
    placeBet(raiseAmount);
    setShowRaiseSlider(false);
  }, [placeBet, raiseAmount]);

  const formatNumber = useCallback((num) => {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  }, []);

  if (!isMyTurn || !game) return null;

  return (
    <>
      <div className="betting-controls-wrapper">
        <div className="betting-controls-container">
          {/* If forced all-in, show only all-in and fold options */}
          {isForceAllIn ? (
            <>
              <button 
                className="all-in-button call-all-in" 
                onClick={() => placeBet(maxRaise)}
              >
                <span className="action-text">All-In</span>
                <span className="action-amount">
                  ${formatNumber(maxRaise)}
                </span>
              </button>
              
              <button className="fold-button" onClick={fold}>
                <span className="action-text">Fold</span>
              </button>
            </>
          ) : (
            <>
              {/* Normal betting controls when not forced all-in */}
              {canCheck && (
                <button className="check-button" onClick={check}>
                  <span className="action-text">Check</span>
                </button>
              )}
              
              {!canCheck && !isCallAllIn && (
                <button 
                  className="call-button" 
                  onClick={() => placeBet(callAmount)}
                >
                  <span className="action-text">Call</span>
                  <span className="action-amount">
                    ${formatNumber(callAmount)}
                  </span>
                </button>
              )}
              
              {!canCheck && isCallAllIn && (
                <button 
                  className="all-in-button call-all-in" 
                  onClick={() => placeBet(maxRaise)}
                >
                  <span className="action-text">All-In</span>
                  <span className="action-amount">
                    ${formatNumber(maxRaise)}
                  </span>
                </button>
              )}

              {!isCallAllIn && (
                <button 
                  className="raise-button" 
                  onClick={handleOpenRaiseSlider}
                >
                  <span className="action-text">Raise</span>
                </button>
              )}

              <button className="fold-button" onClick={fold}>
                <span className="action-text">Fold</span>
              </button>
            </>
          )}
        </div>
      </div>

      {showRaiseSlider && !isForceAllIn && (
        <div className="raise-slider-overlay" onClick={handleCloseRaiseSlider}>
          <div className="raise-controls" onClick={(e) => e.stopPropagation()}>
            <div className="raise-header">
              <span>Set Your Raise</span>
              <button className="close-button" onClick={handleCloseRaiseSlider}>×</button>
            </div>
            <div className="raise-amount" style={{ color: raiseAmountColor, textShadow: `0 0 15px ${raiseAmountColor}80`, borderColor: `${raiseAmountColor}80` }}>${formatNumber(raiseAmount)}</div>
            <div className="raise-slider-container">
              <style>
                {`
                  .raise-slider::-webkit-slider-thumb {
                    background: linear-gradient(135deg, ${raiseAmountColor}, ${raiseAmountColor}dd);
                    box-shadow: 0 5px 10px rgba(0, 0, 0, 0.3), 0 0 15px ${raiseAmountColor}80;
                  }
                `}
              </style>
              <input
                type="range"
                className="raise-slider"
                min={minRaise}
                max={maxRaise}
                value={raiseAmount}
                onChange={(e) => handleRaiseChange(e.target.value)}
              />
              <div className="slider-track"></div>
            </div>
            <div className="raise-info">
              <span>Min: ${formatNumber(minRaise)}</span>
              <span>Max: ${formatNumber(maxRaise)}</span>
            </div>
            <div className="raise-buttons">
              <button 
                className="confirm-raise-button" 
                onClick={confirmRaise}
              >
                <span className="action-text">Raise</span>
              </button>
              <button 
                className="all-in-button" 
                onClick={() => {
                  placeBet(maxRaise);
                  setShowRaiseSlider(false);
                }}
              >
                All In
              </button>
              <button 
                className="cancel-raise-button" 
                onClick={cancelRaise}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
});

BettingControls.displayName = 'BettingControls';

export default BettingControls; 