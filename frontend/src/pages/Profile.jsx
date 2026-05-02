import React, { useContext, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import { useAuth } from '@/hooks/useAuth';
import PageWrapper from '@/components/layout/PageWrapper';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from '@/components/ui/dialog';
import { toast } from 'sonner';
import {
  Wallet, TrendingUp, Hash, Percent, LogOut,
  PlusCircle, User, Mail, AtSign, Loader2,
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
  const { user, logout, updateBalance, refreshUserData } = useContext(AuthContext);
  const { addBalance } = useAuth();

  const [showTopUp, setShowTopUp] = useState(false);
  const [amount, setAmount] = useState('');
  const [topUpLoading, setTopUpLoading] = useState(false);
  const [topUpError, setTopUpError] = useState('');

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleTopUp = async () => {
    const val = parseFloat(amount);
    if (!val || val <= 0) { setTopUpError('Enter a valid amount.'); return; }
    setTopUpLoading(true);
    setTopUpError('');
    try {
      const res = await addBalance(val);
      updateBalance(res?.balance ?? (user.balance + val));
      await refreshUserData();
      toast.success(`${formatChips(val)} added to your balance!`);
      setShowTopUp(false);
      setAmount('');
    } catch (err) {
      setTopUpError(err?.message || 'Failed to add balance. Please try again.');
    } finally {
      setTopUpLoading(false);
    }
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

        {/* Balance top-up */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-surface border border-border rounded-[var(--radius-xl)] p-5"
        >
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-text">Add Funds</h2>
              <p className="text-text-muted text-sm mt-0.5">Top up your balance to keep playing.</p>
            </div>
            <Button onClick={() => setShowTopUp(true)} variant="outline" className="gap-2">
              <PlusCircle size={15} />
              Top Up
            </Button>
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
          <div className="p-5 space-y-3">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="flex items-center justify-between py-2 border-b border-border/50 last:border-0">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-4 w-16" />
                <Skeleton className="h-4 w-12" />
              </div>
            ))}
            <p className="text-text-dim text-xs text-center pt-2">Hand history will appear here once you play.</p>
          </div>
        </motion.div>
      </div>

      {/* Top-up dialog */}
      <Dialog open={showTopUp} onOpenChange={v => !v && setShowTopUp(false)}>
        <DialogContent className="max-w-[360px]">
          <DialogHeader>
            <DialogTitle>Add Funds</DialogTitle>
            <DialogDescription>Enter the amount you'd like to add to your balance.</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            {/* Quick amounts */}
            <div className="grid grid-cols-3 gap-2">
              {[100, 500, 1000].map(amt => (
                <button
                  key={amt}
                  onClick={() => setAmount(String(amt))}
                  className="py-2 rounded-[var(--radius)] border border-border bg-surface-elevated text-sm font-semibold text-text-muted hover:border-gold hover:text-gold transition-colors"
                >
                  ${amt}
                </button>
              ))}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="topup-amount">Custom Amount ($)</Label>
              <Input
                id="topup-amount"
                type="number"
                min="1"
                placeholder="Enter amount"
                value={amount}
                onChange={e => { setAmount(e.target.value); setTopUpError(''); }}
              />
            </div>
            {topUpError && <p className="text-danger text-sm">{topUpError}</p>}
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setShowTopUp(false)}>Cancel</Button>
            <Button onClick={handleTopUp} disabled={topUpLoading} className="gap-2">
              {topUpLoading && <Loader2 size={14} className="animate-spin" />}
              {topUpLoading ? 'Processing…' : 'Add Funds'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageWrapper>
  );
}
