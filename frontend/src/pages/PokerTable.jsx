import React, { useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { AuthContext } from '@/context/AuthContext';
import { useGame } from '@/hooks/useGame';
import { useConnectionStatus } from '@/hooks/useConnectionStatus';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import TableScene from '@/components/poker/TableScene';
import ActionBar from '@/components/poker/ActionBar';
import TableTopBar from '@/components/poker/TableTopBar';
import ChatDrawer from '@/components/poker/ChatDrawer';
import { Skeleton } from '@/components/ui/skeleton';

export default function PokerTable() {
  const { gameId } = useParams();
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const desktop = useMediaQuery('(min-width: 1024px)');
  const connection = useConnectionStatus();
  const t = useGame(gameId, user?.username);
  const [chatOpen, setChatOpen] = useState(false);
  const [seenChat, setSeenChat] = useState(0);

  useEffect(() => { if (chatOpen || desktop) setSeenChat(t.chat.length); }, [chatOpen, desktop, t.chat.length]);
  useEffect(() => {
    if (t.status === 'error') { toast.error(t.error); navigate('/lobby', { replace: true }); }
  }, [t.status]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (t.status === 'ready' && t.heroIndex === -1) { toast.info('You are no longer seated at this table.'); navigate('/lobby', { replace: true }); }
  }, [t.status, t.heroIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  if (t.status !== 'ready') {
    return (
      <div className="min-h-dvh flex flex-col items-center justify-center gap-6 p-6" aria-busy="true">
        <Skeleton className="w-full max-w-sm aspect-[3/4] rounded-[50%]" />
        <p className="text-text-muted text-sm">Taking a seat…</p>
      </div>
    );
  }

  const leave = async () => {
    try { await t.actions.leave(); } catch (e) { toast.error(e.message); }
    navigate('/lobby');
  };

  return (
    <div className="h-dvh flex flex-col bg-background overflow-hidden">
      <TableTopBar game={t.game} status={connection} onLeave={leave}
        onOpenChat={() => setChatOpen(true)} unread={t.chat.length - seenChat} />
      <div className="flex-1 flex min-h-0">
        <main className="flex-1 flex flex-col min-h-0">
          <div className="flex-1 flex items-center justify-center p-4 pb-40 lg:pb-4 min-h-0">
            <div className="w-full max-w-md lg:max-w-4xl">
              <TableScene game={t.game} heroIndex={t.heroIndex} hand={t.hand}
                orientation={desktop ? 'landscape' : 'portrait'} />
            </div>
          </div>
          <ActionBar game={t.game} hero={t.hero} heroIndex={t.heroIndex} isMyTurn={t.isMyTurn}
            online={connection === 'online'} actions={t.actions} />
        </main>
        {desktop && <ChatDrawer docked messages={t.chat} heroId={t.hero?.id} onSend={t.actions.sendChat} />}
      </div>
      {!desktop && <ChatDrawer open={chatOpen} onClose={() => setChatOpen(false)} messages={t.chat}
        heroId={t.hero?.id} onSend={t.actions.sendChat} />}
      <div aria-live="polite" className="sr-only">{t.isMyTurn ? 'Your turn' : ''}</div>
    </div>
  );
}
