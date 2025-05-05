import React, { useContext, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { isLoggedIn, user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);

  // Function to get initials from the user's name
  const getUserInitials = (name) => {
    if (!name) return '';
    const nameParts = name.split(' ');
    const initials = nameParts.map((part) => part[0]).join('');
    return initials.toUpperCase();
  };

  // Check if current path matches the link
  const isActive = (path) => {
    return location.pathname === path;
  };

  const toggleDropdown = () => {
    setDropdownOpen(!dropdownOpen);
  };

  const closeDropdown = () => {
    setDropdownOpen(false);
  };

  return (
    <nav className="navbar">
      <div className="navLeft">
        <h1 className="logo" onClick={() => navigate('/')}>
          BETRIX<span className="logoAccent">POKER</span>
        </h1>
      </div>
      
      <div className="navCenter">
        <div className="navLinks">
          <div 
            className={`navLink ${isActive('/') ? 'active' : ''}`} 
            onClick={() => navigate('/')}
          >
            Home
          </div>
          <div 
            className={`navLink ${isActive('/lobby') ? 'active' : ''}`} 
            onClick={() => navigate('/lobby')}
          >
            Game Lobby
          </div>
          {isLoggedIn && (
            <div 
              className={`navLink ${isActive('/profile') ? 'active' : ''}`} 
              onClick={() => navigate('/profile')}
            >
              Profile
            </div>
          )}
        </div>
      </div>
      
      <div className="navRight">
        {isLoggedIn && user ? (
          <>
            <div className="userInfo">
              <span className="userName">Welcome, {user.name}</span>
              <span className="userBalance">${user.balance || 0}</span>
            </div>
            <div className="userMenu">
              <div className="userIcon" onClick={toggleDropdown}>
                <div className="initialsCircle">{getUserInitials(user.name)}</div>
              </div>
              <div className={`dropdown ${dropdownOpen ? 'show' : ''}`}>
                <div className="dropdownItem" onClick={() => {
                  navigate('/profile');
                  closeDropdown();
                }}>
                  <i className="fas fa-user-circle"></i> Profile
                </div>
                <div className="dropdownItem" onClick={() => {
                  logout();
                  closeDropdown();
                  navigate('/login');
                }}>
                  <i className="fas fa-sign-out-alt"></i> Logout
                </div>
              </div>
            </div>
          </>
        ) : (
          <div className="authButtons">
            <button className="navButton loginBtn" onClick={() => navigate('/login')}>
              Login
            </button>
            <button className="navButton signupBtn" onClick={() => navigate('/register')}>
              Sign Up
            </button>
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;