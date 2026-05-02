import { useQuery, useMutation, useSubscription } from '@apollo/client/react';
import { GET_GAME, GET_GAME_FOR_PLAYER } from '../graphql/queries';
import { JOIN_GAME, PLAYER_ACTION, START_HAND, SEND_CHAT } from '../graphql/mutations';
import { GAME_UPDATED, PLAYER_UPDATED } from '../graphql/subscriptions';

/**
 * Hook that provides all game operations and real-time subscriptions.
 * Replaces the manual STOMP setup and Axios calls in PokerTable.jsx.
 */
export function useGame(gameId, playerId) {
  // Query: fetch game state
  const {
    data: gameData,
    loading: gameLoading,
    error: gameError,
    refetch: refetchGame,
  } = useQuery(
    playerId ? GET_GAME_FOR_PLAYER : GET_GAME,
    {
      variables: playerId ? { gameId, playerId } : { id: gameId },
      skip: !gameId,
    }
  );

  // Subscription: game-wide updates
  const { data: gameUpdateData } = useSubscription(GAME_UPDATED, {
    variables: { gameId },
    skip: !gameId,
  });

  // Subscription: player-specific updates (private hand)
  const { data: playerUpdateData } = useSubscription(PLAYER_UPDATED, {
    variables: { gameId, playerId },
    skip: !gameId || !playerId,
  });

  // Mutations
  const [joinGameMutation] = useMutation(JOIN_GAME);
  const [playerActionMutation] = useMutation(PLAYER_ACTION);
  const [startHandMutation] = useMutation(START_HAND);
  const [sendChatMutation] = useMutation(SEND_CHAT);

  const joinGame = () => joinGameMutation({ variables: { gameId } });

  const doAction = (actionType, amount) =>
    playerActionMutation({
      variables: {
        gameId,
        input: { playerId, actionType, amount },
      },
    });

  const startHand = () => startHandMutation({ variables: { gameId } });

  const sendChat = (message) =>
    sendChatMutation({ variables: { gameId, message, playerId } });

  return {
    game: gameData?.game || gameData?.gameForPlayer || null,
    gameLoading,
    gameError,
    refetchGame,
    gameUpdate: gameUpdateData?.gameUpdated || null,
    playerUpdate: playerUpdateData?.playerUpdated || null,
    joinGame,
    doAction,
    startHand,
    sendChat,
  };
}
