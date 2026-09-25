import React, { useContext } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { LayoutGrid, User, Shield } from 'lucide-react';
import { AuthContext } from '@/context/AuthContext';
import { cn } from '@/lib/utils';

export const isChromeless = (path) => path === '/' || path.startsWith('/game/');
export const isAdminUser = (user) => user?.roles?.some((r) => r === 'ADMIN' || r === 'ROLE_ADMIN');

export default function BottomTabBar() {
  const { isLoggedIn, user } = useContext(AuthContext);
  const { pathname } = useLocation();
  if (!isLoggedIn || isChromeless(pathname)) return null;

  const tabs = [
    { to: '/lobby', label: 'Lobby', Icon: LayoutGrid },
    { to: '/profile', label: 'Profile', Icon: User },
    ...(isAdminUser(user) ? [{ to: '/admin', label: 'Admin', Icon: Shield }] : []),
  ];

  return (
    <nav aria-label="Main" className="lg:hidden fixed bottom-0 inset-x-0 z-40 border-t border-border bg-surface/90 backdrop-blur pb-[env(safe-area-inset-bottom)]">
      <ul className="flex">
        {tabs.map(({ to, label, Icon }) => (
          <li key={to} className="flex-1">
            <NavLink to={to} className={({ isActive }) => cn(
              'flex flex-col items-center justify-center gap-1 h-16 text-xs',
              isActive ? 'text-neon-cyan' : 'text-text-muted hover:text-text')}>
              <Icon size={22} aria-hidden="true" />
              {label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
