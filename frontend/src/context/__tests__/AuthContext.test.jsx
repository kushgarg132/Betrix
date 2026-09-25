import { render, screen, waitFor } from '@testing-library/react';
import { MockedProvider } from '@apollo/client/testing/react';
import { useContext } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AuthProvider, { AuthContext } from '../AuthContext';
import { GET_ME } from '@/graphql/queries';

vi.mock('sonner', () => ({ toast: { error: vi.fn() } }));

function Probe() {
  const { isLoggedIn, user } = useContext(AuthContext);
  return <div>{isLoggedIn ? `in:${user?.name ?? '-'}` : 'out'}</div>;
}

const meMock = (me) => ({ request: { query: GET_ME }, result: { data: { me } } });

describe('AuthProvider', () => {
  beforeEach(() => localStorage.clear());

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
});
