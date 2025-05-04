import React from 'react';
import './Card.css';

const getSuitSymbol = (suit) => {
  switch (suit.toLowerCase()) {
    case 'hearts':
      return '♥';
    case 'diamonds':
      return '♦';
    case 'clubs':
      return '♣';
    case 'spades':
      return '♠';
    default:
      return '';
  }
};

// Function to get card face for court cards
const getCardFace = (rank, suit) => {
  if (['J', 'Q', 'K', 'A'].includes(rank)) {
    return (
      <div className="card-face">
        {rank === 'K' && <div className="card-king"></div>}
        {rank === 'Q' && <div className="card-queen"></div>}
        {rank === 'J' && <div className="card-jack"></div>}
        {rank === 'A' && <div className="card-ace">{getSuitSymbol(suit)}</div>}
      </div>
    );
  }
  return null;
};

const Card = ({ card, hidden }) => {
  if (hidden) {
    return <div className="card hidden"></div>;
  }

  const { rank, suit } = card;

  return (
    <div className={`card realistic ${suit.toLowerCase()}`}>
      <div className="card-top">
        <span>{rank}</span>
        <span>{getSuitSymbol(suit)}</span>
      </div>
      {getCardFace(rank, suit)}
      <div className="card-center">
        {rank !== 'A' && rank !== 'K' && rank !== 'Q' && rank !== 'J' && getSuitSymbol(suit)}
      </div>
      <div className="card-bottom">
        <span>{getSuitSymbol(suit)}</span>
        <span>{rank}</span>
      </div>
    </div>
  );
};

export { getSuitSymbol };
export default Card; 