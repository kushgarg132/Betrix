import React, { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';

/**
 * Gate a route on being logged in. Redirects to / (Home, preserving the page the visitor wanted,
 * so it can send them back after sign-in) instead of rendering.
 *
 * Redirecting via <Navigate> rather than calling navigate() in a page component's render body
 * matters: navigate() during render is a side effect happening outside React's render cycle,
 * which is what AdminPanel used to do.
 */
export function ProtectedRoute({ children }) {
  const { isLoggedIn } = useContext(AuthContext);
  const location = useLocation();

  if (!isLoggedIn) {
    return <Navigate to="/" state={{ from: location.pathname }} replace />;
  }
  return children;
}

/** ProtectedRoute, plus an admin role check. Non-admins are sent home. */
export function AdminRoute({ children }) {
  const { isLoggedIn, user } = useContext(AuthContext);
  const location = useLocation();

  if (!isLoggedIn) {
    return <Navigate to="/" state={{ from: location.pathname }} replace />;
  }
  const isAdmin = user?.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN');
  if (!isAdmin) {
    return <Navigate to="/" replace />;
  }
  return children;
}
