import React, { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';

/**
 * Gate a route on being logged in. Redirects to /login (preserving the page the visitor wanted,
 * so Login can send them back) instead of rendering.
 *
 * Redirecting via <Navigate> rather than calling navigate() in a page component's render body
 * matters: navigate() during render is a side effect happening outside React's render cycle,
 * which is what AdminPanel used to do.
 */
export function ProtectedRoute({ children }) {
  const { isLoggedIn } = useContext(AuthContext);
  const location = useLocation();

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  return children;
}

/** ProtectedRoute, plus an admin role check. Non-admins are sent home, not to /login. */
export function AdminRoute({ children }) {
  const { isLoggedIn, user } = useContext(AuthContext);
  const location = useLocation();

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  const isAdmin = user?.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN');
  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }
  return children;
}
