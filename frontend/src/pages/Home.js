import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

// Add keyframes for animations
const styleSheet = document.styleSheets[0];
const animations = `
@keyframes gradientShift {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
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

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

@keyframes shimmer {
  0% {
    background-position: -1000px 0;
  }
  100% {
    background-position: 1000px 0;
  }
}
`;

try {
  styleSheet.insertRule(animations, styleSheet.cssRules.length);
} catch (e) {
  // Animations already exist or error inserting
}

const Home = () => {
  const navigate = useNavigate();
  const [mounted, setMounted] = useState(false);
  const [hoveredButton, setHoveredButton] = useState(null);
  const [hoveredCard, setHoveredCard] = useState(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  const getButtonStyle = (buttonType) => {
    const baseStyle = buttonType === 'primary' ? styles.primaryButton : styles.secondaryButton;
    const isHovered = hoveredButton === buttonType;

    return {
      ...baseStyle,
      transform: isHovered ? 'translateY(-3px) scale(1.05)' : 'translateY(0) scale(1)',
      boxShadow: isHovered
        ? (buttonType === 'primary'
          ? '0 8px 25px rgba(0, 170, 255, 0.6), 0 0 40px rgba(0, 170, 255, 0.3)'
          : '0 8px 25px rgba(0, 255, 204, 0.4), 0 0 40px rgba(0, 255, 204, 0.2)')
        : baseStyle.boxShadow,
    };
  };

  const getCardStyle = (index) => {
    const isHovered = hoveredCard === index;

    return {
      ...styles.featureCard,
      transform: isHovered ? 'translateY(-10px) scale(1.05)' : 'translateY(0) scale(1)',
      boxShadow: isHovered
        ? '0 15px 40px rgba(0, 255, 204, 0.3), 0 0 20px rgba(0, 170, 255, 0.2)'
        : styles.featureCard.boxShadow,
    };
  };

  return (
    <div style={styles.container}>
      {/* Floating poker chips */}
      <div style={styles.floatingChip1}>♠</div>
      <div style={styles.floatingChip2}>♥</div>
      <div style={styles.floatingChip3}>♦</div>
      <div style={styles.floatingChip4}>♣</div>

      <div style={styles.overlay}>
        <div style={styles.mainContent}>
          <h1 style={{
            ...styles.title,
            animation: mounted ? 'fadeInDown 0.8s ease-out' : 'none',
          }}>
            <span style={styles.titleHighlight}>BETRIX</span> POKER
          </h1>
          <p style={{
            ...styles.subtitle,
            animation: mounted ? 'fadeInUp 0.8s ease-out 0.2s backwards' : 'none',
          }}>
            Experience the thrill of poker in a modern digital environment
          </p>

          <div style={{
            ...styles.buttonContainer,
            animation: mounted ? 'scaleIn 0.6s ease-out 0.4s backwards' : 'none',
          }}>
            <button
              style={getButtonStyle('primary')}
              onClick={() => navigate('/lobby')}
              onMouseEnter={() => setHoveredButton('primary')}
              onMouseLeave={() => setHoveredButton(null)}
            >
              Enter Game Lobby
            </button>
            <button
              style={getButtonStyle('secondary')}
              onClick={() => navigate('/profile')}
              onMouseEnter={() => setHoveredButton('secondary')}
              onMouseLeave={() => setHoveredButton(null)}
            >
              View Profile
            </button>
          </div>

          <div style={styles.featureGrid}>
            <div
              style={{
                ...getCardStyle(0),
                animation: mounted ? 'fadeInUp 0.6s ease-out 0.6s backwards' : 'none',
              }}
              onMouseEnter={() => setHoveredCard(0)}
              onMouseLeave={() => setHoveredCard(null)}
            >
              <div style={styles.featureIcon}>💰</div>
              <h3 style={styles.featureTitle}>Real-time Play</h3>
              <p style={styles.featureText}>Compete in real-time with players worldwide</p>
            </div>
            <div
              style={{
                ...getCardStyle(1),
                animation: mounted ? 'fadeInUp 0.6s ease-out 0.7s backwards' : 'none',
              }}
              onMouseEnter={() => setHoveredCard(1)}
              onMouseLeave={() => setHoveredCard(null)}
            >
              <div style={styles.featureIcon}>🏆</div>
              <h3 style={styles.featureTitle}>Tournaments</h3>
              <p style={styles.featureText}>Join tournaments and win big prizes</p>
            </div>
            <div
              style={{
                ...getCardStyle(2),
                animation: mounted ? 'fadeInUp 0.6s ease-out 0.8s backwards' : 'none',
              }}
              onMouseEnter={() => setHoveredCard(2)}
              onMouseLeave={() => setHoveredCard(null)}
            >
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
    background: 'linear-gradient(-45deg, #0f0c29, #302b63, #24243e, #0f2027, #203a43, #2c5364)',
    backgroundSize: '400% 400%',
    animation: 'gradientShift 15s ease infinite',
    position: 'relative',
    paddingBottom: 'env(safe-area-inset-bottom, 0)',
  },
  overlay: {
    minHeight: '100vh',
    backgroundColor: 'rgba(15, 12, 41, 0.4)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '20px',
    paddingBottom: 'calc(20px + env(safe-area-inset-bottom, 0))',
  },
  mainContent: {
    textAlign: 'center',
    maxWidth: '1000px',
    width: '100%',
    paddingBottom: '20px',
    position: 'relative',
    zIndex: 2,
  },
  title: {
    fontSize: '3.5rem',
    fontWeight: 'bold',
    marginBottom: '20px',
    color: '#fff',
    textShadow: '0 3px 6px rgba(0, 0, 0, 0.5)',
    letterSpacing: '2px',
  },
  titleHighlight: {
    color: '#00aaff',
    textShadow: '0 0 10px rgba(0, 170, 255, 0.5)',
    background: 'linear-gradient(90deg, #00aaff, #00ffcc, #00aaff)',
    backgroundSize: '200% auto',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
    backgroundClip: 'text',
    animation: 'shimmer 3s linear infinite',
  },
  subtitle: {
    fontSize: '1.5rem',
    marginBottom: '40px',
    color: '#e0e0e0',
    maxWidth: '800px',
    margin: '0 auto 40px',
  },
  buttonContainer: {
    display: 'flex',
    justifyContent: 'center',
    gap: '20px',
    marginBottom: '50px',
    flexWrap: 'wrap',
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
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
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
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  },
  featureGrid: {
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
    gap: '30px',
    marginTop: '20px',
  },
  featureCard: {
    backgroundColor: 'rgba(22, 33, 62, 0.7)',
    padding: '30px 20px',
    borderRadius: '15px',
    width: '250px',
    boxShadow: '0 8px 20px rgba(0, 0, 0, 0.2)',
    backdropFilter: 'blur(5px)',
    transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
    cursor: 'pointer',
    border: '1px solid rgba(0, 170, 255, 0.1)',
  },
  featureIcon: {
    fontSize: '2.5rem',
    marginBottom: '15px',
    filter: 'drop-shadow(0 0 10px rgba(0, 255, 204, 0.3))',
  },
  featureTitle: {
    fontSize: '1.3rem',
    color: '#00ffcc',
    marginBottom: '10px',
    fontWeight: 'bold',
  },
  featureText: {
    fontSize: '1rem',
    color: '#e0e0e0',
    lineHeight: '1.5',
  },
  // Floating poker suit symbols
  floatingChip1: {
    position: 'absolute',
    top: '15%',
    left: '10%',
    fontSize: '3rem',
    color: 'rgba(0, 170, 255, 0.2)',
    animation: 'float 6s ease-in-out infinite',
    zIndex: 1,
    textShadow: '0 0 20px rgba(0, 170, 255, 0.3)',
  },
  floatingChip2: {
    position: 'absolute',
    top: '25%',
    right: '15%',
    fontSize: '2.5rem',
    color: 'rgba(255, 0, 100, 0.2)',
    animation: 'float 7s ease-in-out infinite 1s',
    zIndex: 1,
    textShadow: '0 0 20px rgba(255, 0, 100, 0.3)',
  },
  floatingChip3: {
    position: 'absolute',
    bottom: '20%',
    left: '15%',
    fontSize: '2.8rem',
    color: 'rgba(255, 0, 100, 0.2)',
    animation: 'float 8s ease-in-out infinite 2s',
    zIndex: 1,
    textShadow: '0 0 20px rgba(255, 0, 100, 0.3)',
  },
  floatingChip4: {
    position: 'absolute',
    bottom: '30%',
    right: '10%',
    fontSize: '3.2rem',
    color: 'rgba(0, 170, 255, 0.2)',
    animation: 'float 9s ease-in-out infinite 3s',
    zIndex: 1,
    textShadow: '0 0 20px rgba(0, 170, 255, 0.3)',
  },
};

export default Home;