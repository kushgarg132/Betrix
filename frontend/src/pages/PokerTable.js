import React, { useEffect, useState, useContext } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import './PokerTable.css';
import PlayerCard from '../components/PlayerCard';
import ReactSlider from 'react-slider';
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

const initializeSocketConnection = (gameId, playerId, setUpdateActions, setCurrentPlayer, setError) => {
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
        setCurrentPlayer((prevPlayer) => ({ ...prevPlayer, hand: cards }));
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
  const [currentPlayer, setCurrentPlayer] = useState({ id: null, username: null, cards: [], index: null });
  const [raiseAmount, setRaiseAmount] = useState(10); // Default raise amount

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
    if (gameId) {
      setLoading(true); // Ensure loading state is set before API call
      axios
        .post(`/game/${gameId}/join`)
        .then((response) => {
          setGame(response.data);
          setLoading(false);
          console.log('Game Joined', response.data);
          const playerIndex = response.data.players.findIndex((p) => p?.username === user.username);

          const player = response.data.players[playerIndex];
          setCurrentPlayer({
            id: player.id,
            username: player.username,
            cards: player.cards || [],
            index: playerIndex
          });
          const client = initializeSocketConnection(gameId, player.id, setUpdateActions, setCurrentPlayer, setError);
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
            body: JSON.stringify({ playerId: currentPlayer.id }),
          });
          stompClient.deactivate();
        }
      };
    }
  }, [gameId]);

  useEffect(() => {
    console.log('Game state updated:', game);
  }, [game]);

  useEffect(() => {
    if (updateAction) {
      console.log('Update action:', updateAction);
      switch (updateAction.type) {
        case gameStatus.GAME_STARTED:
          setGame((prevGame) => ({
            ...prevGame,
            ...updateAction.payload,
            players: [...prevGame.players],
          }));
          break;

        case gameStatus.PLAYER_JOINED:
          setGame((prevGame) => ({
            ...prevGame,
            players: [...prevGame.players, updateAction.payload],
          }));
          break;

        case gameStatus.PLAYER_LEFT:
          setGame((prevGame) => ({
            ...prevGame,
            players: prevGame.players.filter(
              (player) => player._id !== updateAction.payload
            ),
          }));
          break;

        case gameStatus.PLAYER_TURN:
          setGame((prevGame) => ({
            ...prevGame,
            currentPlayerIndex: updateAction.payload,
          }));
          break;

        case gameStatus.PLAYER_BET:
          setGame(updateAction.payload);
          break;

        case gameStatus.PLAYER_FOLDED:
          setGame((prevGame) => ({
            ...prevGame,
            players: prevGame.players.map((player) =>
              player._id === updateAction.payload
                ? { ...player, hasFolded: true }
                : player
            ),
          }));
          break;

        case gameStatus.CARDS_DEALT:
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
          setGame((prevGame) => ({
            ...prevGame,
            ...updateAction.payload,
            players: [...prevGame.players],
          }));
          break;

        case gameStatus.ROUND_COMPLETE:
          setGame((prevGame) => ({
            ...prevGame,
            pot: updateAction.payload.pot,
            communityCards: updateAction.payload.communityCards,
          }));
          break;

        case gameStatus.GAME_ENDED:
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
    }
  }, [updateAction]);

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

  const placeBet = (amount) => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: currentPlayer.id,
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
          playerId: currentPlayer.id,
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
          playerId: currentPlayer.id,
          action: 'CHECK',
        }),
      });
    }
  };

  if (loading) return <div>Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  if (!game || !game.communityCards || !game.players) {
    console.error('Game data is incomplete:', game);
    return <div>No game data available.</div>;
  }

  const isPlayerTurn = game?.currentPlayerIndex === currentPlayer.index;

  return (
    <div className="poker-table">
      <div className="circular-table">
        <div className="community-cards">
          {game.communityCards.map((card, index) => (
            <div key={index} className={`card ${card.suit.toLowerCase()}`}>
              <div className="card-top">
                <span>{card.rank}</span>
                <span>{getSuitSymbol(card.suit)}</span>
              </div>
              <div className="card-center">{getSuitSymbol(card.suit)}</div>
              <div className="card-bottom">
                <span>{getSuitSymbol(card.suit)}</span>
                <span>{card.rank}</span>
              </div>
            </div>
          ))}
        </div>

        {game.players.map((player, index) => (
          <div
            key={player._id}
            className="player"
            data-position={index}
            style={{
              opacity: player.hasFolded ? 0.5 : 1,
              border: game.currentPlayerIndex === index ? '2px solid yellow' : 'none',
            }}
          >
            <div className="player-name">{player.username}</div>
            {player.username === currentPlayer.username && (
              <div className="player-cards">
                {currentPlayer.hand?.map((card, idx) => (
                  <div key={idx} className={`card ${card.suit.toLowerCase()}`}>
                    <div className="card-top">
                      <span>{card.rank}</span>
                      <span>{getSuitSymbol(card.suit)}</span>
                    </div>
                    <div className="card-center">{getSuitSymbol(card.suit)}</div>
                    <div className="card-bottom">
                      <span>{getSuitSymbol(card.suit)}</span>
                      <span>{card.rank}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <div className="chips">Chips: {player.chips}</div>
            <div className="player-bet">Bet: {game.currentBettingRound?.playerBets[player.username] || 0}</div>
          </div>
        ))}
      </div>

      <div className="game-info">
        <h3>Game Information</h3>
        <p><strong>Pot:</strong> ${game.pot}</p>
        <p><strong>Current Player:</strong> {game.players[game.currentPlayerIndex]?.username || 'N/A'}</p>
      </div>

      <button className="start-game-button" onClick={startGame}>Start Game</button>

      {isPlayerTurn && (
        <div className="betting-controls">
          <button className="bet-button" onClick={() => placeBet(0.5)}>Bet</button>
          <button className="check-button" onClick={check}>Check</button>
          <button className="fold-button" onClick={fold}>Fold</button>
          <ReactSlider
            className="raise-slider"
            thumbClassName="raise-slider-thumb"
            trackClassName="raise-slider-track"
            min={10}
            max={user?.chips ?? 100}
            value={raiseAmount}
            onChange={(value) => setRaiseAmount(value)}
            renderThumb={(props, state) => <div {...props}>{state.valueNow}</div>}
          />
        </div>
      )}
    </div>
  );
};

export default PokerTable;