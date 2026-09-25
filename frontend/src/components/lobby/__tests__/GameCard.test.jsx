import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { MockedProvider } from '@apollo/client/testing/react';
import { describe, expect, it } from 'vitest';
import GameCard from '../GameCard';

const baseGame = { id: 'g1', status: 'WAITING', playerCount: 2, maxPlayers: 6, smallBlindAmount: 10, bigBlindAmount: 20 };

function renderCard(game, onJoin = () => {}) {
  return render(
    <MemoryRouter>
      <MockedProvider mocks={[]}>
        <GameCard game={game} onJoin={onJoin} />
      </MockedProvider>
    </MemoryRouter>
  );
}

describe('GameCard join button', () => {
  it('offers to join an open table you are not seated at', () => {
    renderCard(baseGame);
    const button = screen.getByRole('button', { name: /join/i });
    expect(button).toBeEnabled();
    expect(screen.getByText('Join')).toBeInTheDocument();
  });

  it('a full table you are not seated at is disabled and says so honestly, not "Spectate"', () => {
    renderCard({ ...baseGame, playerCount: 6 });

    expect(screen.queryByText('Spectate')).not.toBeInTheDocument();
    const button = screen.getByRole('button', { name: /full/i });
    expect(button).toBeDisabled();
    expect(screen.getByText('Full')).toBeInTheDocument();
  });

  it('offers to return to a full table you are already seated at', () => {
    renderCard({ ...baseGame, playerCount: 6, isYourGame: true });

    const button = screen.getByRole('button', { name: /rejoin/i });
    expect(button).toBeEnabled();
  });

  it('renders one seat dot per seat, filled for taken seats', () => {
    render(<MemoryRouter><GameCard game={baseGame} onJoin={() => {}} /></MemoryRouter>);
    const dots = screen.getAllByTestId('seat-dot');
    expect(dots).toHaveLength(6);
    expect(dots.filter((d) => d.dataset.filled === 'true')).toHaveLength(2);
  });

  it('shows blinds without a currency sign', () => {
    render(<MemoryRouter><GameCard game={baseGame} onJoin={() => {}} /></MemoryRouter>);
    expect(screen.getByText('10/20')).toBeInTheDocument();
    expect(screen.queryByText(/\$/)).not.toBeInTheDocument();
  });
});
