import React, { useState } from 'react';
import { MessageSquare, Send, Trophy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import BottomSheet from '@/components/layout/BottomSheet';
import { cn } from '@/lib/utils';

const HAND_RANKINGS = [
  ['Royal Flush',     '10♠ J♠ Q♠ K♠ A♠'],
  ['Straight Flush',  '5-9 same suit'],
  ['Four of a Kind',  '4 of the same rank'],
  ['Full House',      'Three + Pair'],
  ['Flush',           '5 same suit'],
  ['Straight',        '5 consecutive'],
  ['Three of a Kind', '3 same rank'],
  ['Two Pair',        '2 different pairs'],
  ['Pair',            '2 same rank'],
  ['High Card',       'Best single card'],
];

function ChatPanel({ messages, heroId, onSend }) {
  const [text, setText] = useState('');
  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText('');
  };
  return (
    <div className="flex flex-col h-full min-h-0">
      <div className="flex-1 overflow-y-auto px-3 py-2 space-y-1.5 scrollbar-none">
        {messages.length === 0 ? (
          <p className="text-text-dim text-xs text-center mt-6">No messages yet</p>
        ) : (
          messages.map((m) => (
            <div key={`${m.timestamp}-${m.senderId}`} className="text-xs">
              <span className={cn('font-semibold', m.senderId === heroId ? 'text-neon-cyan' : 'text-text')}>
                {m.senderName}:{' '}
              </span>
              <span className="text-text-muted">{m.message}</span>
            </div>
          ))
        )}
      </div>
      <div className="flex gap-2 p-3 border-t border-border shrink-0">
        <Input
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="Say something…"
          maxLength={500}
          className="h-11 text-sm"
        />
        <Button size="icon" onClick={handleSend} aria-label="Send chat message">
          <Send size={16} />
        </Button>
      </div>
    </div>
  );
}

function RankingsPanel() {
  return (
    <div className="px-3 py-2 space-y-1.5">
      {HAND_RANKINGS.map(([name, desc], i) => (
        <div key={name} className="flex items-center justify-between py-1.5 border-b border-border/50 last:border-0">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-neon-cyan min-w-[14px]">{10 - i}</span>
            <span className="text-xs font-semibold text-text">{name}</span>
          </div>
          <span className="text-xs text-text-dim text-right max-w-[120px] leading-tight">{desc}</span>
        </div>
      ))}
    </div>
  );
}

function ChatTabs({ messages, heroId, onSend }) {
  return (
    <Tabs defaultValue="chat" className="flex flex-col flex-1 min-h-0">
      <div className="px-3 pt-3 shrink-0">
        <TabsList className="w-full grid grid-cols-2 h-9">
          <TabsTrigger value="chat" className="gap-1">
            <MessageSquare size={13} /> Chat
          </TabsTrigger>
          <TabsTrigger value="rankings" className="gap-1">
            <Trophy size={13} /> Hands
          </TabsTrigger>
        </TabsList>
      </div>
      <TabsContent value="chat" className="flex-1 min-h-0 overflow-hidden">
        <ChatPanel messages={messages} heroId={heroId} onSend={onSend} />
      </TabsContent>
      <TabsContent value="rankings" className="flex-1 min-h-0 overflow-y-auto scrollbar-none">
        <RankingsPanel />
      </TabsContent>
    </Tabs>
  );
}

/** Docked right panel at lg:, a BottomSheet everywhere else. */
export default function ChatDrawer({ open, onClose, messages, heroId, onSend, docked }) {
  if (docked) {
    return (
      <aside className="w-80 border-l border-border bg-surface flex flex-col shrink-0">
        <ChatTabs messages={messages} heroId={heroId} onSend={onSend} />
      </aside>
    );
  }

  return (
    <BottomSheet open={open} onClose={onClose} title="Table chat">
      <div className="flex flex-col h-[55vh]">
        <ChatTabs messages={messages} heroId={heroId} onSend={onSend} />
      </div>
    </BottomSheet>
  );
}
