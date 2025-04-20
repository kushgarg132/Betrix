import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import './App.css';
import PokerTable from './pages/PokerTable';
import Home from './pages/Home';
import GameLobby from './pages/GameLobby';
import Login from './pages/Login';
import Register from './pages/Register';
import Navbar from './components/Navbar';
import Profile from './pages/Profile';
function App() {
  return (<>
    <Router>
      <div className="App">
        
        <Navbar/>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/lobby" element={<GameLobby />} />
          <Route path="/game/:gameId" element={<PokerTable />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </div>
    </Router>
    </>
  );
}

export default App;
