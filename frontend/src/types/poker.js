/**
 * @typedef {Object} Card
 * @property {string} rank - The card rank (2-10, J, Q, K, A)
 * @property {string} suit - The card suit (hearts, diamonds, clubs, spades)
 */

/**
 * @typedef {Object} Player
 * @property {string} _id - Player ID
 * @property {string} id - Player ID
 * @property {string} username - Player username
 * @property {number} chips - Player's chips
 * @property {Card[]} [hand] - Player's hand (optional)
 * @property {boolean} hasFolded - Whether player has folded
 */

/**
 * @typedef {Object} BettingRound
 * @property {Object.<string, number>} playerBets - Map of player username to bet amount
 */

/**
 * @typedef {Object} Game
 * @property {string} _id - Game ID
 * @property {string} id - Game ID
 * @property {Player[]} players - Array of players
 * @property {Card[]} communityCards - Shared community cards
 * @property {number} pot - Current pot amount
 * @property {number} currentBet - Current bet amount
 * @property {number} currentPlayerIndex - Index of the current player
 * @property {string} status - Game status
 * @property {string} [winner] - Winner of the game (optional)
 * @property {string} [bigBlindPosition] - Username of player in big blind position (optional)
 * @property {string} [smallBlindPosition] - Username of player in small blind position (optional)
 * @property {BettingRound} [currentBettingRound] - Current betting round information (optional)
 */

/**
 * @typedef {Object} CurrentPlayer
 * @property {string} id - Player ID
 * @property {string} username - Player username
 * @property {Card[]} hand - Player's hand
 * @property {number} index - Player's index in the game players array
 */

export {}; 