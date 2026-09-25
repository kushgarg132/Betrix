export const initialTableState = { game: null, hand: [], chat: [], showdown: null };

// Reuses `g` by reference when it already carries both array fields (every real game payload
// does, via GameFields) so `toBe` identity holds across untouched dispatches; only clones to
// backfill defaults for a partial/malformed payload.
const withGame = (state, g) => ({
  ...state,
  game: g && g.communityCards !== undefined && g.players !== undefined
    ? g
    : { ...g, communityCards: g?.communityCards || [], players: g?.players || [] },
});

/** Pure: every subscription event goes through here, so React StrictMode double-invocation is harmless. */
export function tableReducer(state, action) {
  if (action.type === 'joined') return withGame(state, action.game);
  if (action.type !== 'update') return state;

  const { type, payload } = action.update;
  switch (type) {
    case 'CARDS_DEALT':
      return Array.isArray(payload) ? { ...state, hand: payload } : state;
    case 'CHAT_MESSAGE':
      return payload ? { ...state, chat: [...state.chat, payload] } : state;
    case 'COMMUNITY_CARDS':
      return Array.isArray(payload) ? { ...state, game: { ...state.game, communityCards: payload } } : state;
    case 'PLAYER_JOINED': {
      const players = state.game?.players || [];
      if (!payload || players.some((p) => p.id === payload.id)) return state;
      return { ...state, game: { ...state.game, players: [...players, payload] } };
    }
    case 'PLAYER_ACTION':
      return withGame(state, payload?.game || payload);
    case 'GAME_STARTED':
    case 'ROUND_STARTED':
      return { ...withGame(state, payload), showdown: null };
    case 'GAME_ENDED': {
      const g = payload?.game || payload;
      const next = withGame(state, { ...g, status: 'ENDED' });
      return {
        ...next,
        hand: [],
        showdown: { game: g, winners: payload?.winners || [], bestHand: payload?.bestHand || null },
      };
    }
    case 'SHOWDOWN_SEEN':
      return { ...state, showdown: null };
    default:
      return state;
  }
}

export function heroIndex(game, username) {
  return game?.players?.findIndex((p) => p.username === username) ?? -1;
}
