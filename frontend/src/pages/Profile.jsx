import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';
import PageWrapper from '@/components/layout/PageWrapper';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { formatChips, getPlayerInitials } from '@/lib/utils';

// No account balance: chips are play money scoped to a table, not the account (see AuthContext /
// backend User.balance, deprecated). Table chips show on the table itself, not here.
function StatTile({ label, value, valueClassName }) {
  return (
    <div className="bg-surface border border-border rounded-[var(--radius-xl)] p-4">
      <div className={`font-mono tabular-nums text-2xl font-bold ${valueClassName || 'text-text'}`}>{value}</div>
      <div className="text-text-dim text-xs mt-1">{label}</div>
    </div>
  );
}

export default function Profile() {
  const navigate = useNavigate();
  const { user, logout } = useContext(AuthContext);

  if (!user) {
    return (
      <PageWrapper>
        <div className="max-w-2xl mx-auto space-y-4 py-8">
          <Skeleton className="h-32 w-full rounded-[var(--radius-xl)]" />
          <div className="grid grid-cols-2 gap-3">
            {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-20" />)}
          </div>
        </div>
      </PageWrapper>
    );
  }

  const isGuest = user.roles?.includes('GUEST');
  const netProfit = user.netProfit ?? 0;
  const initials = getPlayerInitials(user.name || user.username);

  const handleSignIn = () => {
    logout();
    navigate('/');
  };

  return (
    <PageWrapper>
      <div className="max-w-2xl mx-auto space-y-4 py-4">
        {/* Header */}
        <div className="flex items-center gap-4">
          <Avatar className="h-16 w-16 border-2 border-neon-cyan/40">
            {user.avatarUrl && <AvatarImage src={user.avatarUrl} alt="" />}
            <AvatarFallback className="text-xl font-display">{initials}</AvatarFallback>
          </Avatar>
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="font-display text-2xl font-bold text-text">{user.name}</h1>
            {isGuest && <Badge variant="surface">Guest</Badge>}
          </div>
        </div>

        {/* Stat tiles */}
        <div className="grid grid-cols-2 gap-3">
          <StatTile label="Hands Played" value={(user.handsPlayed ?? 0).toLocaleString()} />
          <StatTile label="Hands Won" value={(user.handsWon ?? 0).toLocaleString()} />
          <StatTile label="Win Rate" value={`${user.winRate ?? 0}%`} />
          <StatTile
            label="Net Result"
            value={formatChips(netProfit)}
            valueClassName={netProfit >= 0 ? 'text-success' : 'text-danger'}
          />
        </div>

        {isGuest && (
          <div className="bg-surface border border-border rounded-[var(--radius-xl)] p-5 flex items-center justify-between gap-4 flex-wrap">
            <p className="text-text-muted text-sm">Sign in with Google to keep your stats</p>
            <Button onClick={handleSignIn} size="sm">Sign in</Button>
          </div>
        )}
      </div>
    </PageWrapper>
  );
}
