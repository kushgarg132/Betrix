import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Toaster } from 'sonner';
import PokerTable from './pages/PokerTable';
import Home from './pages/Home';
import GameLobby from './pages/GameLobby';
import Login from './pages/Login';
import Register from './pages/Register';
import Navbar from './components/layout/Navbar';
import Profile from './pages/Profile';
import AdminPanel from './pages/AdminPanel';
import NotFound from './pages/NotFound';

function App() {
  return (
    <Router>
      <div className="min-h-dvh flex flex-col bg-background text-text">
        <Navbar />
        <Routes>
          <Route path="/"        element={<Home />} />
          <Route path="/login"   element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/lobby"   element={<GameLobby />} />
          <Route path="/game/:gameId" element={<PokerTable />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/admin"   element={<AdminPanel />} />
          <Route path="*"        element={<NotFound />} />
        </Routes>
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: 'var(--color-surface-elevated)',
              color: 'var(--color-text)',
              border: '1px solid var(--color-border)',
            },
          }}
        />
      </div>
    </Router>
  );
}

export default App;
