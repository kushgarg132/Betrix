import React from 'react';
import './LeftSidebar.css';

const LeftSidebar = ({ leftSidebarVisible, toggleLeftSidebar, leaveTable }) => {
  // Define styles directly in the component
  const containerStyle = {
    position: 'fixed',
    top: '80px',
    left: '0',
    height: 'calc(100vh - 80px)',
    zIndex: '100'
  };
  
  const sidebarStyle = {
    width: '200px',
    background: 'linear-gradient(145deg, #132639, #1e3c5a)',
    color: 'white',
    transition: 'transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275)',
    borderTopRightRadius: '10px',
    borderBottomRightRadius: '10px',
    boxShadow: '5px 0 20px rgba(0, 0, 0, 0.5)',
    borderRight: '1px solid rgba(52, 152, 219, 0.3)',
    height: '100%',
    transform: leftSidebarVisible ? 'translateX(0)' : 'translateX(-100%)'
  };
  
  const contentStyle = {
    padding: '20px',
    height: '100%'
  };
  
  const headerStyle = {
    marginTop: '0',
    marginBottom: '20px',
    fontSize: '1.4rem',
    color: '#3498db',
    textAlign: 'center',
    textShadow: '0 0 10px rgba(52, 152, 219, 0.5)',
    borderBottom: '1px solid rgba(52, 152, 219, 0.3)',
    paddingBottom: '10px'
  };
  
  const buttonStyle = {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '10px 15px',
    width: '100%',
    background: 'linear-gradient(145deg, #c0392b, #e74c3c)',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    marginTop: '20px',
    fontWeight: 'bold',
    cursor: 'pointer',
    boxShadow: '0 5px 15px rgba(0, 0, 0, 0.4)',
    position: 'relative',
    overflow: 'hidden',
    fontSize: '1rem',
    border: '1px solid rgba(231, 76, 60, 0.5)'
  };
  
  const toggleButtonStyle = {
    position: 'absolute',
    left: leftSidebarVisible ? '200px' : '0',
    top: '80px',
    width: '40px',
    height: '60px',
    zIndex: '9999',
    background: 'linear-gradient(145deg, #216396, #2980b9)',
    color: 'white',
    border: '2px solid rgba(52, 152, 219, 0.5)',
    borderRadius: leftSidebarVisible ? '0 5px 5px 0' : '5px',
    cursor: 'pointer',
    fontSize: '1.2rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '3px 0 15px rgba(0, 0, 0, 0.4)'
  };
  
  const iconStyle = {
    marginRight: '10px',
    fontSize: '1.2rem'
  };

  return (
    <div style={containerStyle}>
      {/* Left sidebar content */}
      <div style={sidebarStyle}>
        <div style={contentStyle}>
          <h3 style={headerStyle}>Game Options</h3>
          <button style={buttonStyle} onClick={leaveTable}>
            <span style={iconStyle}>🚪</span>
            <span>Leave Table</span>
          </button>
        </div>
      </div>
      
      {/* Toggle button */}
      <button 
        style={toggleButtonStyle}
        onClick={toggleLeftSidebar}
      >
        {leftSidebarVisible ? '◀' : '▶'}
      </button>
    </div>
  );
};

export default LeftSidebar;