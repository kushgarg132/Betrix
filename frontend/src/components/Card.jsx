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
  const classNames = ['card'];
  if (cardContext) {
    classNames.push(`${cardContext}-card`);
  }

  if (hidden) {
    classNames.push('hidden');
    return <img src="/cards/back.svg" alt="Card Back" className={classNames.join(' ')} />;
  }

  const { rank, suit } = card;
  const cardRank = getCardRank(rank);
  const cardImage = `/cards/${cardRank}${suit.charAt(0).toUpperCase()}.svg`;

  return (
    <img 
      src={cardImage} 
      alt={`${cardRank} of ${suit}`} 
      className={classNames.join(' ')}
    />
  );
};

export default Card; 