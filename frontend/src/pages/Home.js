import React from 'react';
import { useNavigate } from 'react-router-dom';

const Home = () => {
  const navigate = useNavigate();

  return (
    <div>
      <h1>Welcome to Poker App</h1>
      <button onClick={() => navigate('/lobby')}>Enter Game Lobby</button>
    </div>
  );
};

export default Home;