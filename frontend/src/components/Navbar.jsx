import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  const { isLoggedIn, user, logout } = useContext(AuthContext);
  const navigate = useNavigate();

  // Function to get initials from the user's name
  const getUserInitials = (name) => {
    if (!name) return '';
    const nameParts = name.split(' ');
    const initials = nameParts.map((part) => part[0]).join('');
    return initials.toUpperCase();
  };

  return (
    <nav className="navbar">
      <div className="navLeft">
        <h1 className="logo" onClick={() => navigate('/')}>
          BETRIX
        </h1>
      </div>
      <div className="navCenter">
        {isLoggedIn && user && (
          <div className="userInfo">
            <span className="userName">Welcome, {user.name}</span>
            <span className="userBalance">${user.balance || 0}</span>
          </div>
        )}
      </div>
      <div className="navRight">
        {isLoggedIn && user ? (
          <div className="userMenu">
            <div className="userIcon">
              <div className="initialsCircle">{getUserInitials(user.name)}</div>
            </div>
            <div className="dropdown">
              <p className="dropdownItem" onClick={() => navigate('/profile')}>
                <strong>Profile</strong>
              </p>
              <p className="dropdownItem" onClick={logout}>
                Logout
              </p>
            </div>
          </div>
        ) : (
          <>
            <button className="navButton" onClick={() => navigate('/login')}>
              Login
            </button>
            <button className="navButton" onClick={() => navigate('/register')}>
              Sign Up
            </button>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;