import React, { useEffect, useState, useContext, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from '../api/axios';
import { AuthContext } from '../context/AuthContext';
import API_CONFIG from '../config/apiConfig';
import './PokerTable.css';
import PlayerCard from '../components/PlayerCard';

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

// Function to get card face for court cards
const getCardFace = (rank, suit) => {
  if (['J', 'Q', 'K', 'A'].includes(rank)) {
    return (
      <div className="card-face">
        {rank === 'K' && <div className="card-king"></div>}
        {rank === 'Q' && <div className="card-queen"></div>}
        {rank === 'J' && <div className="card-jack"></div>}
        {rank === 'A' && <div className="card-ace">{getSuitSymbol(suit)}</div>}
      </div>
    );
  }
  return null;
};

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

// Poker hand rankings for the modal
const pokerHandRankings = [
  { 
    name: "Royal Flush", 
    description: "A, K, Q, J, 10, all the same suit", 
    rank: 1,
    example: [
      { rank: "A", suit: "spades" },
      { rank: "K", suit: "spades" },
      { rank: "Q", suit: "spades" },
      { rank: "J", suit: "spades" },
      { rank: "10", suit: "spades" }
    ]
  },
  { 
    name: "Straight Flush", 
    description: "Five cards in a sequence, all in the same suit", 
    rank: 2,
    example: [
      { rank: "9", suit: "hearts" },
      { rank: "8", suit: "hearts" },
      { rank: "7", suit: "hearts" },
      { rank: "6", suit: "hearts" },
      { rank: "5", suit: "hearts" }
    ]
  },
  { 
    name: "Four of a Kind", 
    description: "All four cards of the same rank", 
    rank: 3,
    example: [
      { rank: "Q", suit: "spades" },
      { rank: "Q", suit: "hearts" },
      { rank: "Q", suit: "diamonds" },
      { rank: "Q", suit: "clubs" },
      { rank: "7", suit: "spades" }
    ]
  },
  { 
    name: "Full House", 
    description: "Three of a kind with a pair", 
    rank: 4,
    example: [
      { rank: "K", suit: "spades" },
      { rank: "K", suit: "hearts" },
      { rank: "K", suit: "diamonds" },
      { rank: "3", suit: "clubs" },
      { rank: "3", suit: "spades" }
    ]
  },
  { 
    name: "Flush", 
    description: "Any five cards of the same suit, but not in a sequence", 
    rank: 5,
    example: [
      { rank: "A", suit: "clubs" },
      { rank: "J", suit: "clubs" },
      { rank: "8", suit: "clubs" },
      { rank: "6", suit: "clubs" },
      { rank: "2", suit: "clubs" }
    ]
  },
  { 
    name: "Straight", 
    description: "Five cards in a sequence, but not of the same suit", 
    rank: 6,
    example: [
      { rank: "8", suit: "spades" },
      { rank: "7", suit: "hearts" },
      { rank: "6", suit: "diamonds" },
      { rank: "5", suit: "clubs" },
      { rank: "4", suit: "spades" }
    ]
  },
  { 
    name: "Three of a Kind", 
    description: "Three cards of the same rank", 
    rank: 7,
    example: [
      { rank: "J", suit: "spades" },
      { rank: "J", suit: "hearts" },
      { rank: "J", suit: "diamonds" },
      { rank: "8", suit: "clubs" },
      { rank: "2", suit: "spades" }
    ]
  },
  { 
    name: "Two Pair", 
    description: "Two different pairs", 
    rank: 8,
    example: [
      { rank: "10", suit: "spades" },
      { rank: "10", suit: "hearts" },
      { rank: "9", suit: "diamonds" },
      { rank: "9", suit: "clubs" },
      { rank: "A", suit: "spades" }
    ]
  },
  { 
    name: "Pair", 
    description: "Two cards of the same rank", 
    rank: 9,
    example: [
      { rank: "5", suit: "spades" },
      { rank: "5", suit: "hearts" },
      { rank: "K", suit: "diamonds" },
      { rank: "Q", suit: "clubs" },
      { rank: "6", suit: "spades" }
    ]
  },
  { 
    name: "High Card", 
    description: "When you haven't made any of the hands above, the highest card plays", 
    rank: 10,
    example: [
      { rank: "A", suit: "spades" },
      { rank: "K", suit: "hearts" },
      { rank: "10", suit: "diamonds" },
      { rank: "4", suit: "clubs" },
      { rank: "2", suit: "spades" }
    ]
  }
];

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

  // Function to render hidden cards for opponents
  const renderOpponentCards = () => {
    return (
      <div className="opponent-cards">
        <div className="card hidden"></div>
        <div className="card hidden"></div>
      </div>
    );
  };

  // Function to render 3D chips - updated to be smaller
  const renderChips = (amount) => {
    if (!amount || amount <= 0) return null;
    
    // Calculate how many chips to show based on amount
    const chipCount = Math.min(Math.ceil(amount / 20), 4); // Fewer chips
    const chips = [];
    
    for (let i = 0; i < chipCount; i++) {
      chips.push(
        <div 
          key={i} 
          className="poker-chip" 
          ref={el => {
            if (chipRefs.current.length < chipCount) {
              chipRefs.current.push(el);
            }
          }}
          style={{
            transform: `translateY(${i * -3}px)`, // Stack closer together
            zIndex: i
          }}
        />
      );
    }
    
    return (
      <div className="chip-stack">
        {chips}
      </div>
    );
  };

  // Function to calculate relative position for display
  const getDisplayPosition = (actualIndex) => {
    if (!game || currentPlayer.index === null) return 0;
    
    const playerCount = game.players.length;
    // Calculate position relative to current player
    let relativePosition = (actualIndex - currentPlayer.index + playerCount) % playerCount;
    return relativePosition;
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

  if (loading) return <div className="loading-spinner"></div>;
  if (error) return <div className="error">{error}</div>;

  if (!game || !game.communityCards || !game.players) {
    console.error('Game data is incomplete:', game);
    return <div>No game data available.</div>;
  }

  return (
    <div className="poker-table">
      <div className={`table-felt ${isMyTurn ? 'my-turn' : ''}`}>
        <div className="wood-border">
          <div className="circular-table">
            <div className="pot-area">
              {renderChips(game.pot)}
              <div className="pot-label">Pot: ${game.pot}</div>
            </div>

            <div className="community-cards">
              {game.communityCards.map((card, index) => (
                <div key={index} className={`card realistic ${card.suit.toLowerCase()}`}>
                  <div className="card-top">
                    <span>{card.rank}</span>
                    <span>{getSuitSymbol(card.suit)}</span>
                  </div>
                  {getCardFace(card.rank, card.suit)}
                  <div className="card-center">{card.rank !== 'A' && card.rank !== 'K' && card.rank !== 'Q' && card.rank !== 'J' && getSuitSymbol(card.suit)}</div>
                  <div className="card-bottom">
                    <span>{getSuitSymbol(card.suit)}</span>
                    <span>{card.rank}</span>
                  </div>
                </div>
              ))}
            </div>

            {game.players.map((player, index) => {
              const displayPosition = getDisplayPosition(index);
              const blindStatus = getBlindStatus(player);
              const playerBet = game.currentBettingRound?.playerBets[player.username] || 0;
              const isCurrentTurn = game.currentPlayerIndex === index;
              
              return (
                <div
                  key={player._id}
                  className={`player ${blindStatus ? blindStatus : ''} ${game.players.length}-players ${isCurrentTurn ? 'current-turn' : ''}`}
                  data-position={displayPosition}
                  style={{
                    opacity: player.hasFolded ? 0.5 : 1,
                    border: isCurrentTurn ? '2px solid #f1c40f' : 'none',
                  }}
                >
                  <div className="player-name">{player.username}</div>
                  
                  {player.username === currentPlayer.username ? (
                    <div className="player-cards">
                      {currentPlayer.hand?.map((card, idx) => (
                        <div key={idx} className={`card realistic ${card.suit.toLowerCase()}`}>
                          <div className="card-top">
                            <span>{card.rank}</span>
                            <span>{getSuitSymbol(card.suit)}</span>
                          </div>
                          {getCardFace(card.rank, card.suit)}
                          <div className="card-center">{card.rank !== 'A' && card.rank !== 'K' && card.rank !== 'Q' && card.rank !== 'J' && getSuitSymbol(card.suit)}</div>
                          <div className="card-bottom">
                            <span>{getSuitSymbol(card.suit)}</span>
                            <span>{card.rank}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    // Show hidden cards for opponents unless they've folded
                    !player.hasFolded && renderOpponentCards()
                  )}
                  
                  <div className="player-info-container">
                    <div className="chips">${player.chips}</div>
                    <div className="player-bet">Played: ${playerBet}</div>
                    {playerBet > 0 && (
                      <div className="bet-chips">
                        {renderChips(playerBet)}
                      </div>
                    )}
                    {blindStatus && (
                      <div className={`blind-indicator ${blindStatus}`}>
                        {blindStatus === 'big-blind' ? 'BB' : 'SB'}
                      </div>
                    )}
                  </div>
                  
                  {/* Show prominent bet amount for opponents */}
                  {player.username !== currentPlayer.username && playerBet > 0 && (
                    <div className="opponent-bet-display">${playerBet}</div>
                  )}
                </div>
              );
            })}

            {/* Animate chips flying to pot when someone bets */}
            {animatingChips && lastAction && (
              <div className="animated-chips">
                {[...Array(6)].map((_, i) => (
                  <div 
                    key={i} 
                    className="flying-chip"
                    style={{
                      animationDelay: `${i * 0.08}s`,
                      animationDuration: `${0.6 + Math.random() * 0.4}s`,
                      left: `${30 + Math.random() * 40}%`
                    }}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Game info panel with 3D effects */}
      <div className="game-info">
        <div className="info-content">
          <h3>Game Information</h3>
          <div className="info-item">
            <span className="info-label">Pot:</span>
            <span className="info-value">${game.pot}</span>
          </div>
          <div className="info-item">
            <span className="info-label">Current Player:</span>
            <span className="info-value">{game.players[game.currentPlayerIndex]?.username || 'N/A'}</span>
          </div>
          {game.bigBlindPosition && (
            <div className="info-item">
              <span className="info-label">Big Blind:</span>
              <span className="info-value">{game.bigBlindPosition}</span>
            </div>
          )}
          {game.smallBlindPosition && (
            <div className="info-item">
              <span className="info-label">Small Blind:</span>
              <span className="info-value">{game.smallBlindPosition}</span>
            </div>
          )}
          <button className="rankings-button" onClick={toggleRankingsModal}>
            <span className="button-icon">♠</span>
            <span>Hand Rankings</span>
          </button>
        </div>
      </div>

      {/* Game status bar for mobile - shows key info during turn */}
      {isMyTurn && (
        <div className="game-status-bar">
          <div className="status-info">
            <span>Your turn! Pot: ${game.pot}</span>
            <span>Current Bet: ${game.currentBet}</span>
          </div>
        </div>
      )}

      {/* Render betting controls OUTSIDE the table when it's player's turn - now horizontal */}
      {isMyTurn && (
        <div className="betting-controls-wrapper">
          <div className="betting-controls-container">
            {game.currentBet === (game.currentBettingRound?.playerBets[currentPlayer.username] || 0) && (
              <button className="check-button" onClick={check}>
                <span className="action-text">Check</span>
                <span className="action-amount">$0</span>
              </button>
            )}
            
            {game.currentBet > (game.currentBettingRound?.playerBets[currentPlayer.username] || 0) && (
              <button 
                className="call-button" 
                onClick={() => placeBet(game.currentBet - (game.currentBettingRound?.playerBets[currentPlayer.username] || 0))}
              >
                <span className="action-text">Call</span>
                <span className="action-amount">
                  ${game.currentBet - (game.currentBettingRound?.playerBets[currentPlayer.username] || 0)}
                </span>
              </button>
            )}

            <button 
              className="raise-button" 
              onClick={toggleRaiseSlider}
            >
              <span className="action-text">Raise</span>
              <span className="action-amount">Min ${Math.max(game.currentBet + 1, 1)}</span>
            </button>

            <button className="fold-button" onClick={fold}>
              <span className="action-text">Fold</span>
            </button>
          </div>
        </div>
      )}

      {/* Sleek raise slider component */}
      {showRaiseSlider && (
        <div className="raise-slider-overlay" onClick={() => setShowRaiseSlider(false)}>
          <div className="raise-controls" onClick={(e) => e.stopPropagation()}>
            <div className="raise-header">
              <span>Raise Amount</span>
              <button className="close-button" onClick={toggleRaiseSlider}>×</button>
            </div>
            <div className="raise-slider-container">
              <input
                type="range"
                className="raise-slider"
                min={Math.max(game.currentBet + 1, 1)}
                max={Math.min(currentPlayer.chips, 1000)}
                value={raiseAmount}
                onChange={(e) => handleRaiseChange(parseInt(e.target.value))}
              />
              <div className="slider-track"></div>
            </div>
            <div className="raise-info">
              <span>Min: ${Math.max(game.currentBet + 1, 1)}</span>
              <span className="raise-amount">${raiseAmount}</span>
              <span>Max: ${Math.min(currentPlayer.chips, 1000)}</span>
            </div>
            <div className="raise-buttons">
              <button 
                className="confirm-raise-button" 
                onClick={() => {
                  placeBet(raiseAmount);
                  setShowRaiseSlider(false);
                }}
              >
                <span className="action-text">Raise to</span>
                <span className="action-amount">${raiseAmount}</span>
              </button>
              <button 
                className="cancel-raise-button" 
                onClick={cancelRaise}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Poker Hand Rankings Modal with visual card combinations */}
      {showRankingsModal && (
        <div className="rankings-modal-overlay">
          <div className="rankings-modal">
            <div className="rankings-header">
              <h3>Poker Hand Rankings</h3>
              <button className="close-button" onClick={toggleRankingsModal}>×</button>
            </div>
            <div className="rankings-content">
              <table className="rankings-table">
                <thead>
                  <tr>
                    <th>Rank</th>
                    <th>Hand</th>
                    <th>Description</th>
                    <th>Example</th>
                  </tr>
                </thead>
                <tbody>
                  {pokerHandRankings.map((hand) => (
                    <tr key={hand.rank}>
                      <td>{hand.rank}</td>
                      <td>{hand.name}</td>
                      <td>{hand.description}</td>
                      <td className="hand-example">
                        <div className="example-cards">
                          {hand.example && hand.example.map((card, i) => (
                            <div key={i} className={`mini-card ${card.suit}`}>
                              <span>{card.rank}</span>
                            </div>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PokerTable;