import React, { memo } from 'react';
import './Table.css';
import Card from './Card';
import PotDisplay from './PotDisplay';

// Using memo to prevent unnecessary re-renders
const Table = memo(({ isMyTurn, game, communityCards, animatingChips }) => {
  // Calculate how many placeholder cards we need (always show 5 total elements)
  const placeholderCount = Math.max(0, 5 - communityCards.length);
  
  // Get ALL winning cards (combine highCards and bestFiveCards)
  let winningCards = [];
  if (game.winners && game.winners.length > 0 && game.winners[0].bestHand) {
    // Get highCards (important cards)
    const highCards = game.winners[0].bestHand.highCards || [];
    
    // Combine them (we'll remove duplicates later)
    winningCards = [...highCards];
  }
  
  // Create a map of winning cards for O(1) lookup
  const winningCardMap = {};
  if (winningCards.length > 0) {
    winningCards.forEach(card => {
      winningCardMap[`${card.rank}-${card.suit}`] = true;
    });
  }
  
  // Determine if there's a winner to display
  const hasWinner = game.winners && game.winners.length > 0 && game.winners[0].bestHand;
  
  // Format the hand rank for display (e.g., "THREE_OF_A_KIND" -> "Three of a kind")
  const formatHandRank = (rank) => {
    if (!rank) return '';
    
    // Create more compact display names
    const displayNames = {
      'HIGH_CARD': 'High Card',
      'ONE_PAIR': 'Pair',
      'TWO_PAIR': 'Two Pair',
      'THREE_OF_A_KIND': 'Three Kind',
      'STRAIGHT': 'Straight',
      'FLUSH': 'Flush',
      'FULL_HOUSE': 'Full House',
      'FOUR_OF_A_KIND': 'Four Kind',
      'STRAIGHT_FLUSH': 'Str. Flush',
      'ROYAL_FLUSH': 'Royal Flush'
    };
    
    return displayNames[rank] || rank.replace(/_/g, ' ');
  };
  
  return (
    <div className={`table-felt ${isMyTurn ? 'my-turn' : ''}`}>
      <div className="wood-border">
        <div className="circular-table">
          {/* Ultra-compact winning hand display */}
          {hasWinner && (
            <div className="winning-hand-display">
              <span className="winner-name">{game.winners[0].username}</span>
              <span className="hand-type">
                {formatHandRank(game.winners[0].bestHand.rank)}
              </span>
            </div>
          )}
          
          {/* Pot display positioned in the middle */}
          <PotDisplay game={game} />

          {/* Community cards centered in the table */}
          <div className="community-cards">
            {/* Show actual community cards */}
            {communityCards.map((card, index) => {
              // Check if this card is part of the winning hand
              const isWinningCard = winningCardMap[`${card.rank}-${card.suit}`];
              
              return (
                <Card 
                  key={`card-${index}-${card.suit}-${card.rank}`} 
                  card={card} 
                  cardContext={isWinningCard ? "winning-community" : "community"} 
                />
              );
            })}
            
            {/* Fill empty card spots with placeholders */}
            {placeholderCount > 0 && 
              Array(placeholderCount).fill(null).map((_, i) => (
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