import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import axios from '../api/axios'; // Import axios for API requests

const Profile = () => {
  const { user, logout, updateBalance } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleAddBalance = async () => {
    const amount = prompt('Enter the amount to add to your balance:');
    if (!amount || isNaN(amount) || amount <= 0) {
      alert('Please enter a valid amount.');
      return;
    }

    try {
      const response = await axios.post(`/user/add-balance?amount=${parseInt(amount)}`);
      alert(`Balance updated successfully! New Balance: $${response.data.balance}`);
      
      // Update the user balance in the context (this will update the navbar too)
      updateBalance(response.data.balance);
      // refreshUserData();
      // Alternatively, refresh all user data from the server
      // await refreshUserData();
    } catch (error) {
      console.error('Error adding balance:', error);
      alert('Failed to add balance. Please try again.');
    }
  };

  if (!user) {
    return <div style={styles.loading}>Loading...</div>;
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>Profile</h1>
        <div style={styles.infoContainer}>
          <div style={styles.avatarContainer}>
            <div style={styles.avatar}>
              {user.name ? user.name.charAt(0).toUpperCase() : '?'}
            </div>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Name:</span>
            <span style={styles.infoValue}>{user.name}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Username:</span>
            <span style={styles.infoValue}>{user.username}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Email:</span>
            <span style={styles.infoValue}>{user.email}</span>
          </div>
        </div>
        <div style={styles.balanceContainer}>
          <div style={styles.balanceInfo}>
            <span style={styles.balanceLabel}>Balance</span>
            <span style={styles.balanceValue}>${user.balance}</span>
          </div>
          <button style={styles.addBalanceButton} onClick={handleAddBalance}>
            Add Balance
          </button>
        </div>
        <button style={styles.logoutButton} onClick={handleLogout}>
          Logout
        </button>
      </div>
    </div>
  );
};

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    backgroundColor: '#1a1a2e',
    padding: '20px',
  },
  card: {
    backgroundColor: '#16213e',
    padding: '30px',
    borderRadius: '15px',
    boxShadow: '0 8px 20px rgba(0, 0, 0, 0.3)',
    textAlign: 'center',
    width: '100%',
    maxWidth: '500px',
    color: '#fff',
  },
  title: {
    fontSize: '28px',
    marginBottom: '25px',
    color: '#00aaff',
    fontWeight: 'bold',
    textShadow: '2px 2px 4px rgba(0, 0, 0, 0.5)',
  },
  avatarContainer: {
    display: 'flex',
    justifyContent: 'center',
    marginBottom: '20px',
  },
  avatar: {
    width: '80px',
    height: '80px',
    borderRadius: '50%',
    backgroundColor: '#00aaff',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '36px',
    fontWeight: 'bold',
    boxShadow: '0 4px 8px rgba(0, 0, 0, 0.3)',
  },
  infoContainer: {
    marginBottom: '25px',
  },
  infoItem: {
    display: 'flex',
    justifyContent: 'space-between',
    margin: '12px 0',
    padding: '10px 15px',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: '8px',
    transition: 'background-color 0.3s ease',
  },
  infoLabel: {
    fontWeight: 'bold',
    color: '#00ffcc',
  },
  infoValue: {
    color: '#fff',
  },
  balanceContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    margin: '25px 0',
    padding: '15px',
    backgroundColor: 'rgba(0, 170, 255, 0.15)',
    borderRadius: '10px',
    boxShadow: '0 4px 8px rgba(0, 0, 0, 0.2)',
  },
  balanceInfo: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
  },
  balanceLabel: {
    fontSize: '14px',
    color: '#00ffcc',
    marginBottom: '5px',
  },
  balanceValue: {
    fontSize: '24px',
    fontWeight: 'bold',
    color: '#fff',
  },
  addBalanceButton: {
    padding: '10px 15px',
    backgroundColor: '#00aaff',
    color: '#fff',
    border: 'none',
    borderRadius: '20px',
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 'bold',
    transition: 'background-color 0.3s ease',
    boxShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
  },
  logoutButton: {
    padding: '12px 20px',
    backgroundColor: '#e63946',
    color: '#fff',
    border: 'none',
    borderRadius: '20px',
    cursor: 'pointer',
    fontSize: '16px',
    fontWeight: 'bold',
    marginTop: '20px',
    transition: 'background-color 0.3s ease',
    boxShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
    width: '100%',
  },
  loading: {
    fontSize: '24px',
    color: '#00aaff',
    textAlign: 'center',
    margin: '100px auto',
    textShadow: '1px 1px 2px rgba(0, 0, 0, 0.5)',
  },
};

export default Profile;