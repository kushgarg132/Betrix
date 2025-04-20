import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { AuthContext } from '../context/AuthContext';

const Home = () => {
  const navigate = useNavigate();

  return (
    <div style={styles.container}>
      <div style={styles.mainContent}>
        <h1 style={styles.title}>Welcome to Poker App</h1>
        <p style={styles.subtitle}>Join the table and test your skills!</p>
        <button style={styles.button} onClick={() => navigate('/lobby')}>
          Enter Game Lobby
        </button>
      </div>
    </div>
  );
};

const styles = {
  container: {
    height: '100vh',
    backgroundImage: 'url("https://images.pexels.com/photos/1796794/pexels-photo-1796794.jpeg?cs=srgb&dl=pexels-ken123films-1796794.jpg&fm=jpg")', // Updated image URL
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    display: 'flex',
    flexDirection: 'column',
  },
  mainContent: {
    textAlign: 'center',
    marginTop: 'auto',
    marginBottom: 'auto',
    color: '#fff',
    textShadow: '0 2px 4px rgba(0, 0, 0, 0.8)',
  },
  title: {
    fontSize: '3rem',
    fontWeight: 'bold',
    marginBottom: '10px',
  },
  subtitle: {
    fontSize: '1.5rem',
    marginBottom: '20px',
  },
  button: {
    padding: '15px 30px',
    fontSize: '1.2rem',
    fontWeight: 'bold',
    color: '#fff',
    backgroundColor: '#007bff',
    border: 'none',
    borderRadius: '5px',
    cursor: 'pointer',
    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.2)',
    transition: 'transform 0.2s, background-color 0.2s',
  },
  buttonHover: {
    backgroundColor: '#0056b3',
    transform: 'scale(1.05)',
  },
};

export default Home;