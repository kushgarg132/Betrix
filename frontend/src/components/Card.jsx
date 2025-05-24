import React from 'react';
import './Card.css';

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

const Card = ({ card, hidden, cardContext }) => {
  // Base card classes
  const cardClasses = ['card'];
  
  // Add context-specific classes
  if (cardContext) {
    cardClasses.push(`card-${cardContext}`);
  }
  
  if (hidden) {
    cardClasses.push('hidden');
    return (
      <div 
        className={cardClasses.join(' ')} 
        data-context={cardContext || 'default'}
      >
        <img 
          src="/cards/back.svg" 
          alt="Card Back" 
          className="card-img"
        />
      </div>
    );
  }

  const { rank, suit } = card;
  const cardRank = getCardRank(rank);
  const cardImage = `/cards/${cardRank}${suit.charAt(0).toUpperCase()}.svg`;
  
  // Add suit class for styling
  cardClasses.push(suit.toLowerCase());

  return (
    <div 
      className={cardClasses.join(' ')}
      data-context={cardContext || 'default'}
    >
      <img 
        src={cardImage} 
        alt={`${cardRank} of ${suit}`} 
        className="card-img"
      />
    </div>
  );
};

export default Card; 