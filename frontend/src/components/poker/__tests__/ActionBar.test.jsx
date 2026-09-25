import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ActionBar from '../ActionBar';

vi.mock('../AddBotButton', () => ({ default: () => <button type="button">Add bot</button> }));

const hero = { id: 'p1', chips: 1000, hasFolded: false, isSittingOut: false };
const game = { id: 'g1', status: 'FLOP_BETTING', currentBet: 20, pot: 60, bigBlindAmount: 20,
  currentBettingRound: { bets: { p1: 0 } }, players: [hero] };
const actions = () => ({ bet: vi.fn(), check: vi.fn(), fold: vi.fn(), sitOut: vi.fn(), sitIn: vi.fn(), startHand: vi.fn() });

describe('ActionBar', () => {
  it('lets the hero act on their turn', async () => {
    const a = actions();
    render(<ActionBar game={game} hero={hero} heroIndex={0} isMyTurn online actions={a} />);
    await userEvent.click(screen.getByRole('button', { name: /fold/i }));
    expect(a.fold).toHaveBeenCalled();
    expect(screen.getByRole('button', { name: /call 20/i })).toBeEnabled();
  });

  it('disables betting while offline', () => {
    render(<ActionBar game={game} hero={hero} heroIndex={0} isMyTurn online={false} actions={actions()} />);
    expect(screen.getByRole('button', { name: /fold/i })).toBeDisabled();
  });

  it('shows no betting buttons off-turn, only sit out', () => {
    render(<ActionBar game={game} hero={hero} heroIndex={0} isMyTurn={false} online actions={actions()} />);
    expect(screen.queryByRole('button', { name: /fold/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sit out/i })).toBeInTheDocument();
  });

  it('offers start hand and add bot while waiting', () => {
    render(<ActionBar game={{ ...game, status: 'WAITING', players: [hero, { id: 'p2' }] }} hero={hero} heroIndex={0}
      isMyTurn={false} online actions={actions()} />);
    expect(screen.getByRole('button', { name: /start hand/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: /add bot/i })).toBeInTheDocument();
  });
});
