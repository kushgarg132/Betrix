import React, { useEffect, useState, useContext } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import './PokerTable.css';

const MAX_PLAYERS = 6;

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

const PokerTable = () => {
  const { gameId } = useParams();
  const [game, setGame] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [stompClient, setStompClient] = useState(null);
  const { user } = useContext(AuthContext);

  useEffect(() => {
    axios
      .get(`/game/${gameId}`)
      .then((response) => {
        setGame(response.data);
        setLoading(false);
      })
      .catch((error) => {
        console.error('Error fetching game:', error);
        setError('Failed to load game data.');
        setLoading(false);
      });

    const socket = new SockJS('http://localhost:8080/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => console.log(str),
      onConnect: () => {
        console.log('Connected to WebSocket');
        client.subscribe(`/topic/game/${gameId}`, (message) => {
          const updatedGame = JSON.parse(message.body);
          setGame(updatedGame);
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame);
        setError('WebSocket connection error.');
      },
    });

    client.activate();
    setStompClient(client);

    return () => {
      if (client) {
        client.deactivate();
      }
    };
  }, [gameId]);

  const placeBet = (amount) => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/bet`,
        // body: JSON.stringify({ playerId , amount }),
      });
    }
  };

  const fold = () => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/fold`,
        body: JSON.stringify({}),
      });
    }
  };

  const joinTable = () => {
    axios
      .post(`/game/${gameId}/join`)
      .then((response) => {
        setGame(response.data);
      })
      .catch((error) => {
        console.error('Error joining game:', error);
      });
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  const currentPlayer = game.players[game.currentPlayerIndex];

  return (
    <div className="poker-table">
      <h1 className="table-title">Poker Table - Game ID: {game.id}</h1>
      <div className="table-area">
        <div className="community-cards">
          <h2>Community Cards</h2>
          <div className="cards">
            {game.communityCards.map((card, index) => (
              <div key={index} className={`card ${card.suit.toLowerCase()}`}>
                <div className="card-top">
                  <span>{card.rank}</span>
                  <span>{getSuitSymbol(card.suit)}</span>
                </div>
                <div className="card-center">
                  <span>{getSuitSymbol(card.suit)}</span>
                </div>
                <div className="card-bottom">
                  <span>{card.rank}</span>
                  <span>{getSuitSymbol(card.suit)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="pot-info">
          <h3>Pot: ${game.pot.toFixed(2)}</h3>
        </div>
        <div className="players">
          {game.players.map((player, index) => (
            <div
              key={index}
              className={`player ${index === game.currentPlayerIndex ? 'current-player' : ''}`}
            >
              <h3>{player.username}</h3>
              <p>Chips: ${player.chips.toFixed(2)}</p>
              <p>Last Action: {game.lastActions[player._id] || 'None'}</p>
              <p>Bet: ${game.currentBettingRound.playerBets[player._id] || 0}</p>
              <div className="player-cards">
                {player.hand.map((card, idx) => (
                  <div key={idx} className={`card ${card.suit.toLowerCase()}`}>
                    <div className="card-top">
                      <span>{card.rank}</span>
                      <span>{getSuitSymbol(card.suit)}</span>
                    </div>
                    <div className="card-center">
                      <span>{getSuitSymbol(card.suit)}</span>
                    </div>
                    <div className="card-bottom">
                      <span>{card.rank}</span>
                      <span>{getSuitSymbol(card.suit)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
          {Array.from({ length: MAX_PLAYERS - game.players.length }).map((_, index) => (
            <div
              key={`placeholder-${index}`}
              className="player placeholder"
              onClick={joinTable}
            >
              <h3>Waiting for Player...</h3>
            </div>
          ))}
        </div>
      </div>
      <div className="betting-controls">
        <button onClick={() => placeBet(10)} className="bet-button">
          Bet 10
        </button>
        <button onClick={() => placeBet(0)} className="check-button">
          Check
        </button>
        <button onClick={fold} className="fold-button">
          Fold
        </button>
      </div>
    </div>
  );
};

export default PokerTable;