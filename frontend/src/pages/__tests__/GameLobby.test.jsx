import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { MockedProvider } from '@apollo/client/testing/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { GET_GAMES } from '@/graphql/queries';
import { CREATE_GAME } from '@/graphql/mutations';
import GameLobby from '../GameLobby';

// GameLobby renders a fixed "Create table" button and internally uses useMediaQuery
// (via CreateGameModal) to pick BottomSheet vs Dialog — jsdom has no real matchMedia.
beforeEach(() => {
  window.matchMedia = function () {
    return {
      matches: false,
      media: '',
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
    };
  };
});

const navigateMock = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => navigateMock };
});

const emptyGamesMock = {
  request: { query: GET_GAMES },
  result: { data: { games: [] } },
};

const createGameMock = {
  request: {
    query: CREATE_GAME,
    variables: { input: { smallBlindAmount: 5, bigBlindAmount: 10 } },
  },
  result: {
    data: {
      createGame: {
        __typename: 'Game',
        id: 'newgame1',
        players: [],
        communityCards: [],
        pot: 0,
        pots: [],
        status: 'WAITING',
        currentBettingRound: null,
        dealerPosition: 0,
        currentPlayerIndex: 0,
        currentBet: 0,
        smallBlindAmount: 5,
        bigBlindAmount: 10,
        smallBlindUserId: null,
        bigBlindUserId: null,
        lastActions: null,
        playerActionTimeoutSeconds: 30,
        currentPlayerActionDeadline: null,
        maxPlayers: 6,
        playerCount: 0,
        isGameFull: false,
        createdAt: null,
        updatedAt: null,
      },
    },
  },
};

async function createTable(mocks) {
  render(
    <MemoryRouter>
      <MockedProvider mocks={mocks}>
        <GameLobby />
      </MockedProvider>
    </MemoryRouter>
  );

  await waitFor(() => screen.getByText('No tables yet'));
  fireEvent.click(screen.getAllByRole('button', { name: /create table/i })[0]);
  await waitFor(() => screen.getByText('Custom blinds'));
  fireEvent.click(screen.getByText('5/10'));
  const submitButtons = screen.getAllByRole('button', { name: /create table/i });
  fireEvent.click(submitButtons[submitButtons.length - 1]);
}

describe('GameLobby create-table navigation', () => {
  it('navigates to the new game once createGame resolves with an id', async () => {
    // A failing background refetch is deliberately included: it must never block navigation
    // (this is the bug fixed in GameLobby.jsx — see task-8-report.md for the root cause).
    const refetchMock = { request: { query: GET_GAMES }, error: new Error('refetch network blip') };
    await createTable([emptyGamesMock, createGameMock, refetchMock]);

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/game/newgame1'));
  });

  it('still navigates when the post-create lobby refetch succeeds', async () => {
    const refetchMock = {
      request: { query: GET_GAMES },
      result: { data: { games: [{ id: 'newgame1', status: 'WAITING', playerCount: 0, maxPlayers: 6, smallBlindAmount: 5, bigBlindAmount: 10, pot: 0, createdAt: null, isYourGame: true }] } },
    };
    await createTable([emptyGamesMock, createGameMock, refetchMock]);

    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/game/newgame1'));
  });
});
