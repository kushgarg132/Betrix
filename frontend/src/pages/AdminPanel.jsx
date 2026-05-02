import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@apollo/client/react';
import { motion } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import { GET_GAMES } from '@/graphql/queries';
import { DELETE_GAME } from '@/graphql/mutations';
import { toast } from 'sonner';
import PageWrapper from '@/components/layout/PageWrapper';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from '@/components/ui/dialog';
import {
  Search, Trash2, RefreshCw, Shield, Loader2, Users,
} from 'lucide-react';
import { formatBlinds } from '@/lib/utils';

const STATUS_VARIANT = { WAITING: 'waiting', ACTIVE: 'active', COMPLETED: 'completed' };

function TableRow({ game, onDelete }) {
  return (
    <motion.tr
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="border-b border-border hover:bg-surface-elevated/30 transition-colors group"
    >
      <td className="px-4 py-3 text-xs font-mono text-text-dim">{game.id?.slice(0, 12)}…</td>
      <td className="px-4 py-3">
        <Badge variant={STATUS_VARIANT[game.status] || 'surface'}>{game.status}</Badge>
      </td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-1.5 text-sm text-text">
          <Users size={13} className="text-text-muted" />
          {game.playerCount}<span className="text-text-dim">/{game.maxPlayers}</span>
        </div>
      </td>
      <td className="px-4 py-3 font-mono text-sm text-gold font-semibold">
        {formatBlinds(game.smallBlindAmount, game.bigBlindAmount)}
      </td>
      <td className="px-4 py-3 text-xs text-text-dim">
        {game.createdAt ? new Date(game.createdAt).toLocaleDateString() : '—'}
      </td>
      <td className="px-4 py-3 text-right">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => onDelete(game)}
          className="text-text-dim hover:text-danger hover:bg-danger-muted opacity-0 group-hover:opacity-100 transition-all"
          aria-label={`Delete game ${game.id?.slice(0, 8)}`}
        >
          <Trash2 size={14} />
        </Button>
      </td>
    </motion.tr>
  );
}

function TableRowSkeleton() {
  return (
    <tr className="border-b border-border">
      {[...Array(6)].map((_, i) => (
        <td key={i} className="px-4 py-3"><Skeleton className="h-4 w-full" /></td>
      ))}
    </tr>
  );
}

export default function AdminPanel() {
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const isAdmin = user?.roles?.includes('ROLE_ADMIN');

  const { data, loading, refetch } = useQuery(GET_GAMES, { skip: !isAdmin });
  const [deleteGameMutation] = useMutation(DELETE_GAME);

  if (!user) { navigate('/login'); return null; }
  if (!isAdmin) { navigate('/'); return null; }

  const games = (data?.games || []).filter(g => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      g.id?.toLowerCase().includes(q) ||
      g.status?.toLowerCase().includes(q)
    );
  });

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteGameMutation({ variables: { id: deleteTarget.id } });
      toast.success('Table deleted.');
      setDeleteTarget(null);
      refetch();
    } catch (err) {
      toast.error(err?.message || 'Failed to delete table.');
    } finally {
      setDeleting(false);
    }
  };

  return (
    <PageWrapper>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-[var(--radius-lg)] bg-gold-muted border border-border-gold flex items-center justify-center">
            <Shield size={18} className="text-gold" />
          </div>
          <div>
            <h1 className="font-serif text-2xl font-bold text-text">Admin Panel</h1>
            <p className="text-text-muted text-xs">
              {loading ? 'Loading…' : `${data?.games?.length ?? 0} total tables`}
            </p>
          </div>
        </div>
        <Button variant="ghost" size="icon" onClick={() => refetch()} aria-label="Refresh">
          <RefreshCw size={16} className="text-text-muted" />
        </Button>
      </div>

      {/* Search */}
      <div className="relative mb-5 max-w-xs">
        <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-dim" />
        <Input
          placeholder="Search by ID or status…"
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Table */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-surface border border-border rounded-[var(--radius-xl)] overflow-hidden"
      >
        <div className="overflow-x-auto">
          <table className="w-full min-w-[600px]">
            <thead>
              <tr className="border-b border-border bg-surface-elevated/50">
                {['Game ID', 'Status', 'Players', 'Blinds', 'Created', ''].map(col => (
                  <th key={col} className="px-4 py-3 text-left text-xs font-semibold text-text-dim uppercase tracking-wider">
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                [...Array(5)].map((_, i) => <TableRowSkeleton key={i} />)
              ) : games.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center text-text-dim text-sm">
                    {search ? 'No tables match your search.' : 'No tables found.'}
                  </td>
                </tr>
              ) : (
                games.map(game => (
                  <TableRow key={game.id} game={game} onDelete={setDeleteTarget} />
                ))
              )}
            </tbody>
          </table>
        </div>

        {!loading && games.length > 0 && (
          <div className="px-4 py-3 border-t border-border text-xs text-text-dim">
            Showing {games.length} {search ? 'matching ' : ''}table{games.length !== 1 ? 's' : ''}
          </div>
        )}
      </motion.div>

      {/* Delete confirmation dialog */}
      <Dialog open={!!deleteTarget} onOpenChange={v => !v && setDeleteTarget(null)}>
        <DialogContent className="max-w-[380px]">
          <DialogHeader>
            <DialogTitle>Delete Table</DialogTitle>
            <DialogDescription>
              This will permanently delete table{' '}
              <span className="font-mono text-text">{deleteTarget?.id?.slice(0, 12)}…</span>.
              This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setDeleteTarget(null)} disabled={deleting}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDelete} disabled={deleting} className="gap-2">
              {deleting && <Loader2 size={14} className="animate-spin" />}
              {deleting ? 'Deleting…' : 'Delete Table'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageWrapper>
  );
}
