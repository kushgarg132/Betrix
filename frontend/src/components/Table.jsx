import React, { memo } from 'react';
import './Table.css';
import Card from './Card';

const Table = memo(({ isMyTurn, game, communityCards }) => {
  const placeholderCount = Math.max(0, 5 - communityCards.length);

  // Winning card logic
  const winningCardMap = {};
  if (game.winners && game.winners.length > 0 && game.winners[0].bestHand) {
    const highCards = game.winners[0].bestHand.highCards || [];
    highCards.forEach(card => {
      winningCardMap[`${card.rank}-${card.suit}`] = true;
    });
  }

  const hasWinner = game.winners && game.winners.length > 0 && game.winners[0].bestHand;

  // Calculate Call Amount for the current player
  const currentUsername = game.players?.[game.currentPlayerIndex]?.username;
  const currentPlayerBet = game.currentBettingRound?.playerBets?.[currentUsername] || 0;
  const callAmount = Math.max(0, (game.currentBet || 0) - currentPlayerBet);

  return (
    <div className={`table-felt ${isMyTurn ? 'my-turn' : ''}`}>
      {/* Metallic Rim Accents (8 brass lights) */}
      <div className="metallic-accents">
        {[...Array(8)].map((_, i) => (
          <div key={`accent-${i}`} className="accent-light" />
        ))}
      </div>

      <div className="wood-border">
        <div className="circular-table">
          {/* Pot Display */}
          <div className="pot-display">
            <span>POT:</span>
            <span className="pot-amount">${game.pot?.toLocaleString() || 0}</span>
          </div>

          {/* Winning Hand Overlay */}
          {hasWinner && (
            <div className="winning-hand-display">
              <span className="winner-name">{game.winners[0].username}</span>
              <span className="hand-rank">{game.winners[0].bestHand.rank.replace(/_/g, ' ')}</span>
            </div>
          )}

          {/* Glass Community Card Tray */}
          <div className="community-cards-tray">
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
            <div className="tray-label">Board</div>
          </div>

          {/* In-Table Bet Info Labels (Felt UI) */}
          <div className="felt-info">
            <div className="felt-stat">
              <span className="stat-label">Current Bet</span>
              <span className="stat-value">${game.currentBet?.toLocaleString() || 0}</span>
            </div>
            <div className="felt-stat">
              <span className="stat-label">Call</span>
              <span className="stat-value">${callAmount.toLocaleString()}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
});

Table.displayName = 'Table';
export default Table;