import React, { useContext, useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import axios from '../api/axios';

const Profile = () => {
  const { user, logout, updateBalance } = useContext(AuthContext);
  const navigate = useNavigate();
  const [mounted, setMounted] = useState(false);
  const [hoveredButton, setHoveredButton] = useState(null);

  useEffect(() => {
    setMounted(true);
  }, []);

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
    } catch (error) {
      console.error('Error adding balance:', error);
      alert('Failed to add balance. Please try again.');
    }
  };

  const getButtonStyle = (buttonType, baseStyle) => {
    const isHovered = hoveredButton === buttonType;
    return {
      ...baseStyle,
      transform: isHovered ? 'translateY(-2px) scale(1.05)' : 'translateY(0) scale(1)',
      boxShadow: isHovered
        ? (buttonType === 'add'
          ? '0 8px 25px rgba(0, 170, 255, 0.6), 0 0 40px rgba(0, 170, 255, 0.3)'
          : '0 8px 25px rgba(231, 76, 60, 0.6), 0 0 40px rgba(231, 76, 60, 0.3)')
        : baseStyle.boxShadow,
    };
  };

  if (!user) {
    return <div style={styles.loading}>Loading...</div>;
  }

  return (
    <div style={styles.container}>
      {/* Floating poker chips */}
      <div style={styles.floatingChip1}>♠</div>
      <div style={styles.floatingChip2}>♥</div>
      <div style={styles.floatingChip3}>♦</div>

      <div style={{
        ...styles.card,
        animation: mounted ? 'scaleIn 0.6s ease-out' : 'none',
      }}>
        <h1 style={{
          ...styles.title,
          animation: mounted ? 'shimmer 3s linear infinite' : 'none',
        }}>
          Profile
        </h1>
        <div style={{
          ...styles.infoContainer,
          animation: mounted ? 'fadeInUp 0.6s ease-out 0.2s backwards' : 'none',
        }}>
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
        <div style={{
          ...styles.balanceContainer,
          animation: mounted ? 'fadeInUp 0.6s ease-out 0.4s backwards' : 'none',
        }}>
          <div style={styles.balanceInfo}>
            <span style={styles.balanceLabel}>Balance</span>
            <span style={styles.balanceValue}>${user.balance}</span>
          </div>
          <button
            style={getButtonStyle('add', styles.addBalanceButton)}
            onClick={handleAddBalance}
            onMouseEnter={() => setHoveredButton('add')}
            onMouseLeave={() => setHoveredButton(null)}
          >
            Add Balance
          </button>
        </div>
        <button
          style={getButtonStyle('logout', styles.logoutButton)}
          onClick={handleLogout}
          onMouseEnter={() => setHoveredButton('logout')}
          onMouseLeave={() => setHoveredButton(null)}
        >
          Logout
        </button>
      </div>
    </div>
  );
};

// Add keyframes for animations
if (typeof document !== 'undefined') {
  const styleSheet = document.styleSheets[0];
  const animations = `
  @keyframes gradientShift {
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
  }
  @keyframes scaleIn {
    from {
      opacity: 0;
      transform: scale(0.9);
    }
    to {
      opacity: 1;
      transform: scale(1);
    }
  }
  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translateY(30px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  @keyframes shimmer {
    0% { background-position: -1000px 0; }
    100% { background-position: 1000px 0; }
  }
  @keyframes float {
    0%, 100% { transform: translateY(0px); }
    50% { transform: translateY(-20px); }
  }
  @keyframes pulse {
    0%, 100% {
      transform: scale(1);
      box-shadow: 0 0 15px rgba(0, 170, 255, 0.3);
    }
    50% {
      transform: scale(1.05);
      box-shadow: 0 0 25px rgba(0, 170, 255, 0.5);
    }
  }
  `;

  try {
    if (styleSheet && styleSheet.cssRules) {
      styleSheet.insertRule(animations, styleSheet.cssRules.length);
    }
  } catch (e) {
    // Animations already exist or error inserting
  }
}

