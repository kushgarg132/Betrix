import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import axios from '../api/axios'; // Import axios for API requests

const Profile = () => {
  const { user, logout, setUser } = useContext(AuthContext); // Add setUser to update user context
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
      setUser({ ...user, balance: response.data.balance }); // Update user context with new balance
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
          <p style={styles.info}><strong>Name:</strong> {user.name}</p>
          <p style={styles.info}><strong>Username:</strong> {user.username}</p>
          <p style={styles.info}><strong>Email:</strong> {user.email}</p>
        </div>
        <div style={styles.balanceContainer}>
          <p style={styles.info}><strong>Balance:</strong> ${user.balance}</p>
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
    height: '100vh',
    backgroundColor: '#f8f9fa',
    padding: '20px',
  },
  card: {
    backgroundColor: '#fff',
    padding: '30px',
    borderRadius: '15px',
    boxShadow: '0 8px 16px rgba(0, 0, 0, 0.2)',
    textAlign: 'center',
    width: '100%',
    maxWidth: '500px',
  },
  title: {
    fontSize: '28px',
    marginBottom: '20px',
    color: '#343a40',
    fontWeight: 'bold',
  },
  infoContainer: {
    marginBottom: '20px',
  },
  info: {
    fontSize: '18px',
    margin: '10px 0',
    color: '#495057',
  },
  balanceContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    margin: '20px 0',
    padding: '10px',
    backgroundColor: '#e9ecef',
    borderRadius: '10px',
  },
  addBalanceButton: {
    padding: '10px 15px',
    backgroundColor: '#28a745',
    color: '#fff',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '16px',
    fontWeight: 'bold',
    transition: 'background-color 0.3s ease',
  },
  logoutButton: {
    padding: '10px 20px',
    backgroundColor: '#dc3545',
    color: '#fff',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    fontSize: '16px',
    fontWeight: 'bold',
    marginTop: '20px',
    transition: 'background-color 0.3s ease',
  },
  loading: {
    fontSize: '24px',
    color: '#6c757d',
    textAlign: 'center',
  },
};

export default Profile;