import React from 'react';

const PlayerCard = ({ player, isCurrentUser, isCurrentTurn, positionClass, getSuitSymbol }) => (
  <div className={`player ${positionClass} ${isCurrentTurn ? 'current-player' : ''}`} style={{
    border: isCurrentTurn ? '3px solid gold' : '2px solid #ccc',
    borderRadius: '12px',
    padding: '15px',
    backgroundColor: isCurrentUser ? '#f0f8ff' : '#fff',
    boxShadow: '0 6px 12px rgba(0, 0, 0, 0.15)',
    textAlign: 'center',
    transition: 'transform 0.2s',
    transform: isCurrentTurn ? 'scale(1.08)' : 'scale(1.02)',
    minWidth: '180px',
  }}>
    <h3 style={{ margin: '10px 0', fontSize: '1.4rem', fontWeight: 'bold', color: '#333' }}>{player.username}</h3>
    <p style={{ margin: '6px 0', fontSize: '1.2rem', color: '#555' }}>Chips: ${player.chips?.toFixed(2)}</p>
    <p style={{ margin: '6px 0', fontSize: '1.1rem', color: '#777' }}>Last Action: {player.lastAction || 'None'}</p>
    <p style={{ margin: '6px 0', fontSize: '1.1rem', color: '#777' }}>Bet: ${player.currentBet || 0}</p>
    <div className="player-cards" style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '12px' }}>
      {player.hand.map((card, idx) => (
        <div key={idx} className={`card ${card.suit.toLowerCase()}`} style={{
          width: '60px',
          height: '90px',
          backgroundColor: '#fff',
          border: '2px solid #ddd',
          borderRadius: '8px',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '6px',
          boxShadow: '0 4px 8px rgba(0, 0, 0, 0.15)',
        }}>
          <div className="card-top" style={{ fontSize: '1.1rem', fontWeight: 'bold' }}>
            <span>{card.rank}</span>
            <span>{getSuitSymbol(card.suit)}</span>
          </div>
          <div className="card-center" style={{ fontSize: '1.6rem', fontWeight: 'bold' }}>
            <span>{getSuitSymbol(card.suit)}</span>
          </div>
          <div className="card-bottom" style={{ fontSize: '1.1rem', fontWeight: 'bold' }}>
            <span>{card.rank}</span>
            <span>{getSuitSymbol(card.suit)}</span>
          </div>
        </div>
      ))}
    </div>
  </div>
);

export default PlayerCard;
