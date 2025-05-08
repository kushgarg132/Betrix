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
      
      // Subscribe to game-wide updates
      client.subscribe(`/topic/game/${gameId}`, (message) => {
        try {
          const gameUpdate = JSON.parse(message.body);
          console.log('Received game update:', gameUpdate);
          setUpdateActions(gameUpdate);
        } catch (error) {
          console.error('Error processing game update:', error);
        }
      });
      
      // Subscribe to player-specific updates (corrected path)
      client.subscribe(`/topic/game/${gameId}/player/${playerId}`, (message) => {
        try {
          const playerUpdate = JSON.parse(message.body);
          console.log('Received player update:', playerUpdate);
          
          // Check if this is a cards update
          if (playerUpdate.type === 'CARDS_DEALT' && playerUpdate.payload) {
            setCurrentPlayer((prevPlayer) => ({ ...prevPlayer, hand: playerUpdate.payload }));
          }
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
    PLAYER_CHECK: 'PLAYER_CHECK',
    PLAYER_FOLDED: 'PLAYER_FOLDED',
    CARDS_DEALT: 'CARDS_DEALT',
    ROUND_STARTED: 'ROUND_STARTED',
    ROUND_COMPLETE: 'ROUND_COMPLETE',
    GAME_ENDED: 'GAME_ENDED'
  };

  useEffect(() => {
    const joinGame = async () => {
      try {
        setLoading(true);
        // First join the game with a POST request
        const joinResponse = await axios.post(`/game/${gameId}/join`);
        console.log('Game joined successfully:', joinResponse.data);
        
        // Set the game state
        setGame(joinResponse.data);
        
        // Find current player in the game
        const playerIndex = joinResponse.data.players.findIndex(p => p.username === user.username);
        if (playerIndex !== -1) {
          const player = joinResponse.data.players[playerIndex];
          setCurrentPlayer({
            id: player.id,
            username: player.username,
            hand: player.hand || [],
            index: playerIndex
          });
          
          // Check if it's the player's turn initially
          setIsMyTurn(joinResponse.data.currentPlayerIndex === playerIndex);
        } else {
          console.warn('Current player not found in game players list');
        }
        
        setLoading(false);
        
        // Initialize WebSocket connection after successfully joining
        if (user && user.id) {
          const client = initializeSocketConnection(gameId, user.id, setUpdateActions, setCurrentPlayer, setError);
          setStompClient(client);
          
          // Set up cleanup function
          return () => {
            if (client && client.connected) {
              // Send leave action before disconnecting
              client.publish({
                destination: `/app/game/${gameId}/action`,
                body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'LEAVE' }),
              });
              client.deactivate();
            }
          };
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
    if (updateAction && game) {
      console.log('Processing update action:', updateAction);
      
      // Update game state based on the update type and payload
      setGame(prevGame => {
        if (!prevGame) return updateAction;
        
        // Check if updateAction is already a complete game state (not a GameUpdate object)
        if (!updateAction.type && !updateAction.payload) {
          console.log('Received direct game state update:', updateAction);
          return {
            ...updateAction,
            // Ensure these properties exist to prevent errors
            communityCards: updateAction?.communityCards || [],
            players: updateAction?.players || []
          };
        }
        
        // Extract the payload from the GameUpdate object
        const payload = updateAction.payload;
        const updateType = updateAction.type;
        
        if (!payload) {
          console.warn('Update action payload is empty');
          return prevGame;
        }
        
        console.log(`Processing ${updateType} update with payload:`, payload);
        
        // Handle different update types
        switch (updateType) {
          case 'GAME_STARTED':
          case 'ROUND_STARTED':
          case 'PLAYER_BET':
          case 'PLAYER_CHECKED':
          case 'PLAYER_FOLDED':
          case 'PLAYER_LEFT':
          case 'PLAYER_TURN':
            // These updates contain the full game state
            return {
              ...payload,
              // Ensure these properties exist to prevent errors
              communityCards: payload.communityCards || [],
              players: payload.players || []
            };
            
          case 'PLAYER_JOINED':
            // Add the new player to the players array
            return {
              ...prevGame,
              players: [...(prevGame.players || []), payload]
            };
            
          case 'GAME_ENDED':
            // Update with winners and game state
            return {
              ...(payload.game || prevGame),
              winners: payload.winners || [],
              status: 'ENDED',
              // Ensure these properties exist to prevent errors
              communityCards: (payload.game && payload.game.communityCards) || prevGame.communityCards || [],
              players: (payload.game && payload.game.players) || prevGame.players || []
            };
            
          case 'CARDS_DEALT':
            // Update community cards
            return {
              ...prevGame,
              communityCards: Array.isArray(payload) ? payload : (prevGame.communityCards || [])
            };
            
          default:
            console.warn('Unknown update type:', updateType);
            return {
              ...prevGame,
              // Ensure these properties exist to prevent errors
              communityCards: prevGame.communityCards || [],
              players: prevGame.players || []
            };
        }
      });
      
      // We'll handle the isMyTurn update in a separate useEffect to ensure it's always in sync
    }
  }, [updateAction, currentPlayer]);

  // Dedicated useEffect to update isMyTurn whenever game state changes
  useEffect(() => {
    if (game && game.players && game.currentPlayerIndex !== -1 && currentPlayer.username) {
      // Find the current turn player
      const currentTurnPlayer = game.players[game.currentPlayerIndex];
      
      // Check if it's the current user's turn
      const myTurn = currentTurnPlayer && currentTurnPlayer.username === currentPlayer.username;
      
      console.log(
        `Turn update: Player ${currentTurnPlayer?.username} (index: ${game.currentPlayerIndex}), ` +
        `Current user: ${currentPlayer.username}, isMyTurn: ${myTurn}`
      );
      
      // Update the isMyTurn state
      setIsMyTurn(myTurn);
    } else {
      // If any required data is missing, set isMyTurn to false
      setIsMyTurn(false);
    }
    
    console.log('Game state updated:', game);
    
    // Debug player positions whenever game state updates
    if (game && game.players) {
      logPlayers(game.players, game.currentPlayerIndex);
      checkPlayerElements();
      highlightPlayers();
    }
  }, [game, game?.currentPlayerIndex]);

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
    if (stompClient && stompClient.connected && currentPlayer && currentPlayer.id) {
      try {
        // Send leave action via WebSocket
        stompClient.publish({
          destination: `/app/game/${gameId}/action`,
          body: JSON.stringify({ playerId: currentPlayer.id, actionType: 'LEAVE' }),
        });
        stompClient.deactivate();
        
        console.log('Successfully left the game');
        // Redirect to home or lobby
        window.location.href = '/';
      } catch (error) {
        console.error('Error leaving game:', error);
        // Still redirect even if there was an error
        window.location.href = '/';
      }
    } else {
      console.warn('Cannot leave table: WebSocket not connected or player ID not available');
      // Redirect anyway
      window.location.href = '/';
    }
  };

  if (loading) return <div className="loading-spinner">Loading game...</div>;
  if (error) return <div className="error">{error}</div>;

  if (!game) {
    console.error('Game data is missing');
    return <div>No game data available. Please try again later.</div>;
  }
  
  if (!game.communityCards) {
    console.error('Community cards missing in game data:', game);
    // Initialize with empty array instead of failing
    game.communityCards = [];
  }
  
  if (!game.players || !Array.isArray(game.players)) {
    console.error('Players missing or invalid in game data:', game);
    return <div>Invalid game data. Please try refreshing the page.</div>;
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