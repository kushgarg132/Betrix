import React from 'react';
import { Loader2, WifiOff } from 'lucide-react';
import { cn } from '@/lib/utils';

export default function ConnectionPill({ status }) {
  if (status === 'online') return null;
  const offline = status === 'offline';
  return (
    <div role="status" aria-live="polite" className={cn(
      'flex items-center gap-1.5 rounded-full px-3 h-8 text-xs font-medium border',
      offline ? 'bg-danger/15 text-danger border-danger/40' : 'bg-warning/15 text-warning border-warning/40')}>
      {offline ? <WifiOff size={14} aria-hidden="true" /> : <Loader2 size={14} className="animate-spin" aria-hidden="true" />}
      {offline ? 'Offline, actions paused' : 'Reconnecting…'}
    </div>
  );
}
