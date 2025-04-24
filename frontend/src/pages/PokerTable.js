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

const initializeSocketConnection = (gameId, playerId, setUpdateActions, setCards, setError) => {
  const socket = new SockJS('http://192.168.29.195:8080/ws');
  const client = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    onConnect: () => {
      console.log('Connected to WebSocket');
      client.subscribe(`/topic/game/${gameId}`, (message) => {
        const updatedGame = JSON.parse(message.body);
        setUpdateActions(updatedGame);
      });
      client.subscribe(`/topic/game/${gameId}/${playerId}`, (message) => {
        const cards = JSON.parse(message.body);
        setCards(cards);
        console.log('Received cards:', cards);
      });
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame);
      setError('WebSocket connection error.');
    },
  });

  client.activate();
  return client;
};

const PokerTable = () => {
  const { gameId } = useParams("gameId");
  const [game, setGame] = useState(null);
  const [updateAction, setUpdateActions] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [stompClient, setStompClient] = useState(null);
  const { user } = useContext(AuthContext);
  const [cards, setCards] = useState([]);
  const [playerIdx, setPlayerIdx] = useState();
  
  const gameStatus = {
    GAME_STARTED: 'GAME_STARTED',
    PLAYER_JOINED: 'PLAYER_JOINED',
    PLAYER_LEFT: 'PLAYER_LEFT',
    PLAYER_TURN: 'PLAYER_TURN',
    PLAYER_BET: 'PLAYER_BET',
    PLAYER_FOLDED: 'PLAYER_FOLDED',
    CARDS_DEALT: 'CARDS_DEALT',
    ROUND_STARTED: 'ROUND_STARTED',
    ROUND_COMPLETE: 'ROUND_COMPLETE',
    GAME_ENDED: 'GAME_ENDED'
  };

  useEffect(() => {
    axios
      .post(`/game/${gameId}/join`)
      .then((response) => {
        setGame(response.data);
        setLoading(false);
        console.log("User", user);
        const playerIndex = response.data.players.findIndex((p) => p.username === user.username);
        setPlayerIdx(playerIndex);

        const player = response.data.players[playerIndex];
        
        const client = initializeSocketConnection(gameId, player.id, setUpdateActions, setCards, setError);
        setStompClient(client);
      })
      .catch((error) => {
        console.error('Error fetching game:', error);
        setError('Failed to load game data.');
        setLoading(false);
      });

    return () => {
      if (stompClient && stompClient.connected) {
        stompClient.publish({
          destination: `/app/game/${gameId}/leave`,
          body: JSON.stringify({ playerId: game.players[playerIdx]?.id }), // Use correct player ID
        });
        stompClient.deactivate();
      }
    };
  }, [gameId]);

  useEffect(() => {
    if (updateAction) {
      console.log('Update action:', updateAction);
      switch (updateAction.type) {
        case gameStatus.GAME_STARTED:
          // console.log('Game started:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            ...updateAction.payload,
            players: [...prevGame.players],
          }));
          break;

        case gameStatus.PLAYER_JOINED:
          // console.log('Player joined:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            players: [...prevGame.players, updateAction.payload],
          }));
          break;

        case gameStatus.PLAYER_LEFT:
          // console.log('Player left:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            players: prevGame.players.filter(
              (player) => player._id !== updateAction.payload
            ),
          }));
          break;

        case gameStatus.PLAYER_TURN:
          // console.log(`It's player ${updateAction.payload}'s turn`);
          setGame((prevGame) => ({
            ...prevGame,
            currentPlayerIndex: updateAction.payload,
          }));
          break;

        case gameStatus.PLAYER_BET:
          // console.log('Player bet:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            pot: prevGame.pot + updateAction.payload.amount,
            currentBettingRound: {
              ...prevGame.currentBettingRound,
              playerBets: {
                ...prevGame.currentBettingRound.playerBets,
                [updateAction.payload.playerId]: updateAction.payload.amount,
              },
            },
          }));
          break;

        case gameStatus.PLAYER_FOLDED:
          // console.log('Player folded:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            players: prevGame.players.map((player) =>
              player._id === updateAction.payload
                ? { ...player, folded: true }
                : player
            ),
          }));
          break;

        case gameStatus.CARDS_DEALT:
          // console.log('Cards dealt:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            communityCards: updateAction.payload.communityCards,
            players: prevGame.players.map((player) => ({
              ...player,
              hand: updateAction.payload.playerHands[player._id] || player.hand,
            })),
          }));
          break;

        case gameStatus.ROUND_STARTED:
          // console.log('Round started:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            ...updateAction.payload,
            players: [...prevGame.players],
          }));
          break;

        case gameStatus.ROUND_COMPLETE:
          // console.log('Round complete:', updateAction.payload);
          setGame((prevGame) => ({
            ...prevGame,
            pot: updateAction.payload.pot,
            communityCards: updateAction.payload.communityCards,
          }));
          break;

        case gameStatus.GAME_ENDED:
          // console.log('Game ended:', updateAction.payload);
          alert('Game has ended!');
          setGame((prevGame) => ({
            ...prevGame,
            status: 'ENDED',
            winner: updateAction.payload.winner,
          }));
          break;

        default:
          console.warn('Unhandled update action:', updateAction);
      }
      setUpdateActions(null); // Reset updateAction after handling
      console.log('Game state updated:', game);
    }
  }, [updateAction]);



  const joinTable = () => {
    axios
      .post(`/game/${gameId}/join`)
      .then((response) => {
        console.log('Joined game:', response.data);
        setGame(response.data);
      })
      .catch((error) => {
        console.error('Error joining game:', error);
      });
  };

  const placeBet = (amount) => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: user._id,
          amount,
          actionType: 'BET',
        }),
      });
    }
  };

  const fold = () => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: user._id,
          action: 'FOLD'
        }),
      });
    }
  };

  const check = () => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: user._id,
          action: 'CHECK',
        }),
      });
    }
  };

  const startGame = () => {
    axios
      .post(`/game/${gameId}/start`)
      .then(() => {
        console.log('Game started');
      })
      .catch((error) => {
        console.error('Error starting game:', error);
      });
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  // Ensure the game object and its properties are defined before rendering
  if (!game || !game.communityCards || !game.players) {
    console.error('Game data is incomplete:', game);
    return <div>No game data available.</div>;
  }

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
          <h3>Pot: ${game.pot?.toFixed(2)}</h3>
        </div>
        <div className="players">
          {game.players.map((player, index) => (
            <div
              key={index}
              className={`player ${index === game.currentPlayerIndex ? 'current-player' : ''}`}
            >
              <h3>{player.username}</h3>
              <p>Chips: ${player.chips?.toFixed(2)}</p>
              <p>Last Action: {game.lastActions?.[player.username] || 'None'}</p>
              <p>Bet: ${game.currentBettingRound?.playerBets?.[player.username] || 0}</p>
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
      <div className="player-cards-section">
        <h2>Your Cards</h2>
        <div className="cards">
          {cards.map((card, index) => (
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
      <div className="betting-controls">
        <button onClick={() => placeBet(10)} className="bet-button">
          Bet 10
        </button>
        <button onClick={check} className="check-button">
          Check
        </button>
        <button onClick={fold} className="fold-button">
          Fold
        </button>
      </div>
      <div className="game-controls">
        {game.status === 'WAITING' && (
          <button onClick={startGame} className="start-game-button">
            Start Game
          </button>
        )}
      </div>
    </div>
  );
};

export default PokerTable;