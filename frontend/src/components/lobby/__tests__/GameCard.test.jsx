import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { MockedProvider } from '@apollo/client/testing/react';
import { describe, expect, it } from 'vitest';
import GameCard from '../GameCard';

const baseGame = { id: 'g1', status: 'WAITING', playerCount: 2, maxPlayers: 6, smallBlindAmount: 10, bigBlindAmount: 20 };

function renderCard(game, isPlayerInGame) {
  return render(
    <MemoryRouter>
      <MockedProvider mocks={[]}>
        <GameCard game={game} index={0} isPlayerInGame={isPlayerInGame} />
      </MockedProvider>
    </MemoryRouter>
  );
}

describe('GameCard join button', () => {
  it('offers to join an open table you are not seated at', () => {
    renderCard(baseGame, false);
    expect(screen.getByRole('button', { name: /join game/i })).toBeEnabled();
    expect(screen.getByText('Join Table')).toBeInTheDocument();
  });

  it('a full table you are not seated at is disabled and says so honestly, not "Spectate"', () => {
    renderCard({ ...baseGame, playerCount: 6 }, false);

    expect(screen.queryByText('Spectate')).not.toBeInTheDocument();
    const button = screen.getByRole('button', { name: /table is full/i });
    expect(button).toBeDisabled();
    expect(screen.getByText('Table Full')).toBeInTheDocument();
  });

  it('offers to return to a full table you are already seated at', () => {
    renderCard({ ...baseGame, playerCount: 6 }, true);

    const button = screen.getByRole('button', { name: /return to your table/i });
    expect(button).toBeEnabled();
    expect(screen.getByText('Your table')).toBeInTheDocument();
  });
});
