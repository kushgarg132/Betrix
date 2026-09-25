import { gql } from '@apollo/client';
import { GAME_FIELDS } from './queries';

export const GOOGLE_LOGIN = gql`
  mutation GoogleLogin($idToken: String!) {
    googleLogin(idToken: $idToken) {
      token
      type
    }
  }
`;

export const GUEST_LOGIN = gql`
  mutation GuestLogin {
    guestLogin {
      token
      type
    }
  }
`;

export const CREATE_GAME = gql`
  mutation CreateGame($input: BlindInput!) {
    createGame(input: $input) {
      ...GameFields
    }
  }
  ${GAME_FIELDS}
`;

export const JOIN_GAME = gql`
  mutation JoinGame($gameId: ID!) {
    joinGame(gameId: $gameId) {
      ...GameFields
    }
  }
  ${GAME_FIELDS}
`;

export const DELETE_GAME = gql`
  mutation DeleteGame($gameId: ID!) {
    deleteGame(gameId: $gameId)
  }
`;

export const PLAYER_ACTION = gql`
  mutation PlayerAction($gameId: ID!, $input: PlayerActionInput!) {
    playerAction(gameId: $gameId, input: $input)
  }
`;

export const LEAVE_GAME = gql`
  mutation LeaveGame($gameId: ID!) {
    leaveGame(gameId: $gameId)
  }
`;

export const SIT_OUT = gql`
  mutation SitOut($gameId: ID!) {
    sitOut(gameId: $gameId)
  }
`;

export const SIT_IN = gql`
  mutation SitIn($gameId: ID!) {
    sitIn(gameId: $gameId)
  }
`;

export const START_HAND = gql`
  mutation StartHand($gameId: ID!) {
    startHand(gameId: $gameId)
  }
`;

export const SEND_CHAT = gql`
  mutation SendChat($gameId: ID!, $message: String!) {
    sendChat(gameId: $gameId, message: $message) {
      senderId
      senderName
      message
      timestamp
    }
  }
`;

export const ADD_BOT = gql`
  mutation AddBot($gameId: ID!, $difficulty: BotDifficulty) {
    addBot(gameId: $gameId, difficulty: $difficulty) {
      id
      username
      chips
      isBot
      botDifficulty
    }
  }
`;

export const REMOVE_BOT = gql`
  mutation RemoveBot($gameId: ID!, $botPlayerId: ID!) {
    removeBot(gameId: $gameId, botPlayerId: $botPlayerId)
  }
`;
