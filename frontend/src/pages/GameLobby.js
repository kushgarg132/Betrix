import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from '../api/axios';
import './GameLobby.css'; // Add a CSS file for styling

const GameLobby = () => {
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    axios
      .get('/game/all')
      .then((response) => {
        setGames(response.data);
        setLoading(false);
        console.log('Games:', response.data);
      })
      .catch((error) => {
        console.error('Error fetching games:', error);
        setLoading(false);
      });
  }, []);

  const createGame = () => {
    axios
      .post('/game/create-new')
      .then((response) => navigate(`/game/${response.data.id}`)) // Redirect to the game page with player ID
      .catch((error) => console.error('Error creating game:', error));
  };

  if (loading) {
    return <div className="loading">Loading games...</div>;
  }

  return (
    <div className="game-lobby-container">
      <h1 className="lobby-title">Game Lobby</h1>
      <button className="create-game-button" onClick={createGame}>
        Create New Game
      </button>
      {games.length === 0 ? (
        <p className="no-games-message">No games available. Create one to get started!</p>
      ) : (
        <ul className="games-list">
          {games.map((game) => (
            <li key={game.id} className="game-item">
              <div className="game-details">
                <p><strong>Game ID:</strong> {game.id}</p>
                <p><strong>Status:</strong> {game.status}</p>
                <p><strong>Players:</strong> {game.players.length}/{game.max_PLAYERS}</p>
                <p><strong>Blinds:</strong> Small - ${game.smallBlindAmount}, Big - ${game.bigBlindAmount}</p>
              </div>
              <button
                className="join-game-button"
                onClick={() => navigate(`/game/${game.id}`)}
              >
                Join Game
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default GameLobby;