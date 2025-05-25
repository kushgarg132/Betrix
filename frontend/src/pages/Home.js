import React from 'react';
import { useNavigate } from 'react-router-dom';

const Home = () => {
  const navigate = useNavigate();

  return (
    <div style={styles.container}>
      <div style={styles.overlay}>
        <div style={styles.mainContent}>
          <h1 style={styles.title}>
            <span style={styles.titleHighlight}>BETRIX</span> POKER
          </h1>
          <p style={styles.subtitle}>Experience the thrill of poker in a modern digital environment</p>
          
          <div style={styles.buttonContainer}>
            <button 
              style={styles.primaryButton} 
              onClick={() => navigate('/lobby')}
            >
              Enter Game Lobby
            </button>
            <button 
              style={styles.secondaryButton} 
              onClick={() => navigate('/profile')}
            >
              View Profile
            </button>
          </div>
          
          <div style={styles.featureGrid}>
            <div style={styles.featureCard}>
              <div style={styles.featureIcon}>💰</div>
              <h3 style={styles.featureTitle}>Real-time Play</h3>
              <p style={styles.featureText}>Compete in real-time with players worldwide</p>
            </div>
            <div style={styles.featureCard}>
              <div style={styles.featureIcon}>🏆</div>
              <h3 style={styles.featureTitle}>Tournaments</h3>
              <p style={styles.featureText}>Join tournaments and win big prizes</p>
            </div>
            <div style={styles.featureCard}>
              <div style={styles.featureIcon}>📊</div>
              <h3 style={styles.featureTitle}>Stats Tracking</h3>
              <p style={styles.featureText}>Track your performance and rankings</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles = {
  container: {
    minHeight: '100vh',
    backgroundImage: 'url("https://images.pexels.com/photos/1796794/pexels-photo-1796794.jpeg?cs=srgb&dl=pexels-ken123films-1796794.jpg&fm=jpg")',
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    position: 'relative',
    paddingBottom: 'env(safe-area-inset-bottom, 0)',
  },
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(26, 26, 46, 0.85)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '20px',
    paddingBottom: 'calc(20px + env(safe-area-inset-bottom, 0))',
    overflowY: 'auto',
  },
  mainContent: {
    textAlign: 'center',
    maxWidth: '1000px',
    width: '100%',
    paddingBottom: '20px',
  },
  title: {
    fontSize: '3.5rem',
    fontWeight: 'bold',
    marginBottom: '20px',
    color: '#fff',
    textShadow: '0 3px 6px rgba(0, 0, 0, 0.5)',
    letterSpacing: '2px',
    '@media (max-width: 768px)': {
      fontSize: '2.5rem',
    },
    '@media (max-width: 480px)': {
      fontSize: '2rem',
    },
  },
  titleHighlight: {
    color: '#00aaff',
    textShadow: '0 0 10px rgba(0, 170, 255, 0.5)',
  },
  subtitle: {
    fontSize: '1.5rem',
    marginBottom: '40px',
    color: '#e0e0e0',
    maxWidth: '800px',
    margin: '0 auto 40px',
    '@media (max-width: 768px)': {
      fontSize: '1.2rem',
      marginBottom: '30px',
    },
    '@media (max-width: 480px)': {
      fontSize: '1rem',
      marginBottom: '20px',
    },
  },
  buttonContainer: {
    display: 'flex',
    justifyContent: 'center',
    gap: '20px',
    marginBottom: '50px',
    flexWrap: 'wrap',
    '@media (max-width: 480px)': {
      marginBottom: '30px',
      gap: '15px',
    },
  },
  primaryButton: {
    padding: '15px 30px',
    fontSize: '1.2rem',
    fontWeight: 'bold',
    color: '#fff',
    backgroundColor: '#00aaff',
    border: 'none',
    borderRadius: '30px',
    cursor: 'pointer',
    boxShadow: '0 4px 15px rgba(0, 170, 255, 0.4)',
    transition: 'all 0.3s ease',
    '@media (max-width: 480px)': {
      padding: '12px 25px',
      fontSize: '1rem',
    },
  },
  secondaryButton: {
    padding: '15px 30px',
    fontSize: '1.2rem',
    fontWeight: 'bold',
    color: '#fff',
    backgroundColor: 'transparent',
    border: '2px solid #00ffcc',
    borderRadius: '30px',
    cursor: 'pointer',
    boxShadow: '0 4px 15px rgba(0, 255, 204, 0.2)',
    transition: 'all 0.3s ease',
    '@media (max-width: 480px)': {
      padding: '12px 25px',
      fontSize: '1rem',
    },
  },
  featureGrid: {
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
    gap: '30px',
    marginTop: '20px',
    '@media (max-width: 480px)': {
      gap: '15px',
    },
  },
  featureCard: {
    backgroundColor: 'rgba(22, 33, 62, 0.7)',
    padding: '30px 20px',
    borderRadius: '15px',
    width: '250px',
    boxShadow: '0 8px 20px rgba(0, 0, 0, 0.2)',
    backdropFilter: 'blur(5px)',
    transition: 'transform 0.3s ease, box-shadow 0.3s ease',
    '@media (max-width: 480px)': {
      padding: '20px 15px',
      width: '100%',
    },
  },
  featureIcon: {
    fontSize: '2.5rem',
    marginBottom: '15px',
    '@media (max-width: 480px)': {
      fontSize: '2rem',
      marginBottom: '10px',
    },
  },
  featureTitle: {
    fontSize: '1.3rem',
    color: '#00ffcc',
    marginBottom: '10px',
    fontWeight: 'bold',
    '@media (max-width: 480px)': {
      fontSize: '1.1rem',
    },
  },
  featureText: {
    fontSize: '1rem',
    color: '#e0e0e0',
    lineHeight: '1.5',
    '@media (max-width: 480px)': {
      fontSize: '0.9rem',
    },
  }
};

export default Home;