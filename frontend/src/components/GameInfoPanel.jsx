import React from 'react';
import './GameInfoPanel.css';

const GameInfoPanel = ({ 
  sidebarVisible, 
  toggleSidebar, 
  game, 
  toggleRankingsModal 
}) => {
  // Handle toggle click with explicit state management
  const handleToggleClick = (e) => {
    e.stopPropagation(); // Prevent event bubbling
    toggleSidebar();
  };

  return (
    <div 
      className={`game-info-sidebar ${sidebarVisible ? 'expanded' : 'collapsed'}`}
      data-testid="game-info-sidebar"
    >
      {/* Arrow-shaped toggle button */}
      <button 
        className={`sidebar-toggle ${sidebarVisible ? 'expanded' : 'collapsed'}`}
        onClick={handleToggleClick}
        aria-label={sidebarVisible ? "Hide game info" : "Show game info"}
      >
        <div className="toggle-icon">
          {/* Icon is hidden, arrow shape is created with CSS */}
        </div>
      </button>
      
      <div className="info-content">
        <h3>Game Information</h3>
        <div className="info-item">
          <span className="info-label">Pot:</span>
          <span className="info-value">${game.pot}</span>
        </div>
        <div className="info-item">
          <span className="info-label">Current Player:</span>
          <span className="info-value">{game.players[game.currentPlayerIndex]?.username || 'N/A'}</span>
        </div>
        {game.bigBlindPosition && (
          <div className="info-item">
            <span className="info-label">Big Blind:</span>
            <span className="info-value">{game.bigBlindPosition}</span>
          </div>
        )}
        {game.smallBlindPosition && (
          <div className="info-item">
            <span className="info-label">Small Blind:</span>
            <span className="info-value">{game.smallBlindPosition}</span>
          </div>
        )}
        <button className="rankings-button" onClick={toggleRankingsModal}>
          <span className="button-icon">♠</span>
          <span>Hand Rankings</span>
        </button>
      </div>
    </div>
  );
};

export default GameInfoPanel; 