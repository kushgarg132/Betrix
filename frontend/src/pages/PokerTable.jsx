import React, { useEffect, useState, useContext, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import API_CONFIG from '../config/apiConfig';
import './PokerTable.css';

// Components
import Player from '../components/Player';
import Table from '../components/Table';
import BettingControls from '../components/BettingControls';
import RankingsModal from '../components/RankingsModal';

const initializeSocketConnection = (gameId, playerId, setUpdateActions, setError) => {
  const socket = new SockJS(API_CONFIG.WS_URL);
  const client = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    onConnect: () => {
      client.subscribe(`/topic/game/${gameId}`, (message) => {
        try {
          const gameUpdate = JSON.parse(message.body);
          setUpdateActions(gameUpdate);
        } catch (error) {
          console.error('Error processing game update:', error);
        }
      });

      client.subscribe(`/topic/game/${gameId}/player/${playerId}`, (message) => {
        try {
          const playerUpdate = JSON.parse(message.body);
          setUpdateActions(playerUpdate);
        } catch (error) {
          console.error('Error processing player update:', error);
        }
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

const GAME_STATUS = {
  GAME_STARTED: 'GAME_STARTED',
  PLAYER_JOINED: 'PLAYER_JOINED',
  PLAYER_ACTION: 'PLAYER_ACTION',
  CARDS_DEALT: 'CARDS_DEALT',
  ROUND_STARTED: 'ROUND_STARTED',
  COMMUNITY_CARDS: 'COMMUNITY_CARDS',
  GAME_ENDED: 'GAME_ENDED',
};

const PokerTable = () => {
  const { gameId } = useParams();
  const { user } = useContext(AuthContext);
  const [game, setGame] = useState(null);
  const [updateAction, setUpdateActions] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [stompClient, setStompClient] = useState(null);
  const [currentPlayer, setCurrentPlayer] = useState({ id: null, username: null, hand: [], index: null });
  const [showRankingsModal, setShowRankingsModal] = useState(false);
  const [isMyTurn, setIsMyTurn] = useState(false);
  const [chatMessages, setChatMessages] = useState([]);

  useEffect(() => {
    const joinGame = async () => {
      try {
        setLoading(true);
        const joinResponse = await axios.post(`/game/${gameId}/join`);
        setGame(joinResponse.data);

        const playerIndex = joinResponse.data.players.findIndex(p => p.username === user.username);
        if (playerIndex !== -1) {
          const player = joinResponse.data.players[playerIndex];
          setCurrentPlayer({
            id: player.id,
            username: player.username,
            hand: player.hand || [],
            index: playerIndex,
            chips: player.chips,
            isSittingOut: player.isSittingOut
          });

          const client = initializeSocketConnection(gameId, player.id, setUpdateActions, setError);
          setStompClient(client);

          setLoading(false);
          return () => {
            if (client && client.connected) {
              client.deactivate();
            }
          };
        } else {
          setError('Failed to find player in game.');
        }
      } catch (error) {
        console.error('Error joining game:', error);
        setError('Failed to join the game.');
        setLoading(false);
      }
    };

    if (gameId && user) {
      joinGame();
    }
  }, [gameId, user]);

  useEffect(() => {
    if (updateAction) {
      setGame(prevGame => {
        if (!updateAction.type) return updateAction;
        const { type, payload } = updateAction;

        console.log(`Processing game update: ${type}`, payload);

        switch (type) {
          case 'CHAT_MESSAGE':
            setChatMessages(prev => [...prev, payload].slice(-100));
            return prevGame;

          case GAME_STATUS.CARDS_DEALT:
            setCurrentPlayer(prev => {
              if (Array.isArray(payload)) {
                return { ...prev, hand: payload };
              }
              return prev;
            });
            return prevGame;

          case GAME_STATUS.COMMUNITY_CARDS:
            return {
              ...prevGame,
              communityCards: Array.isArray(payload) ? payload : (prevGame?.communityCards || [])
            };

          case GAME_STATUS.PLAYER_JOINED:
            return {
              ...prevGame,
              players: [...(prevGame?.players || []), payload]
            };

          case GAME_STATUS.PLAYER_ACTION:
            // Extract the nested game object
            const actGame = payload.game || payload;
            return {
              ...actGame,
              communityCards: actGame.communityCards || [],
              players: actGame.players || []
            };

          case GAME_STATUS.GAME_STARTED:
          case GAME_STATUS.ROUND_STARTED:
            return {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || []
            };

          case GAME_STATUS.GAME_ENDED:
            const endGameState = payload.game || payload;
            return {
              ...endGameState,
              winners: payload.winners || [],
              bestHand: payload.bestHand || null,
              status: 'ENDED'
            };

          default:
            return payload.game || payload || prevGame;
        }
      });
      setUpdateActions(null);
    }
  }, [updateAction]);

  useEffect(() => {
    if (game && currentPlayer.index !== null) {
      const isTurn = game.currentPlayerIndex === currentPlayer.index;
      const player = game.players[currentPlayer.index];
      setIsMyTurn(isTurn && player && !player.hasFolded && !player.isSittingOut);
    }
  }, [game, currentPlayer]);

  const placeBet = (amount) => {
    stompClient?.publish({
      destination: `/app/game/${gameId}/action`,
      body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'BET', amount }),
    });
  };

  const fold = () => {
    stompClient?.publish({
      destination: `/app/game/${gameId}/action`,
      body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'FOLD' }),
    });
  };

  const check = () => {
    stompClient?.publish({
      destination: `/app/game/${gameId}/action`,
      body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'CHECK' }),
    });
  };

  const getDisplayPosition = (actualIndex) => {
    if (!game || currentPlayer.index === null) return 0;
    const playerCount = game.players.length;
    return ((actualIndex - currentPlayer.index + playerCount) % playerCount);
  };

  const getBlindStatus = (player) => {
    if (!game) return null;
    if (game.bigBlindUserId === player.username) return 'big-blind';
    if (game.smallBlindUserId === player.username) return 'small-blind';
    return null;
  };

  if (loading) return <div className="loading-spinner">Loading Premium Universe...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="poker-table-container">
      {/* 1. Tournament Info Bar (Mockup Element) */}
      <div className="tournament-top-bar">
        <div className="tournament-label">Tournament #741 / Blinds 500/1000, Ante 100</div>
        <div className="tournament-stats">
          <span>Level <span className="level-badge">14</span></span>
          <span>Players 6/8</span>
          <span>Timer: 12s</span>
        </div>
      </div>

      <div className="poker-table-main">
        {/* 2. Central Table with 3D perspective */}
        <Table
          isMyTurn={isMyTurn}
          game={game}
          communityCards={game.communityCards || []}
        />

        {/* 3. Circular Player Distribution */}
        <div className="player-positions">
          {game.players.map((player, index) => {
            if (!player) return null;
            return (
              <Player
                key={player.id || index}
                player={player}
                displayPosition={getDisplayPosition(index)}
                blindStatus={getBlindStatus(player)}
                playerBet={game.currentBettingRound?.playerBets[player.username] || 0}
                isCurrentTurn={game.currentPlayerIndex === index}
                isCurrentPlayer={player.username === currentPlayer.username}
                currentHand={currentPlayer.hand}
                actionDeadline={game.currentPlayerActionDeadline}
                game={game}
              />
            );
          })}
        </div>
      </div>

      {/* 4. Floating Chat Panel (Bottom Left Mockup) */}
      <div className="floating-chat-container">
        <div className="glass-chat-panel">
          <div className="chat-header">CHAT</div>
          <div className="chat-messages-scroll">
            {chatMessages.map((msg, i) => (
              <div key={i} className="chat-msg">
                <span className="chat-user">{msg.senderName}:</span>
                <span>{msg.message}</span>
              </div>
            ))}
            {chatMessages.length === 0 && <div className="chat-msg opacity-50">Welcome to the table.</div>}
          </div>
        </div>
      </div>

      {/* 5. Central Betting Controls (Bottom Center) */}
      {isMyTurn && (
        <div className="current-player-controls">
          <BettingControls
            isMyTurn={isMyTurn}
            game={game}
            currentPlayer={currentPlayer}
            placeBet={placeBet}
            check={check}
            fold={fold}
          />
        </div>
      )}

      <RankingsModal
        showRankingsModal={showRankingsModal}
        toggleRankingsModal={() => setShowRankingsModal(!showRankingsModal)}
      />
    </div>
  );
};

export default PokerTable;