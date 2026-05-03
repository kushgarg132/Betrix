import React, { useContext, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import { formatChips } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuSeparator, DropdownMenuTrigger, DropdownMenuLabel,
} from '@/components/ui/dropdown-menu';
import {
  User, LogOut, Settings, ChevronDown, Menu, X,
  Layers, Shield, Wallet,
} from 'lucide-react';

const NAV_LINKS = [
  { to: '/lobby', label: 'Lobby' },
  { to: '/profile', label: 'Profile' },
];

function getInitials(user) {
  if (!user) return '?';
  return (user.name || user.username || '?').split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
}

export default function Navbar() {
  const { isLoggedIn, user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const isAdmin = user?.roles?.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN');

  const handleLogout = () => {
    logout();
    navigate('/login');
    setMobileOpen(false);
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="sticky top-0 z-40 w-full glass-surface border-b border-border/50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 h-14 flex items-center justify-between">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 group" onClick={() => setMobileOpen(false)}>
          <div className="w-7 h-7 rounded-[var(--radius-sm)] bg-gold flex items-center justify-center shadow-[0_0_12px_rgba(212,168,67,0.5)]">
            <span className="text-text-inverse font-bold text-xs">B</span>
          </div>
          <span className="font-serif text-xl font-bold text-text group-hover:text-gold transition-colors">
            Betrix
          </span>
        </Link>

        {/* Desktop nav links */}
        <div className="hidden md:flex items-center gap-1">
          {NAV_LINKS.map(({ to, label }) => (
            <Link
              key={to}
              to={to}
              className={`px-3 py-1.5 rounded-[var(--radius)] text-sm font-medium transition-colors duration-150 ${
                isActive(to)
                  ? 'text-gold bg-gold-muted'
                  : 'text-text-muted hover:text-text hover:bg-surface-elevated'
              }`}
            >
              {label}
            </Link>
          ))}
          {isAdmin && (
            <Link
              to="/admin"
              className={`px-3 py-1.5 rounded-[var(--radius)] text-sm font-medium transition-colors duration-150 flex items-center gap-1.5 ${
                isActive('/admin')
                  ? 'text-gold bg-gold-muted'
                  : 'text-text-muted hover:text-text hover:bg-surface-elevated'
              }`}
            >
              <Shield size={14} />
              Admin
            </Link>
          )}
        </div>

        {/* Desktop right side */}
        <div className="hidden md:flex items-center gap-3">
          {isLoggedIn && user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button className="flex items-center gap-2 px-2 py-1 rounded-[var(--radius)] hover:bg-surface-elevated transition-colors group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold">
                  <Avatar className="h-7 w-7">
                    <AvatarFallback className="bg-gold/20 text-gold text-xs font-bold">
                      {getInitials(user)}
                    </AvatarFallback>
                  </Avatar>
                  <span className="text-sm text-text-muted group-hover:text-text transition-colors">
                    {user.username}
                  </span>
                  <div className="flex items-center gap-1 text-gold text-xs font-semibold bg-gold-muted px-1.5 py-0.5 rounded-full">
                    <Wallet size={11} />
                    {formatChips(user.balance)}
                  </div>
                  <ChevronDown size={14} className="text-text-dim group-data-[state=open]:rotate-180 transition-transform" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuLabel>{user.name || user.username}</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => navigate('/profile')}>
                  <User size={14} /> Profile
                </DropdownMenuItem>
                {isAdmin && (
                  <DropdownMenuItem onClick={() => navigate('/admin')}>
                    <Shield size={14} /> Admin Panel
                  </DropdownMenuItem>
                )}
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleLogout} className="text-danger focus:text-danger focus:bg-danger-muted">
                  <LogOut size={14} /> Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="sm" onClick={() => navigate('/login')}>Sign in</Button>
              <Button size="sm" onClick={() => navigate('/register')}>Play Now</Button>
            </div>
          )}
        </div>

        {/* Mobile menu toggle */}
        <button
          className="md:hidden p-2 rounded-[var(--radius)] text-text-muted hover:text-text hover:bg-surface-elevated transition-colors"
          onClick={() => setMobileOpen(v => !v)}
          aria-label="Toggle menu"
        >
          {mobileOpen ? <X size={20} /> : <Menu size={20} />}
        </button>
      </div>

      {/* Mobile drawer */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.15 }}
            className="md:hidden border-t border-border bg-surface px-4 py-4 space-y-1"
          >
            {NAV_LINKS.map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                onClick={() => setMobileOpen(false)}
                className={`block px-3 py-2.5 rounded-[var(--radius)] text-sm font-medium transition-colors ${
                  isActive(to)
                    ? 'text-gold bg-gold-muted'
                    : 'text-text-muted hover:text-text hover:bg-surface-elevated'
                }`}
              >
                {label}
              </Link>
            ))}
            {isAdmin && (
              <Link
                to="/admin"
                onClick={() => setMobileOpen(false)}
                className="flex items-center gap-2 px-3 py-2.5 rounded-[var(--radius)] text-sm font-medium text-text-muted hover:text-text hover:bg-surface-elevated"
              >
                <Shield size={14} /> Admin
              </Link>
            )}
            <div className="pt-2 border-t border-border mt-2">
              {isLoggedIn && user ? (
                <div className="space-y-1">
                  <div className="px-3 py-2 flex items-center gap-3">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback className="bg-gold/20 text-gold text-xs font-bold">{getInitials(user)}</AvatarFallback>
                    </Avatar>
                    <div>
                      <p className="text-sm font-medium text-text">{user.username}</p>
                      <p className="text-xs text-gold font-semibold">{formatChips(user.balance)}</p>
                    </div>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-3 py-2.5 rounded-[var(--radius)] text-sm font-medium text-danger hover:bg-danger-muted transition-colors"
                  >
                    <LogOut size={14} /> Sign out
                  </button>
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  <Button variant="outline" onClick={() => { navigate('/login'); setMobileOpen(false); }}>Sign in</Button>
                  <Button onClick={() => { navigate('/register'); setMobileOpen(false); }}>Play Now</Button>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </nav>
  );
}
