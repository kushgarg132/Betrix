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

  const { callAmount, canCheck, minRaise, maxRaise, isCallAllIn } = useMemo(() => {
    if (!game || !currentPlayer) {
      return { callAmount: 0, canCheck: true, minRaise: 1, maxRaise: 1000, isCallAllIn: false };
    }
    const currentPlayerBet = game.currentBettingRound?.playerBets?.[currentPlayer.username] || 0;
    const ca = (game.currentBet || 0) - currentPlayerBet;
    const playerChips = game.players?.[currentPlayer.index]?.chips || 0;
    return {
      callAmount: ca,
      canCheck: ca === 0,
      minRaise: Math.max(ca + 1, 1),
      maxRaise: playerChips,
      isCallAllIn: ca >= playerChips,
    };
  }, [game, currentPlayer]);

  useEffect(() => { setRaiseAmount(minRaise); }, [minRaise]);

  const fmt = useCallback((n) => n.toLocaleString(), []);

  if (!isMyTurn || !game) return null;

  return (
    <div className="betting-controls-wrapper">
      <button className="btn-fold" onClick={fold}>Fold</button>

      {canCheck ? (
        <button className="btn-check" onClick={check}>Check</button>
      ) : (
        <button className="btn-call" onClick={() => placeBet(callAmount)}>
          Call
          <span className="call-amount">${fmt(callAmount)}</span>
        </button>
      )}

      {!isCallAllIn && (
        <div className="raise-section">
          <input
            type="range"
            className="raise-slider"
            min={minRaise}
            max={maxRaise}
            value={raiseAmount}
            onChange={(e) => setRaiseAmount(parseInt(e.target.value))}
          />
          <div className="raise-amount">${fmt(raiseAmount)}</div>
          <button className="btn-raise" onClick={() => placeBet(raiseAmount)}>
            Raise
          </button>
        </div>
      )}
    </div>
  );
});

BettingControls.displayName = 'BettingControls';
export default BettingControls;