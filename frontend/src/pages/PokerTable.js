import React, { useEffect, useState, useContext } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import API_CONFIG from '../config/apiConfig';
import './PokerTable.css';
import './PokerTableCyberpunk.css'; // Neon Cyberpunk theme

// Components
import Player from '../components/Player';
import Table from '../components/Table';
import BettingControls from '../components/BettingControls';
import RankingsModal from '../components/RankingsModal';
import GameInfoPanel from '../components/GameInfoPanel';
import LeftSidebar from '../components/LeftSidebar';

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
  const [animatingChips] = useState(false);
  const [isMyTurn, setIsMyTurn] = useState(false);
  const [sidebarVisible, setSidebarVisible] = useState(false);
  const [leftSidebarVisible, setLeftSidebarVisible] = useState(false);

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
              client.publish({
                destination: `/app/game/${gameId}/action`,
                body: JSON.stringify({ playerId: player.id, actionType: 'LEAVE' }),
              });
              client.deactivate();
            }
          };
        } else {
          setError('Failed to find player in game. Please try again.');
        }
      } catch (error) {
        console.error('Error joining game:', error);
        setError('Failed to join the game. Please try again later.');
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
        if (typeof updateAction.type === 'undefined') {
          return {
            ...updateAction,
            communityCards: updateAction.communityCards || [],
            players: updateAction.players || []
          };
        }

        const { type, payload } = updateAction;
        if (typeof type === 'undefined' || typeof payload === 'undefined') {
          return prevGame;
        }

        let newGameState;
        switch (type) {
          case GAME_STATUS.GAME_STARTED:
          case GAME_STATUS.ROUND_STARTED:
            newGameState = {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || [],
              winners: [],
              pots: payload.pots || [{ amount: payload.pot || 0, eligiblePlayerIds: [] }]
            };
            break;

          case GAME_STATUS.PLAYER_ACTION:
            newGameState = {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || [],
              pots: payload.pots || [{ amount: payload.pot || 0, eligiblePlayerIds: [] }]
            };
            break;

          case GAME_STATUS.CARDS_DEALT:
            setCurrentPlayer(prevPlayer => ({
              ...prevPlayer,
              hand: Array.isArray(payload) ? payload : (prevPlayer.hand || [])
            }));
            return prevGame;

          case GAME_STATUS.PLAYER_JOINED:
            newGameState = {
              ...prevGame,
              players: [...(prevGame?.players || []), payload]
            };
            break;

          case GAME_STATUS.GAME_ENDED:
            console.log("Game ended with payload:", payload);
            newGameState = {
              ...(payload.game),
              winners: payload.winners?.map(winner => {
                console.log("Winner data:", winner);
                const bestHand = winner.bestHand || payload.bestHand;
                return {
                  ...winner,
                  bestHand: bestHand,
                  hand: winner.hand || []
                };
              }) || [],
              status: 'ENDED',
              communityCards: payload.game.communityCards || [],
              players: payload.game.players || [],
              pots: payload.game.pots || [{ amount: payload.game.pot || 0, eligiblePlayerIds: [] }]
            };
            console.log("Updated game state with winners:", newGameState);
            break;

          case GAME_STATUS.COMMUNITY_CARDS:
            newGameState = {
              ...prevGame,
              communityCards: Array.isArray(payload) ? payload : (prevGame?.communityCards || [])
            };
            break;

          default:
            return prevGame ? {
              ...prevGame
            } : null;
        }
        return newGameState;
      });
      setUpdateActions(null);
    }
  }, [updateAction]);

  // Set isMyTurn state when it's the current player's turn
  useEffect(() => {
    if (game && typeof game.currentPlayerIndex === 'number' && typeof currentPlayer.index === 'number') {
      const isTurn = game.currentPlayerIndex === currentPlayer.index;
      // Ensure player hasn't folded and isn't sitting out
      const player = game.players[currentPlayer.index];
      const isActive = player && !player.hasFolded && !player.isSittingOut;
      
      setIsMyTurn(isTurn && isActive);
    } else {
      setIsMyTurn(false);
    }
  }, [game, currentPlayer]);

  const placeBet = (amount) => {
    if (stompClient?.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: currentPlayer.id,
          actionType: 'BET',
          amount: amount
        }),
      });
    }
  };

  const fold = () => {
    if (stompClient?.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: currentPlayer.id,
          actionType: 'FOLD'
        }),
      });
    }
  };

  const check = () => {
    if (stompClient?.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: currentPlayer.id,
          actionType: 'CHECK',
        }),
      });
    }
  };

  const leaveTable = () => {
    if (stompClient?.connected && currentPlayer?.id) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'LEAVE' }),
      });
      stompClient.deactivate();
    }
    window.location.href = '/';
  };

  const sitOut = () => {
    if (stompClient?.connected && currentPlayer?.id) {
       stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'SIT_OUT' }),
      });
    }
  };

  const sitIn = () => {
     if (stompClient?.connected && currentPlayer?.id) {
       stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'SIT_IN' }),
      });
    }
  };

  const getDisplayPosition = (actualIndex) => {
    if (!game || currentPlayer.index === null) return 0;
    const playerCount = game.players.length;
    return ((actualIndex - currentPlayer.index + playerCount) % playerCount) % 6;
  };

  const getBlindStatus = (player) => {
    if (!game) return null;
    if (game.bigBlindUserId === player.username) return 'big-blind';
    if (game.smallBlindUserId === player.username) return 'small-blind';
    return null;
  };

  if (loading) return <div className="loading-spinner">Loading game...</div>;
  if (error) return <div className="error">{error}</div>;
  if (!game || !game.players || !Array.isArray(game.players)) {
    return <div>Invalid game data. Please try refreshing the page.</div>;
  }

  return (
    <div className="poker-table-container">
      <LeftSidebar
        leftSidebarVisible={leftSidebarVisible}
        toggleLeftSidebar={() => setLeftSidebarVisible(!leftSidebarVisible)}
        leaveTable={leaveTable}
        sitOut={sitOut}
        sitIn={sitIn}
        isSittingOut={game?.players?.find(p => p.username === user.username)?.isSittingOut || false}
      />

      <div className="poker-table-main">


        <Table
          isMyTurn={isMyTurn}
          game={game}
          communityCards={game.communityCards}
          animatingChips={animatingChips}
        />

        <div className="player-positions">
          {game.players.map((player, index) => {
            if (!player) return null;
            const displayPosition = getDisplayPosition(index);
            const blindStatus = getBlindStatus(player);
            const playerBet = game.currentBettingRound?.playerBets[player.username] || 0;
            const isCurrentTurn = game.currentPlayerIndex === index;

            return (
              <Player
                key={player._id || index}
                player={player}
                displayPosition={displayPosition}
                blindStatus={blindStatus}
                playerBet={playerBet}
                isCurrentTurn={isCurrentTurn}
                isCurrentPlayer={player.username === currentPlayer.username}
                playerCount={game.players.length}
                currentHand={currentPlayer.hand}
                actionDeadline={game.currentPlayerActionDeadline}
                game={game}
              />
            );
          })}
        </div>
      </div>

      <GameInfoPanel
        sidebarVisible={sidebarVisible}
        toggleSidebar={() => setSidebarVisible(!sidebarVisible)}
        game={game}
        toggleRankingsModal={() => setShowRankingsModal(!showRankingsModal)}
      />

      {/* Betting controls positioned above the current player */}
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