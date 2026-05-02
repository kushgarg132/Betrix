import { gql } from '@apollo/client';
import { GAME_FIELDS } from './queries';

export const LOGIN = gql`
  mutation Login($input: LoginInput!) {
    login(input: $input) {
      token
      type
    }
  }
`;

export const REGISTER = gql`
  mutation Register($input: RegisterInput!) {
    register(input: $input) {
      id
      name
      username
      email
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

export const ADD_BALANCE = gql`
  mutation AddBalance($amount: Int!) {
    addBalance(amount: $amount) {
      id
      balance
    }
  }
`;

export const PLAYER_ACTION = gql`
  mutation PlayerAction($gameId: ID!, $input: PlayerActionInput!) {
    playerAction(gameId: $gameId, input: $input)
  }
`;

export const START_HAND = gql`
  mutation StartHand($gameId: ID!) {
    startHand(gameId: $gameId)
  }
`;

export const SEND_CHAT = gql`
  mutation SendChat($gameId: ID!, $message: String!, $playerId: ID!) {
    sendChat(gameId: $gameId, message: $message, playerId: $playerId) {
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
