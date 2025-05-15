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

const initializeSocketConnection = (gameId, playerId, setUpdateActions, setCurrentPlayer, setError) => {
  const socket = new SockJS(API_CONFIG.WS_URL);
  const client = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    onConnect: () => {
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
        if (currentPlayer && currentPlayer.id) {
          const client = initializeSocketConnection(gameId, currentPlayer.id, setUpdateActions, setCurrentPlayer, setError);
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
    if (updateAction) { // Only run if updateAction is present
      console.log('Processing update action:', updateAction);

      setGame(prevGame => {
        // Scenario 1: updateAction is the new, complete game state (no 'type'/'payload')
        // Check if 'type' is undefined; payload might be undefined if it's a full state
        if (typeof updateAction.type === 'undefined' /* && typeof updateAction.payload === 'undefined' */) {
          console.log('Received direct game state:', updateAction);
          return {
            ...updateAction,
            communityCards: updateAction.communityCards || [],
            players: updateAction.players || []
          };
        }

        // Scenario 2: updateAction is a structured update with type and payload
        const { type, payload } = updateAction;

        if (typeof type === 'undefined' || typeof payload === 'undefined') {
          console.warn('Update action is missing type or payload:', updateAction);
          return prevGame; // Return previous state if update is malformed
        }

        console.log(`Processing ${type} update with payload:`, payload);

        let newGameState;
        switch (type) {
          case gameStatus.GAME_STARTED:
          case gameStatus.ROUND_STARTED:
            if (!payload || typeof payload !== 'object') {
              console.error(`Invalid payload for ${type}:`, payload);
              return prevGame;
            }
            newGameState = {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || [],
              winners: [] // Explicitly clear winners
            };
            break;

          case gameStatus.PLAYER_ACTION:
            if (!payload || typeof payload !== 'object') {
              console.error(`Invalid payload for ${type}:`, payload);
              return prevGame;
            }
            newGameState = {
              ...payload,
              communityCards: payload.communityCards || [],
              players: payload.players || []
            };
            break;

          case gameStatus.CARDS_DEALT:
            setCurrentPlayer((prevPlayer) => ({
              ...prevPlayer,
              hand: Array.isArray(payload) ? payload : (prevPlayer.hand || [])
            }));
            return prevGame;

          case gameStatus.PLAYER_JOINED:
            if (!payload || typeof payload !== 'object') {
              console.error('Invalid payload for PLAYER_JOINED:', payload);
              return prevGame;
            }
            newGameState = {
              ...prevGame,
              players: [...(prevGame?.players || []), payload] // Ensure prevGame.players exists
            };
            break;

          case gameStatus.GAME_ENDED:
            if (!payload || !payload.game || typeof payload.game !== 'object') {
              console.error('Invalid payload for GAME_ENDED (missing or invalid game object):', payload);
              return prevGame;
            }
            newGameState = {
              ...(payload.game), // Base new state on payload.game
              winners: payload.winners || [],
              status: 'ENDED',
              communityCards: payload.game.communityCards || [], // Ensure these exist
              players: payload.game.players || []             // Ensure these exist
            };
            break;

          case gameStatus.COMMUNITY_CARDS:
            if (!Array.isArray(payload)) {
              console.error('Invalid payload for COMMUNITY_CARDS (must be an array):', payload);
              // Keep existing community cards if payload is invalid, or set to empty if none
              return {
                  ...prevGame,
                  communityCards: prevGame?.communityCards || []
              };
            }
            newGameState = {
              ...prevGame,
              communityCards: payload
            };
            break;

          default:
            console.warn('Unknown update type:', type);
            if (prevGame) {
              return { // Return previous state, ensuring critical arrays are present
                ...prevGame,
                communityCards: prevGame.communityCards || [],
                players: prevGame.players || []
              };
            }
            console.warn('Cannot process unknown update type with null prevGame');
            return null;
        }

        // Reset the updateAction after processing
        setUpdateActions(null);
        return newGameState;
      });
    }
  }, [updateAction]); // gameStatus is a constant object, currentPlayer changes are handled via setCurrentPlayer

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
    
    if (game.bigBlindUserId === player.username) {
      return 'big-blind';
    } else if (game.smallBlindUserId === player.username) {
      return 'small-blind';
    }
    return null;
  };

  if (loading) return <div className="loading-spinner">Loading game...</div>;
  if (error) return <div className="error">{error}</div>;

  if (!game) {
    console.error('Game data is missing');
    return <div>No game data available. Please try again later.</div>;
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
        {/* Winners Display */}
        {game && game.winners && game.winners.length > 0 && (
          <div className="winners-overlay" style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            backgroundColor: 'rgba(0, 0, 0, 0.9)',
            color: 'white',
            padding: '30px',
            borderRadius: '15px',
            zIndex: 1000, // Ensure it's on top
            textAlign: 'center',
            boxShadow: '0 0 20px rgba(255, 215, 0, 0.5)', // Gold-ish shadow
            minWidth: '300px',
            border: '1px solid rgba(255, 215, 0, 0.7)' // Gold-ish border
          }}>
            <h2 style={{ color: '#FFD700', marginBottom: '20px' }}>Winner{game.winners.length > 1 ? 's' : ''}!</h2>
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {game.winners.map((winner, index) => (
                <li key={winner.username || index} style={{ margin: '12px 0', fontSize: '1.1em' }}>
                  <strong>{winner.username}</strong>
                  {winner.handDescription && <span style={{ display: 'block', fontSize: '0.9em', color: '#ccc' }}>{`Hand: ${winner.handDescription}`}</span>}
                  {typeof winner.amountWon !== 'undefined' && winner.amountWon > 0 && <span style={{ display: 'block', fontSize: '1em', color: '#4CAF50' }}>{`Wins: ${winner.amountWon} chips`}</span>}
                </li>
              ))}
            </ul>
            <p style={{ marginTop: '25px', fontSize: '0.9em', color: '#aaa' }}>Next round starting soon...</p>
          </div>
        )}

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