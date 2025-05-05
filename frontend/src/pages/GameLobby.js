import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import './GameLobby.css';

const GameLobby = () => {
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [smallBlind, setSmallBlind] = useState('');
  const [bigBlind, setBigBlind] = useState('');
  const [showCustomOption, setShowCustomOption] = useState(false);
  const [error, setError] = useState('');
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();

  // Preset blind options
  const blindOptions = [
    { small: 5, big: 10 },
    { small: 25, big: 50 },
    { small: 50, big: 100 },
    { small: 100, big: 200 }
  ];

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

  const openCreateGameModal = () => {
    setSmallBlind('');
    setBigBlind('');
    setShowCustomOption(false);
    setError('');
    setShowModal(true);
  };

  const handleModalClose = () => {
    setShowModal(false);
  };

  const handleSelectBlinds = (small, big) => {
    setSmallBlind(small.toString());
    setBigBlind(big.toString());
    createGameWithBlinds(small, big);
  };

  const handleCustomOption = () => {
    setSmallBlind('');
    setBigBlind('');
    setShowCustomOption(true);
  };

  const handleCreateCustomGame = () => {
    // Validate inputs
    if (!smallBlind || isNaN(smallBlind) || parseFloat(smallBlind) <= 0) {
      setError('Please enter a valid small blind amount.');
      return;
    }

    if (!bigBlind || isNaN(bigBlind) || parseFloat(bigBlind) <= 0) {
      setError('Please enter a valid big blind amount.');
      return;
    }

    if (parseFloat(bigBlind) < parseFloat(smallBlind)) {
      setError('Big blind amount must be greater than or equal to small blind amount.');
      return;
    }

    createGameWithBlinds(parseFloat(smallBlind), parseFloat(bigBlind));
  };

  const createGameWithBlinds = (smallBlindAmount, bigBlindAmount) => {
    // Create game with specified blind amounts
    axios
      .post('/game/create-new', {
        smallBlindAmount: smallBlindAmount,
        bigBlindAmount: bigBlindAmount
      })
      .then(() => {
        setShowModal(false);
        fetchGames();
      })
      .catch((error) => {
        console.error('Error creating game:', error);
        setError('Failed to create game. Please try again.');
      });
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

  // Check if current user is already in a game
  const isPlayerInGame = (game) => {
    if (!user || !game.players || !game.players.length) return false;
    return game.players.some(player => player.username === user.username);
  };

  if (loading) {
    return (
      <div className="lobby-loading">
        <div className="loading-spinner"></div>
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
          <button className="create-game-button" onClick={openCreateGameModal}>
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
                      <span>
                        {isPlayerInGame(game) ? 'Already Joined' : 
                          game.status === 'WAITING' ? 'Join Game' : 'Spectate'}
                      </span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Create Game Modal */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal-container">
            <div className="modal-header">
              <h2>Create New Game</h2>
              <button className="modal-close" onClick={handleModalClose}>×</button>
            </div>
            <div className="modal-body">
              {error && <div className="error-message">{error}</div>}
              
              {!showCustomOption ? (
                <div className="blind-options">
                  <h3>Select Blinds:</h3>
                  <div className="option-grid">
                    {blindOptions.map((option, index) => (
                      <div 
                        key={index} 
                        className="blind-option" 
                        onClick={() => handleSelectBlinds(option.small, option.big)}
                      >
                        <div className="blind-values">
                          <span className="small-blind">${option.small}</span>
                          <span className="blind-separator">/</span>
                          <span className="big-blind">${option.big}</span>
                        </div>
                      </div>
                    ))}
                    <div 
                      className="blind-option custom-option" 
                      onClick={handleCustomOption}
                    >
                      <div className="blind-values">
                        <span>Custom</span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="custom-blinds">
                  <div className="form-group">
                    <label htmlFor="smallBlind">Small Blind Amount ($)</label>
                    <input
                      type="number"
                      id="smallBlind"
                      value={smallBlind}
                      onChange={(e) => setSmallBlind(e.target.value)}
                      min="1"
                      placeholder="Enter small blind amount"
                    />
                  </div>
                  <div className="form-group">
                    <label htmlFor="bigBlind">Big Blind Amount ($)</label>
                    <input
                      type="number"
                      id="bigBlind"
                      value={bigBlind}
                      onChange={(e) => setBigBlind(e.target.value)}
                      min="1"
                      placeholder="Enter big blind amount"
                    />
                  </div>
                  <div className="custom-actions">
                    <button className="modal-button cancel" onClick={() => setShowCustomOption(false)}>
                      Back
                    </button>
                    <button className="modal-button confirm" onClick={handleCreateCustomGame}>
                      Create Game
                    </button>
                  </div>
                </div>
              )}
            </div>
            {!showCustomOption && (
              <div className="modal-footer">
                <button className="modal-button cancel" onClick={handleModalClose}>Cancel</button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default GameLobby;