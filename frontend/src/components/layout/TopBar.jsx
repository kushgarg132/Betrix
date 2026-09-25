import React, { useContext } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { LayoutGrid, User, Shield, LogOut } from 'lucide-react';
import { AuthContext } from '@/context/AuthContext';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuSeparator, DropdownMenuTrigger, DropdownMenuLabel,
} from '@/components/ui/dropdown-menu';
import { getPlayerInitials, cn } from '@/lib/utils';
import { isChromeless, isAdminUser } from './BottomTabBar';

const LINKS = [
  { to: '/lobby', label: 'Lobby', Icon: LayoutGrid },
  { to: '/profile', label: 'Profile', Icon: User },
];

export default function TopBar() {
  const { isLoggedIn, user, logout } = useContext(AuthContext);
  const { pathname } = useLocation();
  const navigate = useNavigate();
  if (!isLoggedIn || isChromeless(pathname)) return null;

  const isAdmin = isAdminUser(user);
  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-40 h-14 flex items-center justify-between px-4 border-b border-border bg-surface/90 backdrop-blur">
      <Link to="/lobby" className="font-display font-bold text-neon-cyan">
        BETRIX
      </Link>

      <nav aria-label="Main" className="hidden lg:flex items-center gap-1">
        {LINKS.map(({ to, label, Icon }) => (
          <NavLink key={to} to={to} className={({ isActive }) => cn(
            'flex items-center gap-1.5 px-3 py-1.5 rounded-[var(--radius)] text-sm font-medium',
            isActive ? 'text-neon-cyan' : 'text-text-muted hover:text-text')}>
            <Icon size={16} aria-hidden="true" />
            {label}
          </NavLink>
        ))}
        {isAdmin && (
          <NavLink to="/admin" className={({ isActive }) => cn(
            'flex items-center gap-1.5 px-3 py-1.5 rounded-[var(--radius)] text-sm font-medium',
            isActive ? 'text-neon-cyan' : 'text-text-muted hover:text-text')}>
            <Shield size={16} aria-hidden="true" />
            Admin
          </NavLink>
        )}
      </nav>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button
            aria-label="Account menu"
            className="rounded-full p-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-neon-cyan"
          >
            <Avatar className="h-8 w-8">
              {user?.avatarUrl && <AvatarImage src={user.avatarUrl} alt="" />}
              <AvatarFallback>{getPlayerInitials(user?.name)}</AvatarFallback>
            </Avatar>
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          <DropdownMenuLabel>{user?.name}</DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={() => navigate('/profile')}>
            <User size={14} /> Profile
          </DropdownMenuItem>
          {isAdmin && (
            <DropdownMenuItem onClick={() => navigate('/admin')}>
              <Shield size={14} /> Admin
            </DropdownMenuItem>
          )}
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={handleLogout} className="text-danger focus:text-danger focus:bg-danger-muted">
            <LogOut size={14} /> Sign out
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
