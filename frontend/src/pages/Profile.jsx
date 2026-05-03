import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import PageWrapper from '@/components/layout/PageWrapper';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import {
  Wallet, TrendingUp, Hash, Percent, LogOut,
  Clock, Mail, AtSign,
} from 'lucide-react';
import { formatChips, getPlayerInitials } from '@/lib/utils';

const STAT_CARDS = (user) => [
  { label: 'Balance', value: formatChips(user?.balance ?? 0), icon: Wallet, color: 'text-gold', bg: 'bg-gold-muted' },
  { label: 'Hands Played', value: (user?.handsPlayed ?? 0).toLocaleString(), icon: Hash, color: 'text-info', bg: 'bg-blue-900/20' },
  { label: 'Win Rate', value: `${user?.winRate ?? 0}%`, icon: Percent, color: 'text-success', bg: 'bg-success-muted' },
  { label: 'Net Profit', value: formatChips(user?.netProfit ?? 0), icon: TrendingUp, color: 'text-warning', bg: 'bg-amber-900/20' },
];

export default function Profile() {
  const navigate = useNavigate();
  const { user, logout } = useContext(AuthContext);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!user) {
    return (
      <PageWrapper>
        <div className="max-w-2xl mx-auto space-y-4 py-8">
          <Skeleton className="h-32 w-full rounded-[var(--radius-xl)]" />
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-24" />)}
          </div>
        </div>
      </PageWrapper>
    );
  }

  const stats = STAT_CARDS(user);
  const isAdmin = user.roles?.includes('ROLE_ADMIN');
  const initials = getPlayerInitials(user.name || user.username);

  return (
    <PageWrapper>
      <div className="max-w-2xl mx-auto space-y-6 py-4">
        {/* Profile card */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-surface border border-border rounded-[var(--radius-xl)] p-6"
        >
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-4">
              <Avatar className="h-16 w-16 border-2 border-gold/40">
                <AvatarFallback className="bg-gold/15 text-gold text-xl font-bold font-serif">
                  {initials}
                </AvatarFallback>
              </Avatar>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="font-serif text-2xl font-bold text-text">{user.name}</h1>
                  {isAdmin && <Badge variant="warning" className="text-[10px]">Admin</Badge>}
                </div>
                <div className="flex items-center gap-3 mt-1.5 text-text-muted text-sm">
                  <span className="flex items-center gap-1"><AtSign size={12} />{user.username}</span>
                  {user.email && <span className="flex items-center gap-1"><Mail size={12} />{user.email}</span>}
                </div>
              </div>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              className="text-text-muted hover:text-danger hover:bg-danger-muted gap-1.5"
              aria-label="Sign out"
            >
              <LogOut size={14} />
              <span className="hidden sm:inline">Sign out</span>
            </Button>
          </div>
        </motion.div>

        {/* Stats grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {stats.map((s, i) => (
            <motion.div
              key={s.label}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.06 }}
              className="bg-surface border border-border rounded-[var(--radius-xl)] p-4"
            >
              <div className={`inline-flex items-center justify-center w-8 h-8 rounded-[var(--radius-lg)] ${s.bg} mb-3`}>
                <s.icon size={16} className={s.color} />
              </div>
              <div className={`font-bold text-xl font-mono ${s.color}`}>{s.value}</div>
              <div className="text-text-dim text-xs mt-0.5">{s.label}</div>
            </motion.div>
          ))}
        </div>

        {/* Add Funds — coming soon */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-surface border border-border rounded-[var(--radius-xl)] p-5"
        >
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-text">Add Funds</h2>
              <p className="text-text-muted text-sm mt-0.5">Payment processing coming soon.</p>
            </div>
            <Badge variant="surface" className="gap-1.5">
              <Clock size={11} />
              Coming soon
            </Badge>
          </div>
        </motion.div>

        {/* Hand history placeholder */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-surface border border-border rounded-[var(--radius-xl)] overflow-hidden"
        >
          <div className="px-5 py-4 border-b border-border flex items-center justify-between">
            <h2 className="font-semibold text-text">Recent Hands</h2>
            <Badge variant="surface" className="text-[10px]">Coming soon</Badge>
          </div>
          <div className="p-8 flex flex-col items-center gap-2">
            <p className="text-text-dim text-sm text-center">Hand history will appear here once you play.</p>
          </div>
        </motion.div>
      </div>

    </PageWrapper>
  );
}
