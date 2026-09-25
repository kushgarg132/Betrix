import { describe, expect, it } from 'vitest';
import { heroIndex, initialTableState, tableReducer } from '../gameReducer';

const game = { id: 'g1', players: [{ id: 'p1', username: 'alice' }, { id: 'p2', username: 'bot-1' }], communityCards: [] };
const update = (type, payload) => ({ type: 'update', update: { type, payload } });
const joined = tableReducer(initialTableState, { type: 'joined', game });

describe('tableReducer', () => {
  it('stores the joined game', () => {
    expect(joined.game).toBe(game);
  });

  it('keeps the private hand from CARDS_DEALT without touching the game', () => {
    const cards = [{ suit: 'HEARTS', rank: 'ACE' }, { suit: 'SPADES', rank: 'KING' }];
    const s = tableReducer(joined, update('CARDS_DEALT', cards));
    expect(s.hand).toEqual(cards);
    expect(s.game).toBe(game);
  });

  it('appends chat from CHAT_MESSAGE (the type the backend actually sends)', () => {
    const msg = { senderId: 'p2', senderName: 'Bot', message: 'gl', timestamp: '2026-09-25T10:00:00Z' };
    expect(tableReducer(joined, update('CHAT_MESSAGE', msg)).chat).toEqual([msg]);
  });

  it('replaces community cards', () => {
    const cc = [{ suit: 'CLUBS', rank: 'TWO' }];
    expect(tableReducer(joined, update('COMMUNITY_CARDS', cc)).game.communityCards).toEqual(cc);
  });

  it('adds a joining player once, even if the event repeats', () => {
    const p3 = { id: 'p3', username: 'carol' };
    const s = tableReducer(tableReducer(joined, update('PLAYER_JOINED', p3)), update('PLAYER_JOINED', p3));
    expect(s.game.players.map((p) => p.id)).toEqual(['p1', 'p2', 'p3']);
  });

  it('takes the full game from PLAYER_ACTION, GAME_STARTED and ROUND_STARTED', () => {
    const next = { ...game, pot: 40 };
    for (const t of ['PLAYER_ACTION', 'GAME_STARTED', 'ROUND_STARTED']) {
      expect(tableReducer(joined, update(t, next)).game.pot).toBe(40);
    }
  });

  it('records a showdown from GAME_ENDED and clears the old hand', () => {
    const ended = { ...game, pot: 0 };
    const bestHand = { rank: 'FLUSH', highCards: [{ suit: 'HEARTS', rank: 'ACE' }] };
    const withHand = tableReducer(joined, update('CARDS_DEALT', [{ suit: 'HEARTS', rank: 'ACE' }]));
    const s = tableReducer(withHand, update('GAME_ENDED', { game: ended, winners: [game.players[0]], bestHand }));
    expect(s.showdown).toEqual({ game: ended, winners: [game.players[0]], bestHand });
    expect(s.game.status).toBe('ENDED');
    expect(s.hand).toEqual([]);
  });

  it('a new hand starting clears the previous showdown', () => {
    const s = tableReducer(
      tableReducer(joined, update('GAME_ENDED', { game, winners: [], bestHand: null })),
      update('GAME_STARTED', game));
    expect(s.showdown).toBeNull();
  });

  it('ignores unknown update types', () => {
    expect(tableReducer(joined, update('SOMETHING_NEW', {}))).toBe(joined);
  });

  it('clears the showdown on SHOWDOWN_SEEN', () => {
    expect(tableReducer({ ...joined, showdown: {} }, update('SHOWDOWN_SEEN')).showdown).toBeNull();
  });
});

describe('heroIndex', () => {
  it('finds the seat by username', () => {
    expect(heroIndex(game, 'bot-1')).toBe(1);
    expect(heroIndex(game, 'nobody')).toBe(-1);
    expect(heroIndex(null, 'alice')).toBe(-1);
  });
});
