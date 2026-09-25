import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import Home from '../Home';

const guestLogin = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({ guestLogin, googleLogin: vi.fn() }),
}));
vi.mock('@/components/auth/GoogleSignInButton', () => ({
  default: () => <button type="button">Continue with Google</button>,
}));

function renderHome(ctx = {}) {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ isLoggedIn: false, login: vi.fn(), ...ctx }}>
        <Home />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('Home', () => {
  it('offers exactly Google sign-in and guest play', () => {
    renderHome();
    expect(screen.getByRole('button', { name: /continue with google/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /play as guest/i })).toBeInTheDocument();
    expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
  });

  it('shows an error instead of crashing when guest login fails', async () => {
    guestLogin.mockRejectedValueOnce(new Error('Guest login failed.'));
    const user = userEvent.setup();
    renderHome();

    await user.click(screen.getByRole('button', { name: /play as guest/i }));

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Guest login failed.'));
  });

  it('hands a successful guest token to AuthContext', async () => {
    guestLogin.mockResolvedValueOnce({ token: 'jwt-1' });
    const login = vi.fn();
    const user = userEvent.setup();
    renderHome({ login });

    await user.click(screen.getByRole('button', { name: /play as guest/i }));

    await waitFor(() => expect(login).toHaveBeenCalledWith('jwt-1'));
  });
});
