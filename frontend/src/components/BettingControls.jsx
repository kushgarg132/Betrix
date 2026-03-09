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
    setRaiseAmount(minRaise);
  }, [minRaise]);

  const handleRaiseChange = useCallback((value) => {
    setRaiseAmount(parseInt(value));
  }, []);

  const confirmRaise = useCallback(() => {
    placeBet(raiseAmount);
  }, [placeBet, raiseAmount]);

  const formatNumber = useCallback((num) => {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  }, []);

  if (!isMyTurn || !game) return null;

  return (
    <div className="betting-controls-wrapper">
      <div className="betting-controls-container">
        {/* Fold Button */}
        <button className="fold-button" onClick={fold}>
          <span className="action-text">Fold</span>
        </button>

        {/* Check/Call Button */}
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
      </div>

      {/* Bet/Raise Slider Section (Mockup Style) */}
      {!isCallAllIn && !isForceAllIn && (
        <div className="bet-raise-section">
          <div className="bet-raise-container">
            <div className="bet-raise-label">Bet / Raise</div>
            <div className="raise-slider-track">
              <input
                type="range"
                className="raise-slider"
                min={minRaise}
                max={maxRaise}
                value={raiseAmount}
                onChange={(e) => handleRaiseChange(e.target.value)}
                onMouseUp={confirmRaise}
              />
            </div>
            <div className="slider-limits">
              <span>${formatNumber(minRaise)}</span>
              <span>${formatNumber(raiseAmount)}</span>
              <span>${formatNumber(maxRaise)}</span>
            </div>
          </div>
        </div>
      )}

      {/* Final Bet Amount Display */}
      <div className="bet-amount-container">
        <div className="bet-amount-label">Bet Amount</div>
        <div className="bet-amount-box">
          ${formatNumber(raiseAmount)}
        </div>
      </div>
    </div>
  );
});

BettingControls.displayName = 'BettingControls';
export default BettingControls;