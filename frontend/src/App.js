import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import './App.css';
import PokerTable from './PokerTable';
import Home from './pages/Home';
import GameLobby from './pages/GameLobby';
import Login from './pages/Login';
import Register from './pages/Register';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/lobby" element={<GameLobby />} />
          <Route path="/game/:gameId" element={<PokerTable />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
