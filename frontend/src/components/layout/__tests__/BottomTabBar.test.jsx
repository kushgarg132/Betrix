import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import BottomTabBar from '../BottomTabBar';

const renderAt = (path, user) => render(
  <MemoryRouter initialEntries={[path]}>
    <AuthContext.Provider value={{ isLoggedIn: !!user, user }}>
      <BottomTabBar />
    </AuthContext.Provider>
  </MemoryRouter>
);

describe('BottomTabBar', () => {
  it('shows Lobby and Profile, and Admin only for admins', () => {
    renderAt('/lobby', { roles: ['USER'] });
    expect(screen.getByRole('link', { name: /lobby/i })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: /profile/i })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /admin/i })).not.toBeInTheDocument();
  });

  it('adds Admin for admins', () => {
    renderAt('/lobby', { roles: ['ADMIN'] });
    expect(screen.getByRole('link', { name: /admin/i })).toBeInTheDocument();
  });

  it('is hidden on the sign-in screen and at the table', () => {
    const { container } = renderAt('/game/g1', { roles: ['USER'] });
    expect(container).toBeEmptyDOMElement();
  });
});
