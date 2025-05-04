import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../api/axios';
import './GameLobby.css';

const GameLobby = () => {
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sidebarVisible, setSidebarVisible] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchGames();
    // Set up polling to refresh games list every 30 seconds
    const intervalId = setInterval(fetchGames, 30000);
    return () => clearInterval(intervalId);
  }, []);

  const fetchGames = () => {
    setLoading(true);
    axios
      .get('/game/all')
      .then((response) => {
        setGames(response.data);
        setLoading(false);
      })
      .catch((error) => {
        console.error('Error fetching games:', error);
        setLoading(false);
      });
  };

  const createGame = () => {
    axios
      .post('/game/create-new')
      .then((response) => navigate(`/game/${response.data.id}`))
      .catch((error) => console.error('Error creating game:', error));
  };

  const toggleSidebar = () => {
    setSidebarVisible(prev => !prev);
  };

  const getStatusClass = (status) => {
    switch(status) {
      case 'WAITING':
        return 'status-waiting';
      case 'ACTIVE':
        return 'status-active';
      case 'COMPLETED':
        return 'status-completed';
      default:
        return '';
    }
  };

  if (loading) {
    return (
      <div className="lobby-loading">
        <div className="loading-spinner"></div>
        <div className="loading-text">Loading games...</div>
      </div>
    );
  }

  return (
    <div className="game-lobby">
      <div className="lobby-header">
        <h1 className="lobby-title">Game Lobby</h1>
        <button className="refresh-button" onClick={fetchGames}>
          <span className="refresh-icon">↻</span>
        </button>
      </div>
      
      <div className="lobby-content">
        <div className="main-content">
          <button className="create-game-button" onClick={createGame}>
            <span className="create-icon">+</span>
            <span>Create New Game</span>
          </button>
          
          {games.length === 0 ? (
            <div className="no-games-message">
              <div className="empty-state-icon">♠</div>
              <p>No games available. Create one to get started!</p>
            </div>
          ) : (
            <div className="games-list">
              {games.map((game) => (
                <div key={game.id} className="game-card">
                  <div className="game-card-header">
                    <span className={`game-status ${getStatusClass(game.status)}`}>
                      {game.status}
                    </span>
                    <span className="game-id">ID: {game.id.substring(0, 8)}...</span>
                  </div>
                  
                  <div className="game-card-content">
                    <div className="game-info-grid">
                      <div className="info-item">
                        <span className="info-label">Players:</span>
                        <span className="info-value">{game.players.length}/{game.max_PLAYERS}</span>
                      </div>
                      
                      <div className="info-item">
                        <span className="info-label">Small Blind:</span>
                        <span className="info-value">${game.smallBlindAmount}</span>
                      </div>
                      
                      <div className="info-item">
                        <span className="info-label">Big Blind:</span>
                        <span className="info-value">${game.bigBlindAmount}</span>
                      </div>
                    </div>
                    
                    <button
                      className="join-game-button"
                      onClick={() => navigate(`/game/${game.id}`)}
                      // disabled={game.status !== 'WAITING' && game.status !== 'ACTIVE'}
                    >
                      <span className="join-icon">→</span>
                      <span>{game.status === 'WAITING' ? 'Join Game' : game.status === 'ACTIVE' ? 'Spectate' : 'Completed'}</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
        
        <div className={`lobby-sidebar ${sidebarVisible ? 'expanded' : 'collapsed'}`}>
          <div className="sidebar-toggle" onClick={toggleSidebar}>
            <div className="hamburger-icon">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          
          <div className="sidebar-content">
            <h3>Poker Info</h3>
            <div className="sidebar-section">
              <h4>Active Games</h4>
              <div className="stats-value">{games.filter(game => game.status === 'ACTIVE').length}</div>
            </div>
            
            <div className="sidebar-section">
              <h4>Waiting Games</h4>
              <div className="stats-value">{games.filter(game => game.status === 'WAITING').length}</div>
            </div>
            
            <div className="sidebar-section">
              <h4>Game Rules</h4>
              <ul className="rules-list">
                <li>Texas Hold'em Rules</li>
                <li>Small & Big Blinds</li>
                <li>Max 6 players per table</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GameLobby;