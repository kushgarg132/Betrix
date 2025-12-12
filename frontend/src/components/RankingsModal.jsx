import React from 'react';
import './RankingsModal.css';

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

const RankingsModal = ({ showRankingsModal, toggleRankingsModal }) => {
  if (!showRankingsModal) return null;

  return (
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
  );
};

export default RankingsModal; 