import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@apollo/client/react';
import { Users, ArrowRight, ChevronDown, Loader2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Button } from '@/components/ui/button';
import { formatBlinds, cn } from '@/lib/utils';
import { ADD_BOT } from '@/graphql/mutations';
import { toast } from 'sonner';

const STATUS_VARIANT = {
  WAITING:   'waiting',
  ACTIVE:    'active',
  COMPLETED: 'completed',
};

const STATUS_LABEL = {
  WAITING:   'Waiting',
  ACTIVE:    'Live',
  COMPLETED: 'Ended',
};

const BOT_DIFFICULTIES = [
  { value: 'EASY',   label: 'Easy',   cls: 'text-emerald-400 hover:bg-emerald-500/10' },
  { value: 'MEDIUM', label: 'Medium', cls: 'text-amber-400 hover:bg-amber-500/10'    },
  { value: 'HARD',   label: 'Hard',   cls: 'text-red-400 hover:bg-red-500/10'        },
];

function LivePulse() {
  return (
    <span className="relative flex h-2 w-2">
      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-success opacity-75" />
      <span className="relative inline-flex rounded-full h-2 w-2 bg-success" />
    </span>
  );
}

export default function GameCard({ game, index, isPlayerInGame }) {
  const navigate = useNavigate();
  const [botOpen, setBotOpen] = useState(false);
  const [addBot, { loading: botLoading }] = useMutation(ADD_BOT);

  const fillPct = Math.round((game.playerCount / game.maxPlayers) * 100);
  const isFull = game.playerCount >= game.maxPlayers;
  const isActive = game.status === 'ACTIVE';
  const isWaiting = game.status === 'WAITING';

  const handleJoin = () => navigate(`/game/${game.id}`);

  const handleAddBot = async (difficulty) => {
    try {
      await addBot({ variables: { gameId: game.id, difficulty } });
      toast.success(`${difficulty.charAt(0) + difficulty.slice(1).toLowerCase()} bot added`);
      setBotOpen(false);
    } catch (err) {
      toast.error(err.message || 'Failed to add bot');
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay: index * 0.05 }}
      className="bg-surface border border-border rounded-[var(--radius-xl)] p-5 hover:border-border-gold hover:shadow-[0_0_20px_rgba(212,168,67,0.08)] transition-all duration-200 group flex flex-col gap-4"
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 flex-wrap">
          <Badge variant={STATUS_VARIANT[game.status] || 'surface'}>
            {isActive && <LivePulse />}
            {STATUS_LABEL[game.status] || game.status}
          </Badge>
          {isPlayerInGame && (
            <Badge variant="default" className="text-[10px]">Your table</Badge>
          )}
        </div>
        <span className="text-text-dim text-xs font-mono">{game.id?.slice(0, 8)}</span>
      </div>

      {/* Blinds */}
      <div className="flex items-end justify-between">
        <div>
          <span className="text-text-dim text-xs uppercase tracking-wider">Blinds</span>
          <div className="text-gold font-bold text-xl font-mono mt-0.5">
            {formatBlinds(game.smallBlindAmount, game.bigBlindAmount)}
          </div>
        </div>
        <div className="text-right">
          <span className="text-text-dim text-xs uppercase tracking-wider">Players</span>
          <div className="flex items-center gap-1.5 justify-end mt-0.5">
            <Users size={14} className="text-text-muted" />
            <span className="font-semibold text-text">
              {game.playerCount}<span className="text-text-dim">/{game.maxPlayers}</span>
            </span>
          </div>
        </div>
      </div>

      {/* Player fill bar */}
      <Progress
        value={fillPct}
        className="h-1.5"
        indicatorClassName={isFull ? 'bg-danger' : isActive ? 'bg-success' : 'bg-gold'}
      />

      {/* Join button */}
      <Button
        variant={isPlayerInGame ? 'default' : 'outline'}
        className="w-full gap-2 group-hover:border-gold transition-colors"
        onClick={handleJoin}
        aria-label={isPlayerInGame ? `Return to your table` : `Join game ${game.id?.slice(0, 8)}`}
      >
        <span>{isPlayerInGame ? 'Return to Table' : isFull ? 'Spectate' : 'Join Table'}</span>
        <ArrowRight size={15} className="group-hover:translate-x-0.5 transition-transform" />
      </Button>

      {/* Fill with Bot — only shown for WAITING, non-full games */}
      {isWaiting && !isFull && (
        <div>
          <Button
            variant="ghost"
            size="sm"
            className="w-full gap-1.5 text-xs text-text-dim hover:text-text border border-transparent hover:border-white/10"
            onClick={() => setBotOpen(v => !v)}
            aria-expanded={botOpen}
            aria-label="Fill seat with bot"
          >
            <BotIcon size={12} />
            Fill with Bot
            <ChevronDown size={11} className={cn('ml-auto transition-transform', botOpen && 'rotate-180')} />
          </Button>

          <AnimatePresence>
            {botOpen && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.15 }}
                className="overflow-hidden"
              >
                <div className="flex gap-1.5 pt-2">
                  {BOT_DIFFICULTIES.map(d => (
                    <button
                      key={d.value}
                      onClick={() => handleAddBot(d.value)}
                      disabled={botLoading}
                      className={cn(
                        'flex-1 py-1.5 text-[10px] font-semibold rounded border border-white/10 transition-colors',
                        d.cls
                      )}
                    >
                      {botLoading ? <Loader2 size={10} className="mx-auto animate-spin" /> : d.label}
                    </button>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}
    </motion.div>
  );
}

function BotIcon({ size = 14 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="11" width="18" height="10" rx="2"/>
      <circle cx="12" cy="5" r="2"/>
      <line x1="12" y1="7" x2="12" y2="11"/>
      <line x1="8" y1="16" x2="8" y2="16" strokeWidth="3"/>
      <line x1="16" y1="16" x2="16" y2="16" strokeWidth="3"/>
    </svg>
  );
}
