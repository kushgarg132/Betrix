import { act, render, screen, waitFor } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing/react';
import { useContext } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AuthProvider, { AuthContext } from '../AuthContext';
import { GET_ME } from '@/graphql/queries';
import { toast } from 'sonner';

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

function Probe() {
  const { isLoggedIn, user } = useContext(AuthContext);
  return <div>{isLoggedIn ? `in:${user?.name ?? '-'}` : 'out'}</div>;
}

const meMock = (me) => ({ request: { query: GET_ME }, result: { data: { me } } });

describe('AuthProvider', () => {
  beforeEach(() => { localStorage.clear(); toast.error.mockClear(); });

  it('loads the user for a stored token', async () => {
    localStorage.setItem('token', 't');
    const me = { __typename: 'User', id: 'u1', name: 'Alice', username: 'google-1', email: null, avatarUrl: null,
      roles: ['USER'], handsPlayed: 0, handsWon: 0, winRate: 0, netProfit: 0 };
    render(<MockedProvider mocks={[meMock(me)]}><AuthProvider><Probe /></AuthProvider></MockedProvider>);

    await waitFor(() => expect(screen.getByText('in:Alice')).toBeInTheDocument());
  });

  it('logs out when the stored token no longer resolves to a user', async () => {
    localStorage.setItem('token', 'expired');
    render(<MockedProvider mocks={[meMock(null)]}><AuthProvider><Probe /></AuthProvider></MockedProvider>);

    await waitFor(() => expect(screen.getByText('out')).toBeInTheDocument());
    expect(localStorage.getItem('token')).toBeNull();
  });

  // I3: a GET_ME query error (network blip, backend restart, 5xx) is not the same thing as the
  // server authoritatively saying `me: null` - it must not clear a perfectly good token/session.
  it('keeps the token and user on a GET_ME query error, and does not toast', async () => {
    localStorage.setItem('token', 'still-good');
    localStorage.setItem('user', JSON.stringify({ name: 'Cached' }));
    const errorMock = { request: { query: GET_ME }, error: new Error('network blip') };
    render(<MockedProvider mocks={[errorMock]}><AuthProvider><Probe /></AuthProvider></MockedProvider>);
    expect(screen.getByText('in:Cached')).toBeInTheDocument();

    // Let the mocked query actually resolve (with an error) and any resulting effect run.
    await act(async () => { await new Promise((r) => setTimeout(r, 20)); });

    expect(screen.getByText('in:Cached')).toBeInTheDocument();
    expect(localStorage.getItem('token')).toBe('still-good');
    expect(toast.error).not.toHaveBeenCalled();
  });
});