const styles = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    background: 'linear-gradient(-45deg, #0f0c29, #302b63, #24243e, #0f2027, #203a43, #2c5364)',
    backgroundSize: '400% 400%',
    animation: 'gradientShift 15s ease infinite',
    padding: '20px',
    position: 'relative',
    overflow: 'hidden',
  },
  // Floating poker chips
  floatingChip1: {
    position: 'absolute',
    top: '20%',
    left: '10%',
    fontSize: '3rem',
    color: 'rgba(0, 170, 255, 0.15)',
    animation: 'float 6s ease-in-out infinite',
    zIndex: 1,
    textShadow: '0 0 20px rgba(0, 170, 255, 0.3)',
    pointerEvents: 'none',
  },
  floatingChip2: {
    position: 'absolute',
    top: '60%',
    right: '15%',
    fontSize: '2.5rem',
    color: 'rgba(255, 0, 100, 0.15)',
    animation: 'float 7s ease-in-out infinite 1s',
    zIndex: 1,
    textShadow: '0 0 20px rgba(255, 0, 100, 0.3)',
    pointerEvents: 'none',
  },
  floatingChip3: {
    position: 'absolute',
    bottom: '15%',
    left: '15%',
    fontSize: '2.8rem',
    color: 'rgba(0, 255, 204, 0.15)',
    animation: 'float 8s ease-in-out infinite 2s',
    zIndex: 1,
    textShadow: '0 0 20px rgba(0, 255, 204, 0.3)',
    pointerEvents: 'none',
  },
  card: {
    backgroundColor: 'rgba(22, 33, 62, 0.85)',
    backdropFilter: 'blur(15px)',
    padding: '40px',
    borderRadius: '20px',
    boxShadow: '0 15px 40px rgba(0, 0, 0, 0.5), 0 0 20px rgba(0, 170, 255, 0.1)',
    textAlign: 'center',
    width: '100%',
    maxWidth: '500px',
    color: '#fff',
    border: '1px solid rgba(0, 170, 255, 0.2)',
    position: 'relative',
    zIndex: 2,
  },
  title: {
    fontSize: '32px',
    marginBottom: '30px',
    color: '#00aaff',
    fontWeight: 'bold',
    textShadow: '0 0 15px rgba(0, 170, 255, 0.5)',
    fontFamily: "'Orbitron', sans-serif",
    background: 'linear-gradient(90deg, #00aaff, #00ffcc, #00aaff)',
    backgroundSize: '200% auto',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
    backgroundClip: 'text',
  },
  avatarContainer: {
    display: 'flex',
    justifyContent: 'center',
    marginBottom: '25px',
  },
  avatar: {
    width: '100px',
    height: '100px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, #00aaff, #0088cc)',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '42px',
    fontWeight: 'bold',
    boxShadow: '0 8px 20px rgba(0, 170, 255, 0.4), 0 0 30px rgba(0, 170, 255, 0.2)',
    animation: 'pulse 3s ease-in-out infinite',
  },
  infoContainer: {
    marginBottom: '30px',
  },
  infoItem: {
    display: 'flex',
    justifyContent: 'space-between',
    margin: '12px 0',
    padding: '15px 20px',
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    borderRadius: '10px',
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    border: '1px solid rgba(255, 255, 255, 0.05)',
  },
  infoLabel: {
    fontWeight: 'bold',
    color: '#00ffcc',
    textShadow: '0 0 8px rgba(0, 255, 204, 0.3)',
  },
  infoValue: {
    color: '#fff',
  },
  balanceContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    margin: '30px 0',
    padding: '20px',
    background: 'rgba(0, 170, 255, 0.15)',
    borderRadius: '15px',
    boxShadow: '0 4px 15px rgba(0, 170, 255, 0.2)',
    border: '1px solid rgba(0, 170, 255, 0.3)',
  },
  balanceInfo: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
  },
  balanceLabel: {
    fontSize: '14px',
    color: '#00ffcc',
    marginBottom: '8px',
    textShadow: '0 0 8px rgba(0, 255, 204, 0.3)',
  },
  balanceValue: {
    fontSize: '28px',
    fontWeight: 'bold',
    color: '#fff',
    textShadow: '0 0 10px rgba(255, 255, 255, 0.3)',
  },
  addBalanceButton: {
    padding: '12px 20px',
    background: 'linear-gradient(135deg, #00aaff, #0088cc)',
    color: '#fff',
    border: 'none',
    borderRadius: '25px',
    cursor: 'pointer',
    fontSize: '15px',
    fontWeight: 'bold',
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    boxShadow: '0 4px 15px rgba(0, 170, 255, 0.4)',
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
  },
  logoutButton: {
    padding: '14px 24px',
    background: 'linear-gradient(135deg, #e63946, #c81d45)',
    color: '#fff',
    border: 'none',
    borderRadius: '25px',
    cursor: 'pointer',
    fontSize: '16px',
    fontWeight: 'bold',
    marginTop: '20px',
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    boxShadow: '0 4px 15px rgba(231, 76, 60, 0.4)',
    width: '100%',
    textTransform: 'uppercase',
    letterSpacing: '1px',
  },
  loading: {
    fontSize: '24px',
    color: '#00aaff',
    textAlign: 'center',
    margin: '100px auto',
    textShadow: '0 0 10px rgba(0, 170, 255, 0.5)',
  },
};

export default Profile;