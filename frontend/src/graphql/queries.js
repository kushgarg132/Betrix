import { gql } from '@apollo/client';

// ─── Fragments ────────────────────────────────────────

export const CARD_FIELDS = gql`
  fragment CardFields on Card {
    suit
    rank
  }
`;

export const PLAYER_FIELDS = gql`
  fragment PlayerFields on Player {
    id
    name
    username
    hand {
      ...CardFields
    }
    chips
    isActive
    currentBet
    hasFolded
    lastWinAmount
    isSittingOut
    bestHand {
      rank
      highCards {
        ...CardFields
      }
    }
    timeBankMs
    isAllIn
  }
  ${CARD_FIELDS}
`;

export const GAME_FIELDS = gql`
  fragment GameFields on Game {
    id
    players {
      ...PlayerFields
    }
    communityCards {
      ...CardFields
    }
    pot
    pots {
      amount
      eligiblePlayerIds
    }
    status
    currentBettingRound {
      bets
      roundType
    }
    dealerPosition
    currentPlayerIndex
    currentBet
    smallBlindAmount
    bigBlindAmount
    smallBlindUserId
    bigBlindUserId
    lastActions
    playerActionTimeoutSeconds
    currentPlayerActionDeadline
    maxPlayers
    playerCount
    isGameFull
    createdAt
    updatedAt
  }
  ${PLAYER_FIELDS}
`;

// ─── Queries ──────────────────────────────────────────

export const GET_ME = gql`
  query GetMe {
    me {
      id
      name
      username
      email
      balance
      roles
      handsPlayed
      handsWon
      winRate
      netProfit
    }
  }
`;

export const GET_GAMES = gql`
  query GetGames {
    games {
      id
      status
      playerCount
      maxPlayers
      smallBlindAmount
      bigBlindAmount
      pot
      createdAt
    }
  }
`;

export const GET_GAME = gql`
  query GetGame($id: ID!) {
    game(id: $id) {
      ...GameFields
    }
  }
  ${GAME_FIELDS}
`;

export const GET_GAME_FOR_PLAYER = gql`
  query GetGameForPlayer($gameId: ID!, $playerId: ID!) {
    gameForPlayer(gameId: $gameId, playerId: $playerId) {
      ...GameFields
    }
  }
  ${GAME_FIELDS}
`;

export const GET_GAME_EVENTS = gql`
  query GetGameEvents($gameId: ID!) {
    gameEvents(gameId: $gameId) {
      id
      gameId
      timestamp
      eventType
      eventData
    }
  }
`;
