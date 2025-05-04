import React from 'react';
import './LeftSidebar.css';

const LeftSidebar = ({ leftSidebarVisible, toggleLeftSidebar, leaveTable }) => {
  return (
    <div className="left-sidebar-container">
      <div 
        className={`left-sidebar ${leftSidebarVisible ? 'expanded' : 'collapsed'}`} 
        data-testid="left-sidebar"
      >
        <div className="sidebar-content">
          <h3>Game Options</h3>
          <button className="leave-table-button" onClick={leaveTable}>
            <span className="button-icon">🚪</span>
            <span>Leave Table</span>
          </button>
        </div>
      </div>
      
      {/* Separate toggle button with position based on visibility state */}
      <button 
        className={`left-sidebar-toggle-btn ${leftSidebarVisible ? '' : 'collapsed'}`}
        style={{
          left: leftSidebarVisible ? '200px' : '0'
        }}
        onClick={toggleLeftSidebar}
        aria-label="Toggle options menu"
      >
        {leftSidebarVisible ? '◀' : '▶'}
      </button>
    </div>
  );
};

export default LeftSidebar; 