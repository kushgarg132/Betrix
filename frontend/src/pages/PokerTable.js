import React, { useEffect, useState, useContext, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import API_CONFIG from '../config/apiConfig';
import './PokerTable.css';

// Components
import Player from '../components/Player';
import Card, { getSuitSymbol } from '../components/Card';
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
  const [animatingChips, setAnimatingChips] = useState(false);
  const [isMyTurn, setIsMyTurn] = useState(false);
  const [sidebarVisible, setSidebarVisible] = useState(false);
  const [leftSidebarVisible, setLeftSidebarVisible] = useState(false);

  const gameStatus = {
    GAME_STARTED: 'GAME_STARTED',
    PLAYER_JOINED: 'PLAYER_JOINED',
    PLAYER_ACTION: 'PLAYER_ACTION',
    CARDS_DEALT: 'CARDS_DEALT',
    ROUND_STARTED: 'ROUND_STARTED',
    COMMUNITY_CARDS: 'COMMUNITY_CARDS',
    GAME_ENDED: 'GAME_ENDED'
  };

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
            chips: player.chips
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
          case gameStatus.GAME_STARTED:
          case gameStatus.ROUND_STARTED:
            newGameState = {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || [],
              winners: []
            };
            break;

          case gameStatus.PLAYER_ACTION:
            newGameState = {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || []
            };
            break;

          case gameStatus.CARDS_DEALT:
            setCurrentPlayer(prevPlayer => ({
              ...prevPlayer,
              hand: Array.isArray(payload) ? payload : (prevPlayer.hand || [])
            }));
            return prevGame;

          case gameStatus.PLAYER_JOINED:
            newGameState = {
              ...prevGame,
              players: [...(prevGame?.players || []), payload]
            };
            break;

          case gameStatus.GAME_ENDED:
            console.log("Game ended with payload:", payload);
            newGameState = {
              ...(payload.game),
              winners: payload.winners?.map(winner => {
                console.log("Winner data:", winner);
                return {
                  ...winner,
                  bestHand: winner.bestHand || payload.bestHand
                };
              }) || [],
              status: 'ENDED',
              communityCards: payload.game.communityCards || [],
              players: payload.game.players || []
            };
            console.log("Updated game state:", newGameState);
            break;

          case gameStatus.COMMUNITY_CARDS:
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
    }
  }, [updateAction, gameStatus]);

  // Set isMyTurn state when it's the current player's turn
  useEffect(() => {
    if (game && typeof game.currentPlayerIndex === 'number' && typeof currentPlayer.index === 'number') {
      setIsMyTurn(game.currentPlayerIndex === currentPlayer.index);
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
      />

      <div className="poker-table-main">
        {game.winners?.length > 0 && (
          <div className="winners-overlay">
            <h2>Winner{game.winners.length > 1 ? 's' : ''}!</h2>
            <ul>
              {game.winners.map((winner, index) => (
                <li key={winner.username || index}>
                  <strong>{winner.username}</strong>
                  {winner.bestHand && winner.bestHand.rank && (
                    <span className="hand-rank">
                      {typeof winner.bestHand.rank === 'string' 
                        ? winner.bestHand.rank.replace(/_/g, ' ') 
                        : winner.bestHand.rank}
                    </span>
                  )}
                  
                  {winner && winner.hand && winner.hand.length > 0 && (
                    <div className="winner-section">
                      <h4>Player Hand</h4>
                      <div className="winner-cards">
                        {winner.hand.map((card, cardIndex) => (
                          <Card 
                            key={`best-${cardIndex}-${card.rank}-${card.suit}`} 
                            card={card} 
                            hidden={false} 
                          />
                        ))}
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
            <p>Next round starting soon...</p>
          </div>
        )}

        <Table 
          isMyTurn={isMyTurn} 
          game={game} 
          communityCards={game.communityCards} 
          getSuitSymbol={getSuitSymbol} 
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

      <BettingControls 
        isMyTurn={isMyTurn} 
        game={game} 
        currentPlayer={currentPlayer} 
        placeBet={placeBet} 
        check={check} 
        fold={fold} 
      />

      <RankingsModal 
        showRankingsModal={showRankingsModal} 
        toggleRankingsModal={() => setShowRankingsModal(!showRankingsModal)} 
      />
    </div>
  );
};

export default PokerTable;