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

  const { callAmount, canCheck, minRaise, maxRaise, isCallAllIn, isForceAllIn } = useMemo(() => {
    if (!game || !currentPlayer) {
      return { callAmount: 0, canCheck: true, minRaise: 1, maxRaise: 1000, isCallAllIn: false, isForceAllIn: false };
    }

    const currentPlayerBet = game.currentBettingRound?.playerBets[currentPlayer.username] || 0;
    const callAmount = game.currentBet - currentPlayerBet;
    const playerChips = game.players[currentPlayer.index].chips;
    const canCheck = callAmount === 0;
    const isCallAllIn = callAmount >= playerChips;
    const isForceAllIn = game.currentBet > playerChips;
    const minRaise = Math.max(callAmount + 1, 1);
    const maxRaise = playerChips;

    return { callAmount, canCheck, minRaise, maxRaise, isCallAllIn, isForceAllIn };
  }, [game, currentPlayer]);

  useEffect(() => {
    if (showRaiseSlider) setRaiseAmount(minRaise);
  }, [showRaiseSlider, minRaise]);

  const handleRaiseChange = useCallback((value) => {
    setRaiseAmount(parseInt(value));
  }, []);

  const handleOpenRaiseSlider = useCallback(() => setShowRaiseSlider(true), []);
  const handleCloseRaiseSlider = useCallback(() => setShowRaiseSlider(false), []);
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
          {isForceAllIn ? (
            <button className="call-button" onClick={() => placeBet(maxRaise)}>
              <span className="action-text">All-In</span>
              <span className="action-amount">${formatNumber(maxRaise)}</span>
            </button>
          ) : (
            <>
              {canCheck ? (
                <button className="check-button" onClick={check}>
                  <span className="action-text">Check</span>
                </button>
              ) : (
                <button className="call-button" onClick={() => placeBet(callAmount)}>
                  <span className="action-text">Call</span>
                  <span className="action-amount">${formatNumber(callAmount)}</span>
                </button>
              )}

              {!isCallAllIn && (
                <button className="raise-button" onClick={handleOpenRaiseSlider}>
                  <span className="action-text">Raise</span>
                </button>
              )}
            </>
          )}

          <button className="fold-button" onClick={fold}>
            <span className="action-text">Fold</span>
          </button>
        </div>
      </div>

      {showRaiseSlider && !isForceAllIn && (
        <div className="raise-slider-overlay" onClick={handleCloseRaiseSlider}>
          <div className="raise-controls" onClick={(e) => e.stopPropagation()}>
            <div className="raise-header">
              <span>Set Your Raise</span>
              <button className="close-button" onClick={handleCloseRaiseSlider}>×</button>
            </div>

            <div className="raise-amount">${formatNumber(raiseAmount)}</div>

            <input
              type="range"
              className="raise-slider"
              min={minRaise}
              max={maxRaise}
              value={raiseAmount}
              onChange={(e) => handleRaiseChange(e.target.value)}
            />

            <div className="raise-info">
              <span>Min: ${formatNumber(minRaise)}</span>
              <span>Max: ${formatNumber(maxRaise)}</span>
            </div>

            <div className="raise-buttons">
              <button className="confirm-raise-button" onClick={confirmRaise}>
                Confirm Raise
              </button>
              <button className="cancel-raise-button" onClick={handleCloseRaiseSlider}>
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