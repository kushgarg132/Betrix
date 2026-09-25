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
  subscription PlayerUpdated($gameId: ID!) {
    playerUpdated(gameId: $gameId) {
      gameId
      type
      payload
      timestamp
    }
  }
`;
