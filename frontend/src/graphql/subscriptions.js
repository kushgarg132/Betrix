import { gql } from '@apollo/client';

export const GAME_UPDATED = gql`
  subscription GameUpdated($gameId: ID!) {
    gameUpdated(gameId: $gameId) {
      gameId
      type
      payload
      timestamp
    }
  }
`;

export const PLAYER_UPDATED = gql`
  subscription PlayerUpdated($gameId: ID!, $playerId: ID!) {
    playerUpdated(gameId: $gameId, playerId: $playerId) {
      gameId
      type
      payload
      timestamp
    }
  }
`;
