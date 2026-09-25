import React, { useState } from 'react';
import { LogOut, MessageSquare } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import ConnectionPill from './ConnectionPill';
import { formatBlinds } from '@/lib/utils';

/** Table header: leave (with confirm), table id + blinds, connection pill, chat toggle. */
export default function TableTopBar({ game, status, onLeave, onOpenChat, unread }) {
  const [confirmOpen, setConfirmOpen] = useState(false);

  return (
    <div className="h-14 flex items-center gap-2 px-3 border-b border-border bg-surface/80 backdrop-blur">
      <Button variant="ghost" size="icon" aria-label="Leave table" onClick={() => setConfirmOpen(true)}>
        <LogOut size={18} />
      </Button>

      <div className="flex-1 flex items-center justify-center gap-2 font-mono text-sm text-text-muted">
        <span>Table #{game?.id?.slice(-4)}</span>
        <span className="text-text-dim">{formatBlinds(game?.smallBlindAmount, game?.bigBlindAmount)}</span>
      </div>

      <ConnectionPill status={status} />

      <Button
        variant="ghost"
        size="icon"
        className="relative lg:hidden"
        aria-label="Open chat"
        onClick={onOpenChat}
      >
        <MessageSquare size={18} />
        {unread > 0 && <span className="absolute top-1.5 right-1.5 size-2 rounded-full bg-neon-magenta" />}
      </Button>

      <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Leave the table?</DialogTitle>
            <DialogDescription>Your chips at this table are forfeited.</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setConfirmOpen(false)}>Stay</Button>
            <Button variant="danger" onClick={() => { setConfirmOpen(false); onLeave(); }}>Leave</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
