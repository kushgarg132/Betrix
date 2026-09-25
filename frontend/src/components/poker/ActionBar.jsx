import React, { useCallback, useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import AddBotButton from './AddBotButton';
import { formatChips } from '@/lib/utils';

const SNAP_LABELS = ['½ Pot', 'Pot', 'All In'];

/** Pinned bottom action bar: pre-hand controls, off-turn sit out/in, or the hero's turn actions. */
export default function ActionBar({ game, hero, heroIndex, isMyTurn, online, actions }) {
  const [showRaiseSlider, setShowRaiseSlider] = useState(false);
  const [raiseAmount, setRaiseAmount] = useState(0);

  const disabled = !online;
  const myChips = hero?.chips ?? 0;
  const currentBet = game?.currentBet ?? 0;
  const playerCurrentBet = game?.currentBettingRound?.bets?.[hero?.id] ?? 0;
  const callAmount = Math.max(0, currentBet - playerCurrentBet);
  const pot = game?.pot ?? 0;
  const minRaise = Math.max(currentBet * 2, game?.bigBlindAmount ?? 0);
  const maxRaise = myChips + playerCurrentBet;

  const handleRaiseOpen = useCallback(() => {
    setRaiseAmount(minRaise);
    setShowRaiseSlider(true);
  }, [minRaise]);

  const handleBet = useCallback((amount) => {
    actions.bet(amount)?.catch((e) => toast.error(e.message));
  }, [actions]);

  const handleCheck = useCallback(() => {
    actions.check()?.catch((e) => toast.error(e.message));
  }, [actions]);

  const handleFold = useCallback(() => {
    actions.fold()?.catch((e) => toast.error(e.message));
  }, [actions]);

  const handleRaiseSubmit = useCallback(() => {
    handleBet(raiseAmount);
    setShowRaiseSlider(false);
  }, [raiseAmount, handleBet]);

  const canCheck = callAmount === 0;

  // Keyboard shortcuts (meaningful at lg:, where a keyboard is expected; harmless elsewhere since
  // mobile has no physical keyboard to trigger these): active on the hero's turn, online, and not
  // while typing in an input or textarea.
  useEffect(() => {
    if (!isMyTurn || !online) return;
    const onKeyDown = (e) => {
      const tag = document.activeElement?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA') return;
      if (e.key === 'f' || e.key === 'F') handleFold();
      else if (e.key === 'c' || e.key === 'C') (canCheck ? handleCheck() : handleBet(callAmount));
      else if (e.key === 'r' || e.key === 'R') handleRaiseOpen();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [isMyTurn, online, canCheck, callAmount, handleFold, handleCheck, handleBet, handleRaiseOpen]);

  const containerClass = 'fixed lg:static bottom-0 inset-x-0 z-30 p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))] bg-surface/95 backdrop-blur border-t border-border';

  if (game?.status === 'WAITING') {
    return (
      <div className={containerClass}>
        <div className="flex items-center gap-3 max-w-xl mx-auto">
          <Button
            className="flex-1 h-12"
            disabled={disabled || (game.players?.length ?? 0) < 2}
            onClick={() => actions.startHand()?.catch((e) => toast.error(e.message))}
          >
            Start hand
          </Button>
          <AddBotButton gameId={game.id} disabled={disabled} />
        </div>
      </div>
    );
  }

  if (!isMyTurn) {
    return (
      <div className={containerClass}>
        <div className="max-w-xl mx-auto">
          {hero?.isSittingOut ? (
            <Button
              variant="success"
              className="w-full h-12"
              disabled={disabled}
              onClick={() => actions.sitIn()?.catch((e) => toast.error(e.message))}
            >
              Sit in
            </Button>
          ) : (
            <Button
              variant="ghost"
              className="w-full h-12"
              disabled={disabled}
              onClick={() => actions.sitOut()?.catch((e) => toast.error(e.message))}
            >
              Sit out
            </Button>
          )}
        </div>
      </div>
    );
  }

  const snapPoints = [Math.floor(pot / 2), pot, maxRaise];
  const handleSnap = (snap) => setRaiseAmount(Math.min(snap, maxRaise));

  return (
    <div className={containerClass}>
      <div className="max-w-xl mx-auto">
        <AnimatePresence>
          {showRaiseSlider && (
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 12 }}
              transition={{ duration: 0.2 }}
              className="mb-3 bg-surface-elevated border border-border rounded-[var(--radius-lg)] p-4"
            >
              <div className="flex gap-2 mb-3">
                {snapPoints.map((snap, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => handleSnap(snap)}
                    className={`flex-1 py-1.5 rounded-[var(--radius)] text-xs font-semibold border transition-all duration-150 ${
                      raiseAmount === snap
                        ? 'bg-neon-cyan text-text-inverse border-neon-cyan'
                        : 'bg-surface text-text-muted border-border hover:text-text'
                    }`}
                  >
                    {SNAP_LABELS[i]}
                  </button>
                ))}
              </div>
              <Slider
                min={minRaise}
                max={maxRaise}
                step={game.bigBlindAmount ?? 1}
                value={[raiseAmount]}
                onValueChange={([v]) => setRaiseAmount(v)}
                className="mb-3"
              />
              <div className="flex items-center justify-between">
                <span className="text-text-dim text-xs">Raise to</span>
                <span className="text-neon-cyan font-bold text-lg">{formatChips(raiseAmount)}</span>
              </div>
              <Button className="w-full h-11 mt-3 glow-cyan" disabled={disabled} onClick={handleRaiseSubmit}>
                Raise to {formatChips(raiseAmount)}
              </Button>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="grid grid-cols-3 gap-2">
          <Button
            variant="danger"
            className="h-14 font-bold text-base"
            disabled={disabled}
            aria-label="Fold"
            onClick={handleFold}
          >
            Fold
          </Button>

          <Button
            variant="surface"
            className="h-14 font-bold text-base"
            disabled={disabled}
            aria-label={canCheck ? 'Check' : `Call ${formatChips(callAmount)}`}
            onClick={() => (canCheck ? handleCheck() : handleBet(callAmount))}
          >
            {canCheck ? 'Check' : `Call ${formatChips(callAmount)}`}
          </Button>

          <Button
            variant="default"
            className="h-14 font-bold text-base glow-cyan"
            disabled={disabled}
            aria-label="Raise"
            onClick={handleRaiseOpen}
          >
            Raise
          </Button>
        </div>
      </div>
    </div>
  );
}
