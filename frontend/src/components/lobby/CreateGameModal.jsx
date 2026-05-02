import React, { useState } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import BottomSheet from '@/components/layout/BottomSheet';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';

const BLIND_PRESETS = [
  { small: 5,   big: 10  },
  { small: 25,  big: 50  },
  { small: 50,  big: 100 },
  { small: 100, big: 200 },
];

function CreateGameForm({ onSubmit, loading, error }) {
  const [selected, setSelected] = useState(null);
  const [custom, setCustom] = useState(false);
  const [smallBlind, setSmallBlind] = useState('');
  const [bigBlind, setBigBlind] = useState('');
  const [localError, setLocalError] = useState('');

  const handlePreset = (preset) => {
    setSelected(preset);
    setCustom(false);
    setLocalError('');
  };

  const handleCreate = () => {
    if (custom) {
      const s = parseFloat(smallBlind);
      const b = parseFloat(bigBlind);
      if (!s || s <= 0) { setLocalError('Enter a valid small blind.'); return; }
      if (!b || b <= 0) { setLocalError('Enter a valid big blind.'); return; }
      if (b < s) { setLocalError('Big blind must be ≥ small blind.'); return; }
      onSubmit(Math.round(s), Math.round(b));
    } else if (selected) {
      onSubmit(selected.small, selected.big);
    } else {
      setLocalError('Choose a blind level or enter custom amounts.');
    }
  };

  const err = error || localError;

  return (
    <div className="space-y-5">
      <p className="text-text-muted text-sm">Select blind levels to start a new table.</p>

      {/* Preset grid */}
      <div className="grid grid-cols-2 gap-2.5">
        {BLIND_PRESETS.map((p) => {
          const isSelected = !custom && selected?.small === p.small;
          return (
            <button
              key={p.small}
              onClick={() => handlePreset(p)}
              className={cn(
                'py-3 px-4 rounded-[var(--radius-lg)] border text-sm font-semibold transition-all duration-150 text-center',
                isSelected
                  ? 'border-gold bg-gold-muted text-gold shadow-[0_0_12px_rgba(212,168,67,0.2)]'
                  : 'border-border bg-surface-elevated text-text-muted hover:border-border-gold hover:text-text'
              )}
            >
              <span className="font-mono">${p.small} / ${p.big}</span>
              <span className="block text-[10px] font-normal text-text-dim mt-0.5">
                {p.small === 5 ? 'Micro' : p.small === 25 ? 'Low' : p.small === 50 ? 'Mid' : 'High'}
              </span>
            </button>
          );
        })}

        {/* Custom option */}
        <button
          onClick={() => { setCustom(true); setSelected(null); }}
          className={cn(
            'col-span-2 py-3 px-4 rounded-[var(--radius-lg)] border text-sm font-semibold transition-all duration-150',
            custom
              ? 'border-gold bg-gold-muted text-gold'
              : 'border-dashed border-border text-text-dim hover:border-border-gold hover:text-text'
          )}
        >
          Custom blinds
        </button>
      </div>

      {/* Custom fields */}
      {custom && (
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="sb">Small Blind ($)</Label>
            <Input id="sb" type="number" min="1" placeholder="e.g. 10" value={smallBlind} onChange={e => setSmallBlind(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="bb">Big Blind ($)</Label>
            <Input id="bb" type="number" min="1" placeholder="e.g. 20" value={bigBlind} onChange={e => setBigBlind(e.target.value)} />
          </div>
        </div>
      )}

      {err && <p className="text-danger text-sm">{err}</p>}

      <Button onClick={handleCreate} disabled={loading} className="w-full" size="lg">
        {loading ? 'Creating…' : 'Create Table'}
      </Button>
    </div>
  );
}

export default function CreateGameModal({ open, onClose, onSubmit, loading, error, isMobile }) {
  if (isMobile) {
    return (
      <BottomSheet open={open} onClose={onClose} title="Create New Table">
        <CreateGameForm onSubmit={onSubmit} loading={loading} error={error} />
      </BottomSheet>
    );
  }

  return (
    <Dialog open={open} onOpenChange={v => !v && onClose()}>
      <DialogContent className="max-w-[420px]">
        <DialogHeader>
          <DialogTitle>Create New Table</DialogTitle>
          <DialogDescription>Choose stakes to start a Texas Hold'em game.</DialogDescription>
        </DialogHeader>
        <CreateGameForm onSubmit={onSubmit} loading={loading} error={error} />
      </DialogContent>
    </Dialog>
  );
}
