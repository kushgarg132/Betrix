import React, { useContext, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { isLoggedIn, user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

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

  const handleNavigate = (path) => {
    navigate(path);
    setMobileOpen(false);
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
            onClick={() => handleNavigate('/')}
          >
            Home
          </div>
          <div 
            className={`navLink ${isActive('/lobby') ? 'active' : ''}`} 
            onClick={() => handleNavigate('/lobby')}
          >
            Game Lobby
          </div>
          {isLoggedIn && (
            <div 
              className={`navLink ${isActive('/profile') ? 'active' : ''}`} 
              onClick={() => handleNavigate('/profile')}
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
              <span className="userName">{user.name}</span>
              <span className="userBalance">${user.balance || 0}</span>
            </div>
            <div className="userMenu">
              <div className="userIcon" onClick={toggleDropdown}>
                <div className="initialsCircle">{getUserInitials(user.name)}</div>
              </div>
              <div className={`dropdown ${dropdownOpen ? 'show' : ''}`}>
                <div className="dropdownItem" onClick={() => {
                  handleNavigate('/profile');
                  closeDropdown();
                }}>
                  <i className="fas fa-user-circle"></i> Profile
                </div>
                <div className="dropdownItem" onClick={() => {
                  logout();
                  closeDropdown();
                  handleNavigate('/login');
                }}>
                  <i className="fas fa-sign-out-alt"></i> Logout
                </div>
              </div>
            </div>
          </>
        ) : (
          <div className="authButtons">
            <button className="navButton loginBtn" onClick={() => handleNavigate('/login')}>
              Login
            </button>
            <button className="navButton signupBtn" onClick={() => handleNavigate('/register')}>
              Sign Up
            </button>
          </div>
        )}
      </div>

      {/* Hamburger for mobile */}
      <button
        className={`hamburger ${mobileOpen ? 'open' : ''}`}
        aria-label="Toggle navigation menu"
        aria-expanded={mobileOpen}
        onClick={() => setMobileOpen((prev) => !prev)}
      >
        <span className="bar"></span>
        <span className="bar"></span>
        <span className="bar"></span>
      </button>

      {/* Mobile slide-down menu */}
      <div className={`mobileMenu ${mobileOpen ? 'open' : ''}`}>
        <div className="mobileSection">
          <div
            className={`navLink ${isActive('/') ? 'active' : ''}`}
            onClick={() => handleNavigate('/')}
          >
            Home
          </div>
          <div
            className={`navLink ${isActive('/lobby') ? 'active' : ''}`}
            onClick={() => handleNavigate('/lobby')}
          >
            Game Lobby
          </div>
          {isLoggedIn && (
            <div
              className={`navLink ${isActive('/profile') ? 'active' : ''}`}
              onClick={() => handleNavigate('/profile')}
            >
              Profile
            </div>
          )}
        </div>

        <div className="mobileSection">
          {isLoggedIn && user ? (
            <>
              <div className="mobileUser">
                <div className="initialsCircle small">{getUserInitials(user.name)}</div>
                <div className="mobileUserInfo">
                  <div className="userName">{user.name}</div>
                  <div className="userBalance">${user.balance || 0}</div>
                </div>
              </div>
              <div className="dropdownItem" onClick={() => { logout(); handleNavigate('/login'); }}>
                <i className="fas fa-sign-out-alt"></i> Logout
              </div>
            </>
          ) : (
            <div className="authButtons column">
              <button className="navButton loginBtn" onClick={() => handleNavigate('/login')}>
                Login
              </button>
              <button className="navButton signupBtn" onClick={() => handleNavigate('/register')}>
                Sign Up
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;