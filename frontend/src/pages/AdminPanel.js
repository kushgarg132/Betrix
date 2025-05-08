import React, { useState, useEffect, useContext } from 'react';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import './AdminPanel.css';

const AdminPanel = () => {
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [deleteStatus, setDeleteStatus] = useState({ message: '', isError: false });
  const { user } = useContext(AuthContext);

  // Check if user is admin
  const isAdmin = user && user.roles && user.roles.includes('ADMIN');

  useEffect(() => {
    // Redirect non-admin users
    if (!isAdmin) {
      window.location.href = '/';
      return;
    }

    // Fetch all games
    const fetchGames = async () => {
      try {
        setLoading(true);
        const response = await axios.get('/game/all');
        setGames(response.data);
        setLoading(false);
      } catch (err) {
        console.error('Error fetching games:', err);
        setError('Failed to load games. Please try again later.');
        setLoading(false);
      }
    };

    fetchGames();
  }, [isAdmin]);

  const handleDeleteGame = async (gameId) => {
    if (!window.confirm(`Are you sure you want to delete game ${gameId}?`)) {
      return;
    }

    try {
      await axios.delete(`/game/${gameId}`);
      
      // Update games list
      setGames(games.filter(game => game.id !== gameId));
      
      // Show success message
      setDeleteStatus({
        message: `Game ${gameId} successfully deleted`,
        isError: false
      });
      
      // Clear status message after 3 seconds
      setTimeout(() => setDeleteStatus({ message: '', isError: false }), 3000);
    } catch (err) {
      console.error('Error deleting game:', err);
      
      // Show error message
      setDeleteStatus({
        message: `Failed to delete game: ${err.response?.data?.message || err.message}`,
        isError: true
      });
    }
  };

  if (!isAdmin) {
    return <div className="admin-panel">Access denied. Admin privileges required.</div>;
  }

  if (loading) {
    return <div className="admin-panel loading">Loading games...</div>;
  }

  if (error) {
    return <div className="admin-panel error">{error}</div>;
  }

  return (
    <div className="admin-panel">
      <h1>Admin Panel</h1>
      
      {deleteStatus.message && (
        <div className={`status-message ${deleteStatus.isError ? 'error' : 'success'}`}>
          {deleteStatus.message}
        </div>
      )}
      
      <div className="admin-section">
        <h2>Manage Games</h2>
        
        {games.length === 0 ? (
          <p>No games available.</p>
        ) : (
          <table className="games-table">
            <thead>
              <tr>
                <th>Game ID</th>
                <th>Status</th>
                <th>Players</th>
                <th>Created</th>
                <th>Last Activity</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {games.map(game => (
                <tr key={game.id}>
                  <td>{game.id}</td>
                  <td>{game.status}</td>
                  <td>{game.players?.length || 0}</td>
                  <td>{new Date(game.createdAt).toLocaleString()}</td>
                  <td>{new Date(game.updatedAt || game.createdAt).toLocaleString()}</td>
                  <td>
                    <button 
                      className="delete-button"
                      onClick={() => handleDeleteGame(game.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default AdminPanel;
