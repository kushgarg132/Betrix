import React from 'react';
import './RaiseSlider.css';

const RaiseSlider = ({ 
  showRaiseSlider, 
  raiseAmount, 
  handleRaiseChange, 
  game, 
  currentPlayer, 
  toggleRaiseSlider, 
  placeBet, 
  cancelRaise 
}) => {
  if (!showRaiseSlider) return null;

  // Calculate min and max values
  const minRaise = Math.max(game.currentBet + 1, 1);
  const maxRaise = Math.min(currentPlayer.chips, 1000);

  // Helper function to format numbers with commas
  const formatNumber = (num) => {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  };

  return (
    <div className="raise-slider-overlay" onClick={() => toggleRaiseSlider()}>
      <div className="raise-controls" onClick={(e) => e.stopPropagation()}>
        <div className="raise-header">
          <span>Set Your Raise</span>
          <button className="close-button" onClick={toggleRaiseSlider}>×</button>
        </div>
        <div className="raise-amount">${formatNumber(raiseAmount)}</div>
        <div className="raise-slider-container">
          <input
            type="range"
            className="raise-slider"
            min={minRaise}
            max={maxRaise}
            value={raiseAmount}
            onChange={(e) => handleRaiseChange(parseInt(e.target.value))}
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
            onClick={() => {
              placeBet(raiseAmount);
              toggleRaiseSlider();
            }}
          >
            <span className="action-text">Raise</span>
            <span className="action-amount">${formatNumber(raiseAmount)}</span>
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
  );
};

export default RaiseSlider; 