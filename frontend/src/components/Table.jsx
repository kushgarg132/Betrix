import React, { memo } from 'react';
import './Table.css';
import Card from './Card';

const Table = memo(({ isMyTurn, game, communityCards }) => {
  const placeholderCount = Math.max(0, 5 - (communityCards?.length || 0));

  // Winning card lookup
  const winningCardMap = {};
  if (game?.winners?.length > 0 && game.winners[0].bestHand) {
    (game.winners[0].bestHand.highCards || []).forEach(card => {
      winningCardMap[`${card.rank}-${card.suit}`] = true;
    });
  }

  const hasWinner = game?.winners?.length > 0 && game.winners[0]?.bestHand;

  // Generate chip stack visualization based on pot size
  const potAmount = game?.pot || 0;
  const chipCounts = {
    green: Math.min(4, Math.floor(potAmount / 1000) || 1),
    red: Math.min(3, Math.floor(potAmount / 500) || 0),
    blue: Math.min(3, Math.floor(potAmount / 2000) || 0),
    black: Math.min(2, Math.floor(potAmount / 5000) || 0),
  };

  return (
    <div className="table-felt">
      <div className="table-rim">
        <div className="table-surface">

          {/* Chip Stacks */}
          <div className="chip-stacks">
            {chipCounts.green > 0 && (
              <div className="chip-stack-col">
                {[...Array(chipCounts.green)].map((_, i) => <div key={`g${i}`} className="chip chip-green" />)}
              </div>
            )}
            {chipCounts.red > 0 && (
              <div className="chip-stack-col">
                {[...Array(chipCounts.red)].map((_, i) => <div key={`r${i}`} className="chip chip-red" />)}
              </div>
            )}
            {chipCounts.blue > 0 && (
              <div className="chip-stack-col">
                {[...Array(chipCounts.blue)].map((_, i) => <div key={`b${i}`} className="chip chip-blue" />)}
              </div>
            )}
            {chipCounts.black > 0 && (
              <div className="chip-stack-col">
                {[...Array(chipCounts.black)].map((_, i) => <div key={`k${i}`} className="chip chip-black" />)}
              </div>
            )}
          </div>

          {/* Pot Display */}
          <div className="pot-display">
            <div className="pot-label">POT</div>
            <div className="pot-amount">${potAmount.toLocaleString()}</div>
          </div>

          {/* Community Cards */}
          <div className="community-cards">
            {communityCards.map((card, index) => (
              <Card
                key={`card-${index}`}
                card={card}
                cardContext={winningCardMap[`${card.rank}-${card.suit}`] ? "winning-community" : "community"}
              />
            ))}
            {Array(placeholderCount).fill(null).map((_, i) => (
              <div key={`empty-${i}`} className="card-placeholder" />
            ))}
          </div>

          {/* Winner Overlay */}
          {hasWinner && (
            <div className="winning-hand-display">
              <span className="winner-name">{game.winners[0].username}</span>
              <span className="hand-rank">{game.winners[0].bestHand.rank.replace(/_/g, ' ')}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
});

Table.displayName = 'Table';
export default Table;