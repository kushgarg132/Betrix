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
        
        <table className="info-table">
          <tbody>
            <tr>
              <td className="info-label">Pot:</td>
              <td className="info-value">${game.pot || 0}</td>
              <td className="info-label">Big Blind:</td>
              <td className="info-value">{game.bigBlindAmount || 'N/A'}</td>
            </tr>
            <tr>
              <td className="info-label">Current Bet:</td>
              <td className="info-value">${game.currentBet || 0}</td>
              <td className="info-label">Small Blind:</td>
              <td className="info-value">{game.smallBlindAmount || 'N/A'}</td>
            </tr>
            <tr>
              <td className="info-label">Phase:</td>
              <td className="info-value">{game.phase || 'Pre-Flop'}</td>
              <td className="info-label">Dealer:</td>
              <td className="info-value">{game.dealerPosition || 'N/A'}</td>
            </tr>
            <tr>
              <td className="info-label">Current Player:</td>
              <td className="info-value">{game.players && game.currentPlayerIndex !== undefined ? game.players[game.currentPlayerIndex]?.username || 'N/A' : 'N/A'}</td>
              <td className="info-label">Players:</td>
              <td className="info-value">{game.players ? game.players.length : 0}</td>
            </tr>
          </tbody>
        </table>
        
        {/* Community Cards */}
        {game.communityCards && game.communityCards.length > 0 && (
          <table className="info-table community-table">
            <tbody>
              <tr>
                <td className="info-label" style={{ width: '120px' }}>Community Cards:</td>
                <td className="info-value cards-container" colSpan="3">
                  {game.communityCards.map((card, index) => (
                    <span key={index} className="card">{card}</span>
                  ))}
                </td>
              </tr>
            </tbody>
          </table>
        )}
        
        {/* Action Buttons */}
        <div className="button-row">
          <button className="game-button rankings-btn" onClick={toggleRankingsModal}>
            <span className="button-icon">♠</span>
            <span>Rankings</span>
          </button>
          
          <button className="game-button stats-btn" onClick={() => window.alert('Game history will be available soon!')}>
            <span className="button-icon">📊</span>
            <span>Stats</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default GameInfoPanel; 