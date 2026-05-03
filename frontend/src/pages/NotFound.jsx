import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import PageWrapper from '@/components/layout/PageWrapper';

export default function NotFound() {
  const navigate = useNavigate();
  return (
    <PageWrapper>
      <div className="flex flex-col items-center justify-center py-24 text-center gap-5">
        <p className="text-8xl font-bold font-mono text-text-dim">404</p>
        <h1 className="text-2xl font-semibold text-text">Page not found</h1>
        <p className="text-text-muted text-sm max-w-xs">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <Button onClick={() => navigate('/lobby')}>Go to Lobby</Button>
      </div>
    </PageWrapper>
  );
}
