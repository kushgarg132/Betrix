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

const getCardRank = (rank) => {
  switch (rank) {
    case "TWO": return '2';
    case "THREE": return '3';
    case "FOUR": return '4';
    case "FIVE": return '5';
    case "SIX": return '6';
    case "SEVEN": return '7';
    case "EIGHT": return '8';
    case "NINE": return '9';
    case "TEN": return '10';
    case "JACK": return 'J';
    case "QUEEN": return 'Q';
    case "KING": return 'K';
    case "ACE": return 'A';
    default: return '';
  }
};

const Card = ({ card, hidden }) => {
  if (hidden) {
    return <img src="/cards/back.svg" alt="Card Back" className="card hidden" />;
  }

  const { rank, suit } = card;
  const cardRank = getCardRank(rank);
  const cardImage = `/cards/${cardRank}${suit.charAt(0).toUpperCase()}.svg`;

  return (
    <img 
      src={cardImage} 
      alt={`${cardRank} of ${suit}`} 
      className="card"
    />
  );
};

export { getSuitSymbol };
export default Card; 