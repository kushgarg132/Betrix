import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import Register from '../Register';

vi.mock('@/hooks/useAuth', () => ({
  useAuth: () => ({
    register: vi.fn(),
    guestLogin: vi.fn().mockRejectedValue(new Error('Guest login failed.')),
  }),
}));

function renderRegister() {
  return render(
    <MemoryRouter>
      <AuthContext.Provider value={{ login: vi.fn() }}>
        <Register />
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('Register', () => {
  it('shows an error instead of crashing when guest login fails', async () => {
    const user = userEvent.setup();
    renderRegister();

    await user.click(screen.getByRole('button', { name: /continue as guest/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Guest login failed.');
    });
  });
});
