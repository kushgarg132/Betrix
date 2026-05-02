import React, { useState } from 'react';
import { useMutation } from '@apollo/client/react';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { ADD_BOT } from '@/graphql/mutations';

const DIFFICULTIES = [
  { value: 'EASY',   label: 'Easy',   color: 'text-emerald-400 border-emerald-500/40 bg-emerald-500/10 hover:bg-emerald-500/20' },
  { value: 'MEDIUM', label: 'Medium', color: 'text-amber-400  border-amber-500/40  bg-amber-500/10  hover:bg-amber-500/20'  },
  { value: 'HARD',   label: 'Hard',   color: 'text-red-400    border-red-500/40    bg-red-500/10    hover:bg-red-500/20'    },
];

export default function AddBotButton({ gameId, disabled }) {
  const [selected, setSelected] = useState('MEDIUM');
  const [addBot, { loading }] = useMutation(ADD_BOT);

  const handleAdd = async () => {
    try {
      await addBot({ variables: { gameId, difficulty: selected } });
      toast.success(`${selected.charAt(0) + selected.slice(1).toLowerCase()} bot added to table`);
    } catch (err) {
      toast.error(err.message || 'Failed to add bot');
    }
  };

  return (
    <div className="space-y-2">
      <p className="text-[10px] font-semibold text-text-dim uppercase tracking-wider">Difficulty</p>
      <div className="flex gap-1">
        {DIFFICULTIES.map(d => (
          <button
            key={d.value}
            onClick={() => setSelected(d.value)}
            className={cn(
              'flex-1 py-1 text-[10px] font-bold rounded border transition-all duration-150',
              d.color,
              selected === d.value ? 'ring-1 ring-current' : 'opacity-60'
            )}
          >
            {d.label}
          </button>
        ))}
      </div>
      <Button
        size="sm"
        variant="outline"
        className="w-full gap-1.5 text-xs border-white/20 hover:border-gold/50 hover:text-gold"
        onClick={handleAdd}
        disabled={disabled || loading}
        aria-label="Add bot player"
      >
        {loading ? <Loader2 size={12} className="animate-spin" /> : <BotIcon size={12} />}
        {loading ? 'Adding…' : 'Add Bot'}
      </Button>
    </div>
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
      <line x1="7" y1="21" x2="7" y2="21"/>
      <line x1="17" y1="21" x2="17" y2="21"/>
    </svg>
  );
}
