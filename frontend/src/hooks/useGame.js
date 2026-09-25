import { useCallback, useEffect, useMemo, useReducer, useState } from 'react';
import { useMutation, useSubscription } from '@apollo/client/react';
import { JOIN_GAME, PLAYER_ACTION, LEAVE_GAME, SIT_OUT, SIT_IN, START_HAND, SEND_CHAT } from '../graphql/mutations';
import { GAME_UPDATED, PLAYER_UPDATED } from '../graphql/subscriptions';
import { heroIndex as findHero, initialTableState, tableReducer } from '../lib/gameReducer';

const BETTING = ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'];

/**
 * Everything the table screen needs: join, both subscriptions folded through tableReducer, the
 * hero's seat and turn, and the mutations. The acting player is always resolved server-side from
 * the auth token; nothing here sends a player id.
 */
export function useGame(gameId, username) {
  const [state, dispatch] = useReducer(tableReducer, initialTableState);
  const [status, setStatus] = useState('joining');
  const [error, setError] = useState(null);

  const [joinGame] = useMutation(JOIN_GAME);
  const [playerAction] = useMutation(PLAYER_ACTION);
  const [leaveGame] = useMutation(LEAVE_GAME);
  const [sitOutM] = useMutation(SIT_OUT);
  const [sitInM] = useMutation(SIT_IN);
  const [startHandM] = useMutation(START_HAND);
  const [sendChatM] = useMutation(SEND_CHAT);

  useEffect(() => {
    if (!gameId || !username) return;
    let cancelled = false;
    setStatus('joining');
    joinGame({ variables: { gameId } })
      .then(({ data }) => {
        if (cancelled) return;
        const g = data?.joinGame;
        if (!g || findHero(g, username) === -1) throw new Error('Could not take a seat at this table.');
        dispatch({ type: 'joined', game: g });
        setStatus('ready');
      })
      .catch((e) => { if (!cancelled) { setError(e.message || 'Failed to join the table.'); setStatus('error'); } });
    return () => { cancelled = true; };
  }, [gameId, username, joinGame]);

  const ready = status === 'ready';
  useSubscription(GAME_UPDATED, {
    variables: { gameId },
    skip: !ready,
    onData: ({ data }) => data.data?.gameUpdated && dispatch({ type: 'update', update: data.data.gameUpdated }),
  });
  useSubscription(PLAYER_UPDATED, {
    variables: { gameId },
    skip: !ready,
    onData: ({ data }) => data.data?.playerUpdated && dispatch({ type: 'update', update: data.data.playerUpdated }),
  });

  const heroIndex = findHero(state.game, username);
  const hero = heroIndex >= 0 ? state.game.players[heroIndex] : null;
  const isMyTurn = !!(hero && BETTING.includes(state.game.status)
    && state.game.currentPlayerIndex === heroIndex && !hero.hasFolded && !hero.isSittingOut);

  const vars = { variables: { gameId } };
  const actions = useMemo(() => ({
    bet: (amount) => playerAction({ variables: { gameId, input: { actionType: 'BET', amount } } }),
    check: () => playerAction({ variables: { gameId, input: { actionType: 'CHECK' } } }),
    fold: () => playerAction({ variables: { gameId, input: { actionType: 'FOLD' } } }),
    leave: () => leaveGame(vars),
    sitOut: () => sitOutM(vars),
    sitIn: () => sitInM(vars),
    startHand: () => startHandM(vars),
    sendChat: (message) => sendChatM({ variables: { gameId, message } }),
  }), [gameId]); // eslint-disable-line react-hooks/exhaustive-deps

  const clearShowdown = useCallback(() => dispatch({ type: 'update', update: { type: 'SHOWDOWN_SEEN' } }), []);

  return { status, error, ...state, clearShowdown, heroIndex, hero, isMyTurn, actions };
}
