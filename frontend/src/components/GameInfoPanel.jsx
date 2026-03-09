import React, { useState } from 'react';
import './GameInfoPanel.css';

const GameInfoPanel = ({
  sidebarVisible,
  toggleSidebar,
  game,
  toggleRankingsModal,
  chatMessages = [],
  actionLogs = [],
  sendChatMessage
}) => {
  const [showPotDetails, setShowPotDetails] = useState(false);
  const [activeTab, setActiveTab] = useState('chat');
  const [chatInput, setChatInput] = useState('');

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
        </div>

        {/* Tabs for Chat and Logs */}
        <div className="panel-tabs" style={{ display: 'flex', marginTop: '1rem', borderBottom: '1px solid #444' }}>
          <button
            style={{ flex: 1, padding: '8px', background: activeTab === 'chat' ? '#333' : 'transparent', color: 'white', border: 'none', cursor: 'pointer' }}
            onClick={() => setActiveTab('chat')}>Chat</button>
          <button
            style={{ flex: 1, padding: '8px', background: activeTab === 'logs' ? '#333' : 'transparent', color: 'white', border: 'none', cursor: 'pointer' }}
            onClick={() => setActiveTab('logs')}>Action Log</button>
        </div>

        <div className="panel-tab-content" style={{ display: 'flex', flexDirection: 'column', height: '250px', marginTop: '0.5rem' }}>
          {activeTab === 'chat' && (
            <div className="chat-container" style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
              <div className="chat-messages" style={{ flex: 1, overflowY: 'auto', marginBottom: '0.5rem', fontSize: '0.85rem' }}>
                {chatMessages?.map((msg, i) => (
                  <div key={i} className="chat-message" style={{ marginBottom: '4px', wordBreak: 'break-word' }}>
                    <strong style={{ color: '#64b5f6' }}>{msg.senderName}:</strong> {msg.message}
                  </div>
                ))}
              </div>
              <div className="chat-input-row" style={{ display: 'flex', gap: '5px' }}>
                <input
                  type="text"
                  style={{ flex: 1, padding: '6px', borderRadius: '4px', border: '1px solid #555', background: '#222', color: 'white' }}
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && chatInput.trim()) { sendChatMessage(chatInput); setChatInput(''); } }}
                  placeholder="Type message..."
                />
                <button
                  style={{ padding: '6px 12px', background: '#4caf50', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                  onClick={() => { if (chatInput.trim()) { sendChatMessage(chatInput); setChatInput(''); } }}>
                  Send
                </button>
              </div>
            </div>
          )}

          {activeTab === 'logs' && (
            <div className="logs-container" style={{ flex: 1, overflowY: 'auto', fontSize: '0.85rem' }}>
              {actionLogs?.map((log, i) => (
                <div key={i} className="log-entry" style={{ marginBottom: '4px', opacity: 0.8 }}>
                  <span className="log-time" style={{ color: '#aaa', marginRight: '6px', fontSize: '0.75rem' }}>
                    {new Date(log.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                  </span>
                  <span className="log-message">{log.message}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default GameInfoPanel; 