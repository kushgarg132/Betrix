import React, { useState, useMemo } from 'react';
import './GameInfoPanel.css';

const GameInfoPanel = ({
  game,
  toggleRankingsModal,
  chatMessages = [],
  actionLogs = [],
  sendChatMessage
}) => {
  const [activeTab, setActiveTab] = useState('rankings');
  const [chatInput, setChatInput] = useState('');

  // Generate leaderboard from players sorted by chips
  const leaderboard = useMemo(() => {
    if (!game?.players) return [];
    return [...game.players]
      .filter(p => p)
      .sort((a, b) => (b.chips || 0) - (a.chips || 0))
      .map((player, idx) => ({
        rank: idx + 1,
        username: player.username,
        chips: player.chips || 0,
        initials: (() => {
          const name = player.username || '';
          const parts = name.split(/[\s._-]+/);
          return parts.length >= 2
            ? (parts[0][0] + parts[1][0]).toUpperCase()
            : name.substring(0, 2).toUpperCase();
        })()
      }));
  }, [game?.players]);

  const handleSendChat = () => {
    if (chatInput.trim() && sendChatMessage) {
      sendChatMessage(chatInput);
      setChatInput('');
    }
  };

  return (
    <div className="right-sidebar">
      {/* Tab Bar */}
      <div className="tab-bar">
        <button
          className={`tab-btn ${activeTab === 'rankings' ? 'active' : ''}`}
          onClick={() => setActiveTab('rankings')}
        >
          <span className="tab-icon">🏆</span> Rankings
        </button>
        <button
          className={`tab-btn ${activeTab === 'chat' ? 'active' : ''}`}
          onClick={() => setActiveTab('chat')}
        >
          <span className="tab-icon">💬</span> Chat
        </button>
        <button
          className={`tab-btn ${activeTab === 'info' ? 'active' : ''}`}
          onClick={() => setActiveTab('info')}
        >
          <span className="tab-icon">ℹ️</span> Info
        </button>
      </div>

      {/* Tab Content */}
      <div className="tab-content">
        {/* Rankings Tab */}
        {activeTab === 'rankings' && (
          <div className="rankings-tab">
            <h4 className="section-title">
              <span className="title-icon">🏆</span> LEADERBOARD
            </h4>
            <div className="leaderboard-list">
              {leaderboard.map((entry) => (
                <div key={entry.username} className={`leaderboard-row ${entry.rank <= 3 ? 'top-three' : ''}`}>
                  <div className={`rank-badge rank-${entry.rank <= 3 ? entry.rank : 'default'}`}>
                    {entry.rank}
                  </div>
                  <div className="lb-player-info">
                    <span className="lb-name">{entry.username}</span>
                    <span className="lb-chips">$ {entry.chips.toLocaleString()}</span>
                  </div>
                  <div className="lb-card-icon">🃏</div>
                </div>
              ))}
            </div>

            {/* Session Stats */}
            <div className="session-section">
              <h4 className="session-title">YOUR SESSION</h4>
              <div className="session-stats">
                <div className="stat-row">
                  <span className="stat-label">Hands Played</span>
                  <span className="stat-value">{game?.handsPlayed || 0}</span>
                </div>
                <div className="stat-row">
                  <span className="stat-label">Hands Won</span>
                  <span className="stat-value">{game?.handsWon || 0}</span>
                </div>
                <div className="stat-row">
                  <span className="stat-label">Win Rate</span>
                  <span className="stat-value">
                    {game?.handsPlayed > 0
                      ? ((game?.handsWon / game?.handsPlayed) * 100).toFixed(1) + '%'
                      : '0%'}
                  </span>
                </div>
                <div className="stat-row">
                  <span className="stat-value profit">{game?.netProfit >= 0 ? '+' : ''}${game?.netProfit?.toLocaleString() || 0}</span>
                  <span className="stat-label">Net Profit</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Chat Tab */}
        {activeTab === 'chat' && (
          <div className="chat-tab">
            <div className="chat-messages">
              {chatMessages.length === 0 && (
                <div className="chat-empty">No messages yet. Start the conversation!</div>
              )}
              {chatMessages.map((msg, i) => (
                <div key={i} className="chat-message">
                  <strong className="chat-sender">{msg.senderName}:</strong>
                  <span className="chat-text">{msg.message}</span>
                </div>
              ))}
            </div>
            <div className="chat-input-row">
              <input
                type="text"
                className="chat-input"
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') handleSendChat(); }}
                placeholder="Type message..."
              />
              <button className="chat-send-btn" onClick={handleSendChat}>Send</button>
            </div>
          </div>
        )}

        {/* Info Tab */}
        {activeTab === 'info' && (
          <div className="info-tab">
            <h4 className="section-title">GAME INFO</h4>
            <div className="info-grid">
              <div className="info-row">
                <span className="info-label">Pot</span>
                <span className="info-value gold">${game?.pot || 0}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Current Bet</span>
                <span className="info-value">${game?.currentBet || 0}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Big Blind</span>
                <span className="info-value">{game?.bigBlindAmount || 'N/A'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Small Blind</span>
                <span className="info-value">{game?.smallBlindAmount || 'N/A'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Phase</span>
                <span className="info-value">{game?.status ? game.status.replace(/_/g, ' ') : 'Pre-Flop'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Dealer</span>
                <span className="info-value">{game?.dealerPosition !== undefined ? game.dealerPosition + 1 : 'N/A'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">Current Player</span>
                <span className="info-value">
                  {game?.players && game.currentPlayerIndex !== undefined
                    ? game.players[game.currentPlayerIndex]?.username || 'N/A'
                    : 'N/A'}
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">Players</span>
                <span className="info-value">{game?.players ? game.players.length : 0}</span>
              </div>
            </div>

            <button className="rankings-view-btn" onClick={toggleRankingsModal}>
              ♠ Hand Rankings
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default GameInfoPanel;