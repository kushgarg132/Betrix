import React from 'react';
import './LeftSidebar.css';

const LeftSidebar = ({
  leaveTable,
  sitOut,
  sitIn,
  isSittingOut,
  soundEnabled,
  setSoundEnabled,
  gameId,
  game
}) => {
  return (
    <div className="left-sidebar">
      {/* Header */}
      <div className="sidebar-header">
        <div className="sidebar-header-icon">⚙</div>
        <div className="sidebar-header-text">
          <h3>GAME MENU</h3>
          <span className="sidebar-subtitle">Table Controls</span>
        </div>
      </div>

      {/* Menu Items */}
      <div className="sidebar-menu">
        <button
          className="menu-item"
          onClick={isSittingOut ? sitIn : sitOut}
        >
          <span className="menu-icon">🪑</span>
          <div className="menu-text">
            <span className="menu-label">{isSittingOut ? 'Sit In' : 'Stand Up'}</span>
            <span className="menu-desc">{isSittingOut ? 'Rejoin the action' : 'Sit out next round'}</span>
          </div>
        </button>

        <button className="menu-item leave" onClick={leaveTable}>
          <span className="menu-icon">🚪</span>
          <div className="menu-text">
            <span className="menu-label leave-label">Leave Table</span>
            <span className="menu-desc">Exit the game</span>
          </div>
        </button>

        <button className="menu-item">
          <span className="menu-icon">⚙️</span>
          <div className="menu-text">
            <span className="menu-label">Settings</span>
            <span className="menu-desc">Game preferences</span>
          </div>
        </button>

        <button className="menu-item">
          <span className="menu-icon">ℹ️</span>
          <div className="menu-text">
            <span className="menu-label">Rules</span>
            <span className="menu-desc">Game rules & help</span>
          </div>
        </button>
      </div>

      {/* Bottom Section */}
      <div className="sidebar-bottom">
        {/* Sound Toggle */}
        <div className="sound-toggle">
          <span className="sound-label">Sound Effects</span>
          <label className="toggle-switch">
            <input
              type="checkbox"
              checked={soundEnabled}
              onChange={(e) => setSoundEnabled(e.target.checked)}
            />
            <span className="toggle-slider"></span>
          </label>
        </div>

        {/* Table Info */}
        <div className="table-info">
          <div className="table-info-row">
            <span className="info-key">TABLE ID</span>
            <span className="info-val">VIP-{gameId?.substring(0, 4) || '0000'}</span>
          </div>
          <div className="table-info-row">
            <span className="info-key">BLINDS</span>
            <span className="info-val">${game?.smallBlindAmount || 500}/${game?.bigBlindAmount || '1,000'}</span>
          </div>
          <div className="table-info-row">
            <span className="info-key">MIN BUY-IN</span>
            <span className="info-val">${game?.minBuyIn?.toLocaleString() || '50,000'}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LeftSidebar;