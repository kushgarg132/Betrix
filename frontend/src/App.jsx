import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { MotionConfig } from 'framer-motion';
import { Toaster } from 'sonner';
import PokerTable from './pages/PokerTable';
import Home from './pages/Home';
import GameLobby from './pages/GameLobby';
import TopBar from './components/layout/TopBar';
import BottomTabBar from './components/layout/BottomTabBar';
import Profile from './pages/Profile';
import AdminPanel from './pages/AdminPanel';
import NotFound from './pages/NotFound';
import { AdminRoute, ProtectedRoute } from './components/routing/ProtectedRoute';

function App() {
  return (
    <MotionConfig reducedMotion="user">
      <Router>
        <div className="min-h-dvh flex flex-col bg-background text-text">
          <TopBar />
          <Routes>
            <Route path="/"        element={<Home />} />
            <Route path="/lobby"   element={<ProtectedRoute><GameLobby /></ProtectedRoute>} />
            <Route path="/game/:gameId" element={<ProtectedRoute><PokerTable /></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
            <Route path="/admin"   element={<AdminRoute><AdminPanel /></AdminRoute>} />
            <Route path="*"        element={<NotFound />} />
          </Routes>
          <BottomTabBar />
          <Toaster
            position="top-center"
            // clear the 56px top bars (app TopBar and TableTopBar) so toasts never cover leave/chat
            offset={72}
            mobileOffset={{ top: 72 }}
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
    </MotionConfig>
  );
}

export default App;
