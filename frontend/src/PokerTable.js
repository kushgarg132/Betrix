import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import './PokerTable.css';

const PokerTable = () => {
  const { gameId } = useParams();
  const [game, setGame] = useState(null);

  useEffect(() => {
    fetch(`/api/game/${gameId}`)
      .then((response) => response.json())
      .then((data) => setGame(data))
      .catch((error) => console.error('Error fetching game:', error));
  }, [gameId]);

  const placeBet = (amount) => {
    fetch(`/api/game/${gameId}/bet`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: 'player1', amount }),
    })
      .then((response) => response.json())
      .then((updatedGame) => setGame(updatedGame))
      .catch((error) => console.error('Error placing bet:', error));
  };

  if (!game) return <div>Loading...</div>;

  return (
    <div className="poker-table">
      <h1>Game ID: {game.id}</h1>
      <div className="dealer-area">
        <h2>Community Cards</h2>
        <div className="dealer-cards">
          {game.communityCards.map((card, index) => (
            <div key={index} className="card">
              {card.rank} of {card.suit}
            </div>
          ))}
        </div>
      </div>
      <div className="player-area">
        <h2>Your Cards</h2>
        <div className="player-cards">
          {game.players[0].hand.map((card, index) => (
            <div key={index} className="card">
              {card.rank} of {card.suit}
            </div>
          ))}
        </div>
      </div>
      <div className="betting-area">
        <button onClick={() => placeBet(10)}>Bet 10</button>
        <button onClick={() => placeBet(0)}>Check</button>
        <button onClick={() => placeBet(-1)}>Fold</button>
      </div>
    </div>
  );
};

export default PokerTable;