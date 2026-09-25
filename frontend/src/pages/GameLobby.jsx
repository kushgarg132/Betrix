import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@apollo/client/react';
import { GET_GAMES } from '@/graphql/queries';
import { CREATE_GAME } from '@/graphql/mutations';
import { toast } from 'sonner';
import PageWrapper from '@/components/layout/PageWrapper';
import CreateGameModal from '@/components/lobby/CreateGameModal';
import GameCard from '@/components/lobby/GameCard';
import EmptyLobbyState from '@/components/lobby/EmptyLobbyState';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';

export default function GameLobby() {
  const [modalOpen, setModalOpen] = useState(false);
  const [createError, setCreateError] = useState('');
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();

  const { data, loading, refetch } = useQuery(GET_GAMES, { pollInterval: 30000 });
  const [createGameMutation] = useMutation(CREATE_GAME);

  const games = data?.games || [];
  const sorted = [...games].sort((a, b) => Number(b.isYourGame) - Number(a.isYourGame));

  const openModal = () => { setCreateError(''); setModalOpen(true); };
  const closeModal = () => setModalOpen(false);

  const handleCreate = async (smallBlind, bigBlind) => {
    setCreating(true);
    setCreateError('');
    try {
      const { data: res } = await createGameMutation({
        variables: { input: { smallBlindAmount: smallBlind, bigBlindAmount: bigBlind } },
      });
      const id = res?.createGame?.id;
      setModalOpen(false);
      toast.success('Table created!');
      // Navigation is the primary outcome of a successful mutation — it must not depend on this
      // best-effort background refresh of the lobby list. If refetch() fails (network blip, a
      // stray poll collision, whatever), that's a lobby-list staleness problem, not a reason to
      // strand the creator on /lobby after their table was actually created.
      if (id) navigate(`/game/${id}`);
      refetch().catch(() => {});
    } catch (err) {
      setCreateError(err?.message || 'Failed to create table. Please try again.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <PageWrapper>
      <h1 className="font-display text-2xl font-bold text-text mb-4">Lobby</h1>

      {loading && !data ? (
        <div className="grid gap-2 md:grid-cols-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 rounded-xl" />
          ))}
        </div>
      ) : sorted.length === 0 ? (
        <EmptyLobbyState onCreate={openModal} />
      ) : (
        <ul className="grid gap-2 md:grid-cols-2 pb-36 lg:pb-0">
          {sorted.map((game) => (
            <li key={game.id}>
              <GameCard game={game} onJoin={() => navigate(`/game/${game.id}`)} />
            </li>
          ))}
        </ul>
      )}

      <Button
        onClick={openModal}
        size="lg"
        className="glow-cyan fixed right-4 bottom-20 lg:bottom-6 z-30 gap-2"
        aria-label="Create table"
      >
        <Plus size={18} />
        Create table
      </Button>

      <CreateGameModal
        open={modalOpen}
        onClose={closeModal}
        onSubmit={handleCreate}
        loading={creating}
        error={createError}
      />
    </PageWrapper>
  );
}
