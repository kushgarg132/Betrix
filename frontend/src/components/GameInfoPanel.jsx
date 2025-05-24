import React, { useState } from 'react';
import './GameInfoPanel.css';

const GameInfoPanel = ({ 
  sidebarVisible, 
  toggleSidebar, 
  game, 
  toggleRankingsModal 
}) => {
  const [showPotDetails, setShowPotDetails] = useState(false);
  
  // Handle toggle click with explicit state management
  const handleToggleClick = (e) => {
    e.stopPropagation(); // Prevent event bubbling
    toggleSidebar();
  };

  // Format player names for display
  const getEligiblePlayerNames = (eligiblePlayerIds) => {
    if (!game.players || !Array.isArray(game.players) || !eligiblePlayerIds) return 'All players';
    
    const eligiblePlayers = game.players
      .filter(player => eligiblePlayerIds.includes(player.id))
      .map(player => player.username);
    
    return eligiblePlayers.length > 0 ? eligiblePlayers.join(', ') : 'All players';
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
              <td className="info-value pot-value" onClick={() => setShowPotDetails(!showPotDetails)}>
                ${game.pot || 0}
                {game.pots && game.pots.length > 1 && (
                  <span className="pot-details-indicator">{showPotDetails ? '▼' : '▶'}</span>
                )}
              </td>
              <td className="info-label">Big Blind:</td>
              <td className="info-value">{game.bigBlindAmount || 'N/A'}</td>
            </tr>
            
            {/* Show pot details when expanded */}
            {showPotDetails && game.pots && game.pots.length > 1 && (
              <tr className="pot-details-row">
                <td colSpan="4" className="pot-details-cell">
                  <div className="pot-details">
                    <div className="pot-detail">
                      <span className="pot-name">Main Pot:</span>
                      <span className="pot-amount">${game.pots[0].amount}</span>
                    </div>
                    {game.pots.slice(1).map((sidePot, index) => (
                      <div key={`side-pot-${index}`} className="pot-detail">
                        <span className="pot-name">Side Pot {index + 1}:</span>
                        <span className="pot-amount">${sidePot.amount}</span>
                        <div className="pot-eligible">
                          <span className="eligible-label">Eligible:</span>
                          <span className="eligible-players">{getEligiblePlayerNames(sidePot.eligiblePlayerIds)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </td>
              </tr>
            )}
            
            <tr>
              <td className="info-label">Current Bet:</td>
              <td className="info-value">${game.currentBet || 0}</td>
              <td className="info-label">Small Blind:</td>
              <td className="info-value">{game.smallBlindAmount || 'N/A'}</td>
            </tr>
            <tr>
              <td className="info-label">Phase:</td>
              <td className="info-value">{game.status ? game.status.replace(/_/g, ' ') : 'Pre-Flop'}</td>
              <td className="info-label">Dealer:</td>
              <td className="info-value">{game.dealerPosition !== undefined ? game.dealerPosition + 1 : 'N/A'}</td>
            </tr>
            <tr>
              <td className="info-label">Current Player:</td>
              <td className="info-value">{game.players && game.currentPlayerIndex !== undefined ? game.players[game.currentPlayerIndex]?.username || 'N/A' : 'N/A'}</td>
              <td className="info-label">Players:</td>
              <td className="info-value">{game.players ? game.players.length : 0}</td>
            </tr>
          </tbody>
        </table>
        
        
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