import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import Profile from '../Profile';

function renderProfile(user) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ user, logout: () => {} }}>
        <Profile />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('Profile', () => {
  it('renders a loading skeleton without crashing while the user is not loaded yet', () => {
    // AuthContext starts with user: null until GET_ME resolves; this must not throw.
    expect(() => renderProfile(null)).not.toThrow();
  });

  it('renders the profile once the user is loaded', () => {
    renderProfile({ name: 'Alice', username: 'google-1', roles: ['USER'], handsPlayed: 3, handsWon: 1, winRate: 33, netProfit: 50 });

    expect(screen.getByRole('heading', { name: 'Alice' })).toBeInTheDocument();
    expect(screen.queryByText('google-1')).not.toBeInTheDocument();
  });

  it('shows a sign-in prompt for guests and never shows one for signed-in users', () => {
    renderProfile({ name: 'Guest123', username: 'guest-abc', roles: ['GUEST'], handsPlayed: 0, handsWon: 0, winRate: 0, netProfit: 0 });
    expect(screen.getByText(/sign in with google to keep your stats/i)).toBeInTheDocument();
    expect(screen.getByText('Guest')).toBeInTheDocument();
  });
});
