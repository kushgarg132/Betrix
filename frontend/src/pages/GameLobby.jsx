import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@apollo/client/react';
import { motion } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import { GET_GAMES } from '@/graphql/queries';
import { CREATE_GAME } from '@/graphql/mutations';
import { toast } from 'sonner';
import PageWrapper from '@/components/layout/PageWrapper';
import CreateGameModal from '@/components/lobby/CreateGameModal';
import GameCard from '@/components/lobby/GameCard';
import EmptyLobbyState from '@/components/lobby/EmptyLobbyState';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { PlusCircle, RefreshCw } from 'lucide-react';
import { useMediaQuery } from '@/hooks/useMediaQuery';

function GameCardSkeleton() {
  return (
    <div className="bg-surface border border-border rounded-[var(--radius-xl)] p-5 space-y-4">
      <Skeleton className="h-5 w-24" />
      <div className="flex justify-between">
        <Skeleton className="h-7 w-20" />
        <Skeleton className="h-5 w-16" />
      </div>
      <Skeleton className="h-1.5 w-full" />
      <Skeleton className="h-10 w-full" />
    </div>
  );
}

export default function GameLobby() {
  const [modalOpen, setModalOpen] = useState(false);
  const [createError, setCreateError] = useState('');
  const [creating, setCreating] = useState(false);
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  const isMobile = useMediaQuery('(max-width: 767px)');

  const { data, loading, refetch } = useQuery(GET_GAMES, { pollInterval: 30000 });
  const [createGameMutation] = useMutation(CREATE_GAME);

  const games = data?.games || [];

  const isPlayerInGame = (game) => {
    if (!user || !game.playerUsernames?.length) return false;
    return game.playerUsernames.includes(user.username);
  };

  const openModal = () => { setCreateError(''); setModalOpen(true); };
  const closeModal = () => setModalOpen(false);

  const handleCreate = async (smallBlind, bigBlind) => {
    setCreating(true);
    setCreateError('');
    try {
      const { data: res } = await createGameMutation({
        variables: { input: { smallBlindAmount: smallBlind, bigBlindAmount: bigBlind } },
      });
      setModalOpen(false);
      toast.success('Table created!');
      await refetch();
      if (res?.createGame?.id) navigate(`/game/${res.createGame.id}`);
    } catch (err) {
      setCreateError(err?.message || 'Failed to create table. Please try again.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <PageWrapper>
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-serif text-3xl font-bold text-text">Game Lobby</h1>
          <p className="text-text-muted text-sm mt-1">
            {loading ? 'Loading tables…' : `${games.length} table${games.length !== 1 ? 's' : ''} available`}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => refetch()}
            aria-label="Refresh tables"
            className="text-text-muted"
          >
            <RefreshCw size={16} />
          </Button>
          <Button onClick={openModal} className="gap-2">
            <PlusCircle size={16} />
            <span className="hidden sm:inline">Create Table</span>
            <span className="sm:hidden">Create</span>
          </Button>
        </div>
      </div>

      {/* Content */}
      {loading && !data ? (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => <GameCardSkeleton key={i} />)}
        </div>
      ) : games.length === 0 ? (
        <EmptyLobbyState onCreate={openModal} />
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {games.map((game, i) => (
            <GameCard
              key={game.id}
              game={game}
              index={i}
              isPlayerInGame={isPlayerInGame(game)}
            />
          ))}
        </div>
      )}

      {/* Create Game Modal (responsive) */}
      <CreateGameModal
        open={modalOpen}
        onClose={closeModal}
        onSubmit={handleCreate}
        loading={creating}
        error={createError}
        isMobile={isMobile}
      />
    </PageWrapper>
  );
}
