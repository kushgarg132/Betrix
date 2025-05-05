import React, { useEffect, useState, useContext, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import API_CONFIG from '../config/apiConfig';
import './PokerTable.css';

// Import Components
import Player from '../components/Player';
import Card, { getSuitSymbol } from '../components/Card';
import Table from '../components/Table';
import BettingControls from '../components/BettingControls';
import RaiseSlider from '../components/RaiseSlider';
import RankingsModal from '../components/RankingsModal';
import GameInfoPanel from '../components/GameInfoPanel';
import LeftSidebar from '../components/LeftSidebar';

// Import debug utility
import { logPlayers, checkPlayerElements, highlightPlayers } from '../debug/PokerDebug';

const initializeSocketConnection = (gameId, playerId, setUpdateActions, setCurrentPlayer, setError) => {
  const socket = new SockJS(API_CONFIG.WS_URL);
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
  const [currentPlayer, setCurrentPlayer] = useState({ id: null, username: null, hand: [], index: null });
  const [raiseAmount, setRaiseAmount] = useState(10); // Default raise amount
  const [showRaiseSlider, setShowRaiseSlider] = useState(false); // State for slider visibility
  const [showRankingsModal, setShowRankingsModal] = useState(false); // State for rankings modal
  const [animatingChips, setAnimatingChips] = useState(false); // State for chip animation
  const [lastAction, setLastAction] = useState(null); // Store the last action for animation
  const chipRefs = useRef([]); // Refs for chip animations
  const [isMyTurn, setIsMyTurn] = useState(false); // Track if it's the user's turn
  const [sidebarVisible, setSidebarVisible] = useState(false); // State for sidebar visibility - minimized by default
  const [leftSidebarVisible, setLeftSidebarVisible] = useState(false); // Default to minimized

  const toggleSidebar = () => {
    // Toggle sidebar visibility with explicit boolean state
    setSidebarVisible(prevVisible => !prevVisible);
    console.log("Game info sidebar toggled:", !sidebarVisible);
  };

  const toggleLeftSidebar = () => {
    setLeftSidebarVisible(!leftSidebarVisible);
  };

  const toggleRaiseSlider = () => {
    setShowRaiseSlider((prev) => !prev); // Toggle slider visibility
  };

  const toggleRankingsModal = () => {
    setShowRankingsModal((prev) => !prev); // Toggle rankings modal
  };

  const cancelRaise = () => {
    setShowRaiseSlider(false); // Close the slider
    setRaiseAmount(Math.max(game.currentBet + 1, 1)); // Reset raise amount to minimum valid value
  };

  const handleRaiseChange = (value) => {
    setRaiseAmount(Math.round(value)); // Update raiseAmount with rounded value
  };

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
            hand: player.hand || [],
            index: playerIndex
          });
          
          // Check if it's the player's turn initially
          setIsMyTurn(response.data.currentPlayerIndex === playerIndex);
          
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
    
    // Debug player positions whenever game state updates
    if (game && game.players) {
      logPlayers(game.players, game.currentPlayerIndex);
      checkPlayerElements();
      highlightPlayers();
    }
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
          // Update isMyTurn state when turn changes
          setIsMyTurn(updateAction.payload === currentPlayer.index);
          setGame((prevGame) => ({
            ...prevGame,
            currentPlayerIndex: updateAction.payload,
          }));
          break;

        case gameStatus.PLAYER_BET:
          // Store the last action for animation
          setLastAction({
            type: 'bet',
            playerId: updateAction.payload.players[updateAction.payload.currentPlayerIndex - 1 < 0 
              ? updateAction.payload.players.length - 1 
              : updateAction.payload.currentPlayerIndex - 1]._id,
            amount: updateAction.payload.currentBet
          });
          setAnimatingChips(true);
          setTimeout(() => {
            setAnimatingChips(false);
          }, 1000);
          setGame(updateAction.payload);
          // Update isMyTurn state when receiving a bet update
          setIsMyTurn(updateAction.payload.currentPlayerIndex === currentPlayer.index);
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

  // Update isMyTurn whenever currentPlayerIndex changes
  useEffect(() => {
    if (game && currentPlayer.index !== null) {
      setIsMyTurn(game.currentPlayerIndex === currentPlayer.index);
    }
  }, [game?.currentPlayerIndex, currentPlayer.index]);

  const placeBet = (amount) => {
    if (stompClient && stompClient.connected) {
      // Animate chips before sending the action
      setLastAction({
        type: 'bet',
        playerId: currentPlayer.id,
        amount: amount
      });
      setAnimatingChips(true);
      
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: currentPlayer.id,
          amount,
          actionType: 'BET',
        }),
      });
      
      // Reset animation after a delay
      setTimeout(() => {
        setAnimatingChips(false);
      }, 1000);
    }
  };

  const fold = () => {
    if (stompClient && stompClient.connected) {
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
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({
          playerId: currentPlayer.id,
          actionType: 'CHECK',
        }),
      });
    }
  };

  // Function to calculate relative position for display
  const getDisplayPosition = (actualIndex) => {
    if (!game || currentPlayer.index === null) {
      console.log('Cannot position player: game or currentPlayer.index is null');
      return 0;
    }
    
    const playerCount = game.players.length;
    // Calculate position relative to current player
    let relativePosition = (actualIndex - currentPlayer.index + playerCount) % playerCount;
    
    // Debug information
    console.log(`Player positioning: actual=${actualIndex}, current=${currentPlayer.index}, relative=${relativePosition}, count=${playerCount}`);
    
    // Force position to be between 0-5 for consistent positioning regardless of player count
    return relativePosition % 6;
  };

  // Function to determine if a player is a big or small blind
  const getBlindStatus = (player) => {
    if (!game) return null;
    
    if (game.bigBlindPosition === player.username) {
      return 'big-blind';
    } else if (game.smallBlindPosition === player.username) {
      return 'small-blind';
    }
    return null;
  };

  const leaveTable = () => {
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/game/${gameId}/action`,
        body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'LEAVE' }),
      });
      stompClient.deactivate();
      // Redirect to home or lobby
      window.location.href = '/';
    }
  };

  if (loading) return <div className="loading-spinner"></div>;
  if (error) return <div className="error">{error}</div>;

  if (!game || !game.communityCards || !game.players) {
    console.error('Game data is incomplete:', game);
    return <div>No game data available.</div>;
  }

  return (
    <div className="poker-table-container">
      {/* Left Sidebar */}
      <LeftSidebar 
        leftSidebarVisible={leftSidebarVisible} 
        toggleLeftSidebar={toggleLeftSidebar} 
        leaveTable={leaveTable} 
      />

      <div className="poker-table-main">
        {/* Main Table */}
        <Table 
          isMyTurn={isMyTurn} 
          game={game} 
          communityCards={game.communityCards} 
          getSuitSymbol={getSuitSymbol} 
          animatingChips={animatingChips} 
        />

        {/* Players positioned around the table */}
        <div className="player-positions">
          {game.players.length > 0 && game.players.map((player, index) => {
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

        {/* Game Info Panel */}
        <GameInfoPanel 
          sidebarVisible={sidebarVisible} 
          toggleSidebar={toggleSidebar} 
          game={game} 
          toggleRankingsModal={toggleRankingsModal} 
        />
      </div>

      {/* Betting Controls fixed at bottom */}
      <BettingControls 
        isMyTurn={isMyTurn} 
        game={game} 
        currentPlayer={currentPlayer} 
        toggleRaiseSlider={toggleRaiseSlider} 
        placeBet={placeBet} 
        check={check} 
        fold={fold} 
      />

      {/* Modals */}
      <RaiseSlider 
        showRaiseSlider={showRaiseSlider} 
        raiseAmount={raiseAmount} 
        handleRaiseChange={handleRaiseChange} 
        game={game} 
        currentPlayer={currentPlayer} 
        toggleRaiseSlider={toggleRaiseSlider} 
        placeBet={placeBet} 
        cancelRaise={cancelRaise} 
      />

      <RankingsModal 
        showRankingsModal={showRankingsModal} 
        toggleRankingsModal={toggleRankingsModal} 
      />
    </div>
  );
};

export default PokerTable;