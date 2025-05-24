import React, { memo } from 'react';
import './Table.css';
import Card from './Card';
import PotDisplay from './PotDisplay';

// Using memo to prevent unnecessary re-renders
const Table = memo(({ isMyTurn, game, communityCards, animatingChips }) => {
  return (
    <div className={`table-felt ${isMyTurn ? 'my-turn' : ''}`}>
      <div className="wood-border">
        <div className="circular-table">
          <PotDisplay game={game} />

          <div className="community-cards">
            {communityCards.map((card, index) => (
              <Card key={`card-${index}-${card.suit}-${card.rank}`} card={card} cardContext="table" />
            ))}
            
            {/* Fill empty card spots for better layout */}
            {communityCards.length < 5 && 
              Array(5 - communityCards.length).fill(null).map((_, i) => (
                <div key={`empty-${i}`} className="card-placeholder"></div>
              ))
            }
          </div>

          {/* Animate chips flying to pot when someone bets */}
          {animatingChips && (
            <div className="animated-chips">
              {[...Array(4)].map((_, i) => (
                <div 
                  key={i} 
                  className="flying-chip"
                  style={{
                    animationDelay: `${i * 0.08}s`,
                    animationDuration: `${0.6 + Math.random() * 0.4}s`,
                    left: `${30 + Math.random() * 40}%`
                  }}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
});

Table.displayName = 'Table'; // For debugging in React DevTools

export default Table; 