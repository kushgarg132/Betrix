import React, { useState, useContext, useEffect } from 'react';
import axios from '../api/axios';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import './Auth.css';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [mounted, setMounted] = useState(false);
  const [hoveredButton, setHoveredButton] = useState(null);
  const navigate = useNavigate();
  const { login } = useContext(AuthContext);

  useEffect(() => {
    setMounted(true);
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post('/auth/login', { username, password });
      login(response.data.token);
      navigate('/');
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  const handleGuestLogin = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post('/auth/guest');
      login(response.data.token);
      navigate('/');
    } catch (error) {
      console.error('Guest Login failed:', error);
    }
  };

  return (
    <div className={`auth-container ${mounted ? 'mounted' : ''}`}>
      {/* Floating poker chips */}
      <div className="floating-suit floating-suit-1">♠</div>
      <div className="floating-suit floating-suit-2">♥</div>
      <div className="floating-suit floating-suit-3">♦</div>
      <div className="floating-suit floating-suit-4">♣</div>

      <div className="glass-card auth-card">
        <h1 className="auth-title">Login</h1>
        <form onSubmit={handleLogin} className="auth-form">
          <div className="input-group">
            <input
              type="text"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="input-group">
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button
            type="submit"
            className="btn-primary w-full"
            onMouseEnter={() => setHoveredButton('login')}
            onMouseLeave={() => setHoveredButton(null)}
          >
            Login
          </button>
        </form>
        <button
          onClick={handleGuestLogin}
          className="guest-btn"
          onMouseEnter={() => setHoveredButton('guest')}
          onMouseLeave={() => setHoveredButton(null)}
        >
          Continue as Guest
        </button>
        <p className="auth-footer">
          Don't have an account?
          <span
            className="auth-link"
            onClick={() => navigate('/register')}
          >
            Sign Up
          </span>
        </p>
      </div>
    </div>
  );
};

export default Login;