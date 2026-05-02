import React, { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronUp, ChevronDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import { formatChips } from '@/lib/utils';

const SNAP_LABELS = ['½ Pot', 'Pot', 'All In'];

export default function BettingControls({ isMyTurn, game, currentPlayer, placeBet, check, fold }) {
  const [showRaiseSlider, setShowRaiseSlider] = useState(false);
  const [raiseAmount, setRaiseAmount] = useState(0);

  const myChips = currentPlayer?.chips ?? 0;
  const currentBet = game?.currentBet ?? 0;
  const playerCurrentBet = game?.currentBettingRound?.bets?.[currentPlayer?.id] ?? 0;
  const callAmount = Math.max(0, currentBet - playerCurrentBet);
  const pot = game?.pot ?? 0;
  const minRaise = Math.max(currentBet * 2, game?.bigBlindAmount ?? 0);
  const maxRaise = myChips + playerCurrentBet;

  const handleRaiseOpen = useCallback(() => {
    setRaiseAmount(minRaise);
    setShowRaiseSlider(true);
  }, [minRaise]);

  const handleRaiseSubmit = useCallback(() => {
    placeBet(raiseAmount);
    setShowRaiseSlider(false);
  }, [raiseAmount, placeBet]);

  if (!isMyTurn || !game) return null;

  const snapPoints = [
    Math.floor(pot / 2),
    pot,
    maxRaise,
  ];

  const handleSnap = (snapValue) => {
    setRaiseAmount(Math.min(snapValue, maxRaise));
  };

  const canCall = callAmount > 0 && myChips >= callAmount;
  const canCheck = callAmount === 0;
  const canRaise = myChips > callAmount;

  return (
    <div className="w-full">
      {/* Raise slider panel */}
      <AnimatePresence>
        {showRaiseSlider && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 12 }}
            transition={{ duration: 0.2 }}
            className="mb-3 bg-black/60 border border-white/10 rounded-xl p-4"
          >
            {/* Snap buttons */}
            <div className="flex gap-2 mb-3">
              {snapPoints.map((snap, i) => (
                <button
                  key={i}
                  onClick={() => handleSnap(snap)}
                  className={`flex-1 py-1.5 rounded-lg text-xs font-semibold border transition-all duration-150 ${
                    raiseAmount === snap
                      ? 'bg-gold text-text-inverse border-gold'
                      : 'bg-white/5 text-white/60 border-white/10 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  {SNAP_LABELS[i]}
                </button>
              ))}
            </div>

            {/* Slider */}
            <Slider
              min={minRaise}
              max={maxRaise}
              step={game.bigBlindAmount ?? 1}
              value={[raiseAmount]}
              onValueChange={([v]) => setRaiseAmount(v)}
              className="mb-3"
            />

            <div className="flex items-center justify-between">
              <span className="text-white/50 text-xs">Raise to</span>
              <span className="text-gold font-bold text-lg">{formatChips(raiseAmount)}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Action buttons */}
      <div className="grid grid-cols-3 gap-2">
        {/* Fold */}
        <Button
          variant="danger"
          size="xl"
          onClick={fold}
          className="h-14 font-bold text-base"
          aria-label="Fold hand"
        >
          Fold
        </Button>

        {/* Check or Call */}
        <Button
          variant="surface"
          size="xl"
          onClick={canCheck ? check : () => placeBet(callAmount)}
          className="h-14 font-bold text-base border-white/20 hover:border-white/40"
          aria-label={canCheck ? 'Check' : `Call ${formatChips(callAmount)}`}
        >
          {canCheck ? 'Check' : (
            <span className="flex flex-col items-center leading-tight">
              <span>Call</span>
              <span className="text-gold text-sm">{formatChips(callAmount)}</span>
            </span>
          )}
        </Button>

        {/* Raise */}
        {showRaiseSlider ? (
          <Button
            variant="default"
            size="xl"
            onClick={handleRaiseSubmit}
            className="h-14 font-bold text-base"
            aria-label={`Raise to ${formatChips(raiseAmount)}`}
          >
            <span className="flex flex-col items-center leading-tight">
              <span>Raise</span>
              <span className="text-text-inverse/70 text-sm">{formatChips(raiseAmount)}</span>
            </span>
          </Button>
        ) : (
          <Button
            variant="outline"
            size="xl"
            onClick={handleRaiseOpen}
            disabled={!canRaise}
            className="h-14 font-bold text-base"
            aria-label="Open raise slider"
          >
            Raise
          </Button>
        )}
      </div>
    </div>
  );
}
