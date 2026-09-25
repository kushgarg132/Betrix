import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import PageWrapper from '@/components/layout/PageWrapper';
import PokerCard from '@/components/poker/PokerCard';

export default function NotFound() {
  const navigate = useNavigate();
  return (
    <PageWrapper>
      <div className="flex flex-col items-center justify-center py-24 text-center gap-6">
        <div className="flex items-center gap-2">
          <PokerCard faceDown className="rotate-[-8deg]" />
          <PokerCard faceDown className="rotate-[8deg]" />
        </div>
        <h1 className="font-display text-2xl font-bold text-text">This hand folded</h1>
        <p className="text-text-muted text-sm max-w-xs">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <Button onClick={() => navigate('/lobby')}>Back to lobby</Button>
      </div>
    </PageWrapper>
  );
}
