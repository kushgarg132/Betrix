import { useQuery, useMutation, useSubscription } from '@apollo/client/react';
import { GET_GAME, GET_GAME_FOR_PLAYER } from '../graphql/queries';
import { JOIN_GAME, PLAYER_ACTION, LEAVE_GAME, SIT_OUT, SIT_IN, START_HAND, SEND_CHAT } from '../graphql/mutations';
import { GAME_UPDATED, PLAYER_UPDATED } from '../graphql/subscriptions';

/**
 * Hook that provides all game operations and real-time subscriptions.
 *
 * `playerId` (the caller's own seat, found locally by matching the logged-in username against
 * game.players) is only used here to decide which query/subscription to use and to skip the
 * player-private ones until it's known — it is never sent to the server. Every mutation and the
 * player-scoped query/subscription resolve the acting player from the auth token server-side.
 */
export function useGame(gameId, playerId) {
  const hasToken = !!localStorage.getItem('token');

  // Query: fetch game state — only when authenticated
  const {
    data: gameData,
    loading: gameLoading,
    error: gameError,
    refetch: refetchGame,
  } = useQuery(
    playerId ? GET_GAME_FOR_PLAYER : GET_GAME,
    {
      variables: playerId ? { gameId } : { id: gameId },
      skip: !gameId || !hasToken,
    }
  );

  // Subscription: game-wide updates — only when authenticated
  const { data: gameUpdateData } = useSubscription(GAME_UPDATED, {
    variables: { gameId },
    skip: !gameId || !hasToken,
  });

  // Subscription: player-specific updates (private hand)
  const { data: playerUpdateData } = useSubscription(PLAYER_UPDATED, {
    variables: { gameId },
    skip: !gameId || !playerId || !hasToken,
  });

  // Mutations
  const [joinGameMutation] = useMutation(JOIN_GAME);
  const [playerActionMutation] = useMutation(PLAYER_ACTION);
  const [leaveGameMutation] = useMutation(LEAVE_GAME);
  const [sitOutMutation] = useMutation(SIT_OUT);
  const [sitInMutation] = useMutation(SIT_IN);
  const [startHandMutation] = useMutation(START_HAND);
  const [sendChatMutation] = useMutation(SEND_CHAT);

  const joinGame = () => joinGameMutation({ variables: { gameId } });

  const doAction = (actionType, amount) =>
    playerActionMutation({
      variables: {
        gameId,
        input: { actionType, amount },
      },
    });

  const leaveGame = () => leaveGameMutation({ variables: { gameId } });
  const sitOut = () => sitOutMutation({ variables: { gameId } });
  const sitIn = () => sitInMutation({ variables: { gameId } });

  const startHand = () => startHandMutation({ variables: { gameId } });

  const sendChat = (message) =>
    sendChatMutation({ variables: { gameId, message } });

  return {
    game: gameData?.game || gameData?.gameForPlayer || null,
    gameLoading,
    gameError,
    refetchGame,
    gameUpdate: gameUpdateData?.gameUpdated || null,
    playerUpdate: playerUpdateData?.playerUpdated || null,
    joinGame,
    doAction,
    leaveGame,
    sitOut,
    sitIn,
    startHand,
    sendChat,
  };
}
