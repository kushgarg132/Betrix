import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const GameLobby = () => {
  const [games, setGames] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    // Fetch available games from the backend
    axios
      .get('/api/game')
      .then((response) => setGames(response.data))
      .catch((error) => console.error('Error fetching games:', error));
  }, []);

  const createGame = () => {
    axios
      .post('/api/game')
      .then((response) => navigate(`/game/${response.data.id}`))
      .catch((error) => console.error('Error creating game:', error));
  };

  return (
    <div>
      <h1>Game Lobby</h1>
      <button onClick={createGame}>Create New Game</button>
      <ul>
        {games.map((game) => (
          <li key={game.id}>
            Game ID: {game.id} - Status: {game.status}
            <button onClick={() => navigate(`/game/${game.id}`)}>Join</button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default GameLobby;