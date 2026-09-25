import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import PokerTable from '../PokerTable';

// I1: the showdown overlay must survive a next-hand update arriving mid-reveal (the live game
// object keeps changing underneath), and only clear on dismiss. Everything except PokerTable's
// own cache-the-payload logic is stubbed out so the test is about that logic, not rendering detail.
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => vi.fn(), useParams: () => ({ gameId: 'g1' }) };
});

const useGameMock = vi.fn();
vi.mock('@/hooks/useGame', () => ({ useGame: (...args) => useGameMock(...args) }));
vi.mock('@/hooks/useConnectionStatus', () => ({ useConnectionStatus: () => 'online' }));
vi.mock('@/hooks/useMediaQuery', () => ({ useMediaQuery: () => false }));
vi.mock('@/components/poker/TableScene', () => ({
  default: ({ showdown, onShowdownDone }) => (
    <div>
      {showdown && (
        <div>
          <span>Showdown: {showdown.marker}</span>
          <button type="button" onClick={onShowdownDone}>Dismiss</button>
        </div>
      )}
    </div>
  ),
}));
vi.mock('@/components/poker/ActionBar', () => ({ default: () => <div>ActionBar</div> }));
vi.mock('@/components/poker/TableTopBar', () => ({ default: () => <div>TopBar</div> }));
vi.mock('@/components/poker/ChatDrawer', () => ({ default: () => <div>Chat</div> }));

const baseGame = { id: 'g1', status: 'PRE_FLOP_BETTING', players: [{ id: 'p1', username: 'alice' }], communityCards: [] };

function baseT(overrides = {}) {
  return {
    status: 'ready', error: null, game: baseGame, hand: [], chat: [], showdown: null,
    clearShowdown: vi.fn(), heroIndex: 0, hero: baseGame.players[0], isMyTurn: false,
    actions: { leave: vi.fn(), sendChat: vi.fn() },
    ...overrides,
  };
}

function renderTable() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user: { username: 'alice' } }}>
        <PokerTable />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('PokerTable showdown reveal caching (I1)', () => {
  it('keeps the reveal visible through a next-hand update, and dismisses it on demand', () => {
    const clearShowdown = vi.fn();
    useGameMock.mockReturnValue(baseT());
    const { rerender } = renderTable();
    expect(screen.queryByText(/showdown:/i)).not.toBeInTheDocument();

    // GAME_ENDED arrives: showdown payload shows up live.
    const showdownPayload = { marker: 'hand-1', winners: [{ id: 'p1', name: 'Alice', lastWinAmount: 40 }] };
    useGameMock.mockReturnValue(baseT({ showdown: showdownPayload, clearShowdown }));
    rerender(<MemoryRouter><AuthContext.Provider value={{ user: { username: 'alice' } }}><PokerTable /></AuthContext.Provider></MemoryRouter>);
    expect(screen.getByText('Showdown: hand-1')).toBeInTheDocument();

    // GAME_STARTED for the next hand arrives mid-reveal: reducer clears t.showdown and the live
    // game object changes underneath, but the cached reveal must stay on screen.
    useGameMock.mockReturnValue(baseT({
      showdown: null, clearShowdown, game: { ...baseGame, status: 'FLOP_BETTING', communityCards: ['AS'] },
    }));
    rerender(<MemoryRouter><AuthContext.Provider value={{ user: { username: 'alice' } }}><PokerTable /></AuthContext.Provider></MemoryRouter>);
    expect(screen.getByText('Showdown: hand-1')).toBeInTheDocument();

    // Dismiss: the overlay goes away and the live update is acknowledged.
    fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));
    expect(screen.queryByText(/showdown:/i)).not.toBeInTheDocument();
    expect(clearShowdown).toHaveBeenCalled();
  });
});
