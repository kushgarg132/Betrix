import React from 'react';

const PlayerCard = ({ player, isCurrentUser, isCurrentTurn, positionClass, getSuitSymbol }) => (
  <div className={`player ${positionClass} ${isCurrentTurn ? 'current-player' : ''}`} style={{
    border: isCurrentTurn ? '2px solid gold' : '1px solid #ccc',
    borderRadius: '10px',
    padding: '10px',
    backgroundColor: isCurrentUser ? '#f0f8ff' : '#fff',
    boxShadow: '0 4px 8px rgba(0, 0, 0, 0.1)',
    textAlign: 'center',
    transition: 'transform 0.2s',
    transform: isCurrentTurn ? 'scale(1.05)' : 'scale(1)',
  }}>
    <h3 style={{ margin: '10px 0', fontSize: '1.2rem', color: '#333' }}>{player.username}</h3>
    <p style={{ margin: '5px 0', fontSize: '1rem', color: '#555' }}>Chips: ${player.chips?.toFixed(2)}</p>
    <p style={{ margin: '5px 0', fontSize: '0.9rem', color: '#777' }}>Last Action: {player.lastAction || 'None'}</p>
    <p style={{ margin: '5px 0', fontSize: '0.9rem', color: '#777' }}>Bet: ${player.currentBet || 0}</p>
    <div className="player-cards" style={{ display: 'flex', justifyContent: 'center', gap: '5px', marginTop: '10px' }}>
      {player.hand.map((card, idx) => (
        <div key={idx} className={`card ${card.suit.toLowerCase()}`} style={{
          width: '40px',
          height: '60px',
          backgroundColor: '#fff',
          border: '1px solid #ccc',
          borderRadius: '5px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '5px',
          boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        }}>
          <div className="card-top" style={{ fontSize: '0.8rem', fontWeight: 'bold' }}>
            <span>{card.rank}</span>
            <span>{getSuitSymbol(card.suit)}</span>
          </div>
          <div className="card-center" style={{ fontSize: '1rem', fontWeight: 'bold' }}>
            <span>{getSuitSymbol(card.suit)}</span>
          </div>
          <div className="card-bottom" style={{ fontSize: '0.8rem', fontWeight: 'bold' }}>
            <span>{card.rank}</span>
            <span>{getSuitSymbol(card.suit)}</span>
          </div>
        </div>
      ))}
    </div>
  </div>
);

export default PlayerCard;
