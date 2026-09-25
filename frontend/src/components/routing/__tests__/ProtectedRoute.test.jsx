import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext } from '@/context/AuthContext';
import { AdminRoute, ProtectedRoute } from '../ProtectedRoute';

function renderAt(path, authValue, element, routePath = path) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={authValue}>
        <Routes>
          <Route path={routePath} element={element} />
          <Route path="/login" element={<div>login page</div>} />
          <Route path="/" element={<div>home page</div>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  it('redirects a logged-out visitor to /login instead of rendering the page', () => {
    renderAt('/profile', { isLoggedIn: false, user: null },
      <ProtectedRoute><div>secret profile</div></ProtectedRoute>, '/profile');

    expect(screen.getByText('login page')).toBeInTheDocument();
    expect(screen.queryByText('secret profile')).not.toBeInTheDocument();
  });

  it('renders the page for a logged-in visitor', () => {
    renderAt('/profile', { isLoggedIn: true, user: { username: 'alice' } },
      <ProtectedRoute><div>secret profile</div></ProtectedRoute>, '/profile');

    expect(screen.getByText('secret profile')).toBeInTheDocument();
  });
});

describe('AdminRoute', () => {
  it('redirects a logged-out visitor to /login', () => {
    renderAt('/admin', { isLoggedIn: false, user: null },
      <AdminRoute><div>admin panel</div></AdminRoute>, '/admin');

    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('redirects a logged-in non-admin to / instead of rendering the panel', () => {
    renderAt('/admin', { isLoggedIn: true, user: { username: 'alice', roles: ['USER'] } },
      <AdminRoute><div>admin panel</div></AdminRoute>, '/admin');

    expect(screen.getByText('home page')).toBeInTheDocument();
    expect(screen.queryByText('admin panel')).not.toBeInTheDocument();
  });

  it('renders the panel for an admin', () => {
    renderAt('/admin', { isLoggedIn: true, user: { username: 'root', roles: ['ROLE_ADMIN'] } },
      <AdminRoute><div>admin panel</div></AdminRoute>, '/admin');

    expect(screen.getByText('admin panel')).toBeInTheDocument();
  });
});
