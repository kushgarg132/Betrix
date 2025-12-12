import React from 'react';
import './LeftSidebar.css';

const LeftSidebar = ({ leftSidebarVisible, toggleLeftSidebar, leaveTable, sitOut, sitIn, isSittingOut }) => {
  return (
    <div className="left-sidebar-wrapper">
      {/* Left sidebar content */}
      <div className={`left-sidebar ${leftSidebarVisible ? 'expanded' : 'collapsed'}`}>
        <div className="sidebar-content">
          <h3 className="sidebar-header">Game Options</h3>
          <button className="leave-table-button" onClick={leaveTable}>
            <span className="button-icon">🚪</span>
            <span>Leave Table</span>
          </button>
          
          <button className={`sit-out-button ${isSittingOut ? 'sitting-out' : ''}`} onClick={isSittingOut ? sitIn : sitOut}>
            <span className="button-icon">{isSittingOut ? '🪑' : '☕'}</span>
            <span>{isSittingOut ? 'Sit In' : 'Sit Out'}</span>
          </button>
        </div>
      </div>
      
      {/* Toggle button */}
      <button 
        className={`sidebar-toggle left ${leftSidebarVisible ? 'expanded' : 'collapsed'}`}
        onClick={toggleLeftSidebar}
        aria-label={leftSidebarVisible ? "Hide game options" : "Show game options"}
      >
        {leftSidebarVisible ? '◀' : '▶'}
      </button>
    </div>
  );
};

export default LeftSidebar;