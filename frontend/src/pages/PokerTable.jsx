import React, { useEffect, useState, useContext, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import { useGame } from '@/hooks/useGame';
import { toast } from 'sonner';

import OvalTable from '@/components/poker/OvalTable';
import BettingControls from '@/components/poker/BettingControls';
import AddBotButton from '@/components/poker/AddBotButton';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import {
  ChevronLeft, ChevronRight, MessageSquare, Trophy, BarChart3,
  LogOut, Coffee, Play, Send, Loader2, Wifi, WifiOff,
} from 'lucide-react';
import { cn, formatChips, STATUS_LABELS, ACTION_LABELS } from '@/lib/utils';

/* ─── Constants ──────────────────────────────────────── */
const GAME_STATUS = {
  GAME_STARTED: 'GAME_STARTED',
  PLAYER_JOINED: 'PLAYER_JOINED',
  PLAYER_ACTION: 'PLAYER_ACTION',
  CARDS_DEALT: 'CARDS_DEALT',
  ROUND_STARTED: 'ROUND_STARTED',
  COMMUNITY_CARDS: 'COMMUNITY_CARDS',
  GAME_ENDED: 'GAME_ENDED',
};

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

/* ─── Chat panel ─────────────────────────────────────── */
function ChatPanel({ messages, onSend }) {
  const [text, setText] = useState('');
  const handleSend = () => {
    if (!text.trim()) return;
    onSend(text.trim());
    setText('');
  };
  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 overflow-y-auto px-3 py-2 space-y-1.5 scrollbar-none">
        {messages.length === 0 ? (
          <p className="text-text-dim text-xs text-center mt-6">No messages yet</p>
        ) : (
          messages.map((m, i) => (
            <div key={i} className="text-xs">
              <span className="text-gold font-semibold">{m.username}: </span>
              <span className="text-text-muted">{m.message}</span>
            </div>
          ))
        )}
      </div>
      <div className="flex gap-2 p-3 border-t border-border">
        <Input
          value={text}
          onChange={e => setText(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleSend()}
          placeholder="Say something…"
          className="text-xs h-8"
        />
        <Button size="icon-sm" onClick={handleSend} aria-label="Send chat message">
          <Send size={13} />
        </Button>
      </div>
    </div>
  );
}

/* ─── Rankings cheatsheet ────────────────────────────── */
function RankingsPanel() {
  return (
    <div className="px-3 py-2 space-y-1.5">
      {HAND_RANKINGS.map(([name, desc], i) => (
        <div key={i} className="flex items-center justify-between py-1.5 border-b border-border/50 last:border-0">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-gold min-w-[14px]">{10 - i}</span>
            <span className="text-xs font-semibold text-text">{name}</span>
          </div>
          <span className="text-[10px] text-text-dim text-right max-w-[100px] leading-tight">{desc}</span>
        </div>
      ))}
    </div>
  );
}

/* ─── Left sidebar ───────────────────────────────────── */
function LeftSidebar({ open, onToggle, game, currentPlayer, leaveTable, sitOut, sitIn, gameId }) {
  const isWaiting = game?.status === 'WAITING';
  const hasEmptySeat = (game?.playerCount ?? 0) < (game?.maxPlayers ?? 6);
  const [leaving, setLeaving] = useState(false);
  const handleLeave = async () => {
    setLeaving(true);
    await leaveTable();
  };

  return (
    <div className={cn(
      'relative flex flex-col shrink-0 transition-all duration-300 ease-in-out',
      'border-r border-border bg-surface',
      open ? 'w-56' : 'w-10',
    )}>
      {/* Toggle */}
      <button
        onClick={onToggle}
        className="absolute -right-3 top-4 z-10 w-6 h-6 rounded-full bg-surface border border-border flex items-center justify-center hover:bg-surface-elevated transition-colors"
        aria-label={open ? 'Collapse sidebar' : 'Expand sidebar'}
      >
        {open ? <ChevronLeft size={12} /> : <ChevronRight size={12} />}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col flex-1 overflow-hidden"
          >
            {/* Game info */}
            <div className="p-4 border-b border-border space-y-2">
              <h3 className="text-xs font-semibold text-text-dim uppercase tracking-wider">Table Info</h3>
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <span className="text-text-dim">Stakes</span>
                  <span className="text-gold font-mono font-semibold">
                    ${game?.smallBlindAmount}/{game?.bigBlindAmount}
                  </span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-text-dim">Players</span>
                  <span className="text-text font-medium">{game?.playerCount ?? 0}/{game?.maxPlayers ?? 8}</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-text-dim">Your stack</span>
                  <span className="text-text font-mono">{formatChips(currentPlayer?.chips ?? 0)}</span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-text-dim">Status</span>
                  <Badge variant={game?.status === 'ACTIVE' ? 'active' : 'waiting'} className="text-[9px] px-1.5">
                    {STATUS_LABELS[game?.status] ?? game?.status}
                  </Badge>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="p-4 space-y-2">
              <h3 className="text-xs font-semibold text-text-dim uppercase tracking-wider">Actions</h3>
              {currentPlayer?.isSittingOut ? (
                <Button variant="success" size="sm" className="w-full gap-2 text-xs" onClick={sitIn}>
                  <Play size={12} /> Sit Back In
                </Button>
              ) : (
                <Button variant="ghost" size="sm" className="w-full gap-2 text-xs text-text-muted hover:text-text" onClick={sitOut}>
                  <Coffee size={12} /> Sit Out
                </Button>
              )}
              <Button
                variant="ghost"
                size="sm"
                className="w-full gap-2 text-xs text-danger hover:bg-danger-muted"
                onClick={handleLeave}
                disabled={leaving}
              >
                {leaving ? <Loader2 size={12} className="animate-spin" /> : <LogOut size={12} />}
                {leaving ? 'Leaving…' : 'Leave Table'}
              </Button>
            </div>

            {/* Add Bot */}
            {isWaiting && hasEmptySeat && (
              <div className="p-4 border-t border-border">
                <h3 className="text-xs font-semibold text-text-dim uppercase tracking-wider mb-3">Add Bot</h3>
                <AddBotButton gameId={gameId} />
              </div>
            )}

            {/* Game log */}
            <div className="flex-1 overflow-hidden p-4 border-t border-border">
              <h3 className="text-xs font-semibold text-text-dim uppercase tracking-wider mb-2">Last Actions</h3>
              <div className="space-y-1 overflow-y-auto max-h-40 scrollbar-none">
                {Object.entries(game?.lastActions || {}).slice(-6).map(([username, action], i) => (
                  <div key={i} className="text-[10px] text-text-dim border-b border-border/30 pb-1 last:border-0">
                    {username}: {ACTION_LABELS[action] ?? action}
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ─── Right sidebar ──────────────────────────────────── */
function RightSidebar({ open, onToggle, chatMessages, onSendChat }) {
  return (
    <div className={cn(
      'relative flex flex-col shrink-0 transition-all duration-300 ease-in-out',
      'border-l border-border bg-surface',
      open ? 'w-64' : 'w-10',
    )}>
      {/* Toggle */}
      <button
        onClick={onToggle}
        className="absolute -left-3 top-4 z-10 w-6 h-6 rounded-full bg-surface border border-border flex items-center justify-center hover:bg-surface-elevated transition-colors"
        aria-label={open ? 'Collapse panel' : 'Expand panel'}
      >
        {open ? <ChevronRight size={12} /> : <ChevronLeft size={12} />}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col flex-1 overflow-hidden"
          >
            <Tabs defaultValue="chat" className="flex flex-col flex-1 overflow-hidden">
              <div className="px-3 pt-3 shrink-0">
                <TabsList className="w-full grid grid-cols-2 h-8">
                  <TabsTrigger value="chat" className="text-xs gap-1">
                    <MessageSquare size={11} /> Chat
                  </TabsTrigger>
                  <TabsTrigger value="rankings" className="text-xs gap-1">
                    <Trophy size={11} /> Hands
                  </TabsTrigger>
                </TabsList>
              </div>
              <TabsContent value="chat" className="flex-1 overflow-hidden mt-0">
                <div className="h-full flex flex-col" style={{ maxHeight: 'calc(100vh - 140px)' }}>
                  <ChatPanel messages={chatMessages} onSend={onSendChat} />
                </div>
              </TabsContent>
              <TabsContent value="rankings" className="flex-1 overflow-y-auto mt-0 scrollbar-none">
                <RankingsPanel />
              </TabsContent>
            </Tabs>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ─── Mobile action bar ──────────────────────────────── */
function MobileActionBar({ isMyTurn, game, currentPlayer, placeBet, check, fold }) {
  return (
    <div className="fixed bottom-0 inset-x-0 z-30 pb-[env(safe-area-inset-bottom)] bg-black/90 border-t border-white/10 backdrop-blur-sm">
      <div className="px-4 py-3">
        {isMyTurn ? (
          <BettingControls
            isMyTurn={isMyTurn}
            game={game}
            currentPlayer={currentPlayer}
            placeBet={placeBet}
            check={check}
            fold={fold}
          />
        ) : (
          <div className="h-14 flex items-center justify-center">
            <div className="flex items-center gap-2 text-text-dim text-sm">
              <div className="w-2 h-2 rounded-full bg-text-dim animate-pulse" />
              Waiting for your turn…
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/* ─── Start hand button ──────────────────────────────── */
function StartHandOverlay({ game, onStart, currentPlayer }) {
  const [starting, setStarting] = useState(false);
  const canStart = (game?.players?.length ?? game?.playerCount ?? 0) >= 2;
  const isDealer = game?.dealerPosition === currentPlayer?.index;

  const handleStart = async () => {
    if (!canStart) return;
    setStarting(true);
    try { await onStart(); }
    catch (e) { toast.error('Could not start hand.'); }
    finally { setStarting(false); }
  };

  if (!isDealer && !canStart) return null;

  return (
    <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/50 rounded-full pointer-events-none">
      <div className="pointer-events-auto">
        <Button onClick={handleStart} disabled={starting || !canStart} className="gap-2 shadow-lg">
          {starting ? <Loader2 size={16} className="animate-spin" /> : <Play size={16} />}
          {starting ? 'Starting…' : canStart ? 'Start Hand' : `Need ${2 - (game?.playerCount ?? 0)} more player`}
        </Button>
      </div>
    </div>
  );
}

/* ─── Main PokerTable page ───────────────────────────── */
const PokerTable = () => {
  const { gameId } = useParams();
  const navigate = useNavigate();
  const { user, isLoggedIn } = useContext(AuthContext);

  const [game, setGame] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentPlayer, setCurrentPlayer] = useState({ id: null, username: null, hand: [], index: null });
  const [isMyTurn, setIsMyTurn] = useState(false);
  const [chatMessages, setChatMessages] = useState([]);
  const [leftOpen, setLeftOpen] = useState(true);
  const [rightOpen, setRightOpen] = useState(true);

  const {
    game: queriedGame,
    gameLoading,
    gameError,
    gameUpdate,
    playerUpdate,
    joinGame,
    doAction,
    startHand,
    sendChat,
  } = useGame(gameId, currentPlayer.id);

  /* ── Join on mount ── */
  useEffect(() => {
    // If we're not logged in, redirect to login
    if (!isLoggedIn) {
      navigate('/login', { state: { from: `/game/${gameId}` } });
      return;
    }

    const doJoin = async () => {
      try {
        setLoading(true);
        const { data } = await joinGame();
        const joinedGame = data?.joinGame;
        if (!joinedGame) { 
          setError('Failed to join the game (no game data).'); 
          setLoading(false); 
          return; 
        }
        setGame(joinedGame);
        const playerIndex = joinedGame.players.findIndex(p => p.username === user.username);
        if (playerIndex !== -1) {
          const player = joinedGame.players[playerIndex];
          setCurrentPlayer({ 
            id: player.id, 
            username: player.username, 
            hand: player.hand || [], 
            index: playerIndex, 
            chips: player.chips, 
            isSittingOut: player.isSittingOut 
          });
        } else {
          setError('Failed to find player in game.');
        }
        setLoading(false);
      } catch (err) {
        console.error('Error joining game:', err);
        setError('Failed to join the game.');
        setLoading(false);
      }
    };
    // Only join if we have the user object
    if (gameId && user) {
      doJoin();
    }
  }, [gameId, user, isLoggedIn, navigate]);


  /* ── Game-wide subscription ── */
  useEffect(() => {
    if (gameUpdate) {
      setGame(prevGame => {
        if (!gameUpdate.type) return gameUpdate;
        const { type, payload } = gameUpdate;
        switch (type) {
          case GAME_STATUS.CARDS_DEALT:
            setCurrentPlayer(prev => Array.isArray(payload) ? { ...prev, hand: payload } : prev);
            return prevGame;
          case GAME_STATUS.COMMUNITY_CARDS:
            return { ...prevGame, communityCards: Array.isArray(payload) ? payload : (prevGame?.communityCards || []) };
          case GAME_STATUS.PLAYER_JOINED:
            toast.info(`${payload?.username || 'A player'} joined.`);
            return { ...prevGame, players: [...(prevGame?.players || []), payload] };
          case GAME_STATUS.PLAYER_ACTION: {
            const g = payload.game || payload;
            return { ...g, communityCards: g.communityCards || [], players: g.players || [] };
          }
          case GAME_STATUS.GAME_STARTED:
          case GAME_STATUS.ROUND_STARTED:
            return { ...payload, communityCards: payload.communityCards || [], players: payload.players || [] };
          case GAME_STATUS.GAME_ENDED: {
            const g = payload.game || payload;
            const winners = payload.winners || [];
            if (winners.length) toast.success(`${winners[0]?.username} wins ${formatChips(winners[0]?.lastWinAmount)}!`);
            return { ...g, winners, bestHand: payload.bestHand || null, status: 'ENDED' };
          }
          default:
            return payload.game || payload || prevGame;
        }
      });
    }
  }, [gameUpdate]);

  /* ── Player-specific subscription ── */
  useEffect(() => {
    if (playerUpdate) {
      setGame(prevGame => {
        if (!playerUpdate.type) return playerUpdate;
        const { type, payload } = playerUpdate;
        switch (type) {
          case GAME_STATUS.CARDS_DEALT:
            setCurrentPlayer(prev => Array.isArray(payload) ? { ...prev, hand: payload } : prev);
            return prevGame;
          case GAME_STATUS.PLAYER_ACTION: {
            const g = payload.game || payload;
            return { ...g, communityCards: g.communityCards || [], players: g.players || [] };
          }
          default:
            return payload.game || payload || prevGame;
        }
      });
    }
  }, [playerUpdate]);

  /* ── Track turn ── */
  const ACTIVE_BETTING_STATUSES = ['PRE_FLOP_BETTING', 'FLOP_BETTING', 'TURN_BETTING', 'RIVER_BETTING'];

  useEffect(() => {
    if (game && currentPlayer.index !== null) {
      const inActiveBetting = ACTIVE_BETTING_STATUSES.includes(game.status);
      const isTurn = game.currentPlayerIndex === currentPlayer.index;
      const player = game.players?.[currentPlayer.index];
      const myTurn = inActiveBetting && isTurn && player && !player.hasFolded && !player.isSittingOut;
      setIsMyTurn(myTurn);
      if (myTurn) toast.info("It's your turn!", { id: 'your-turn', duration: 3000 });
    }
  }, [game, currentPlayer]);

  /* ── Sync chat from game events ── */
  useEffect(() => {
    if (gameUpdate?.type === 'CHAT' && gameUpdate.payload) {
      setChatMessages(prev => [...prev, gameUpdate.payload]);
    }
  }, [gameUpdate]);

  /* ── Actions ── */
  const placeBet = useCallback((amount) => doAction('BET', amount), [doAction]);
  const fold = useCallback(() => doAction('FOLD'), [doAction]);
  const check = useCallback(() => doAction('CHECK'), [doAction]);
  const leaveTable = useCallback(async () => { await doAction('LEAVE'); navigate('/lobby'); }, [doAction, navigate]);
  const sitOut = useCallback(() => doAction('SIT_OUT'), [doAction]);
  const sitIn = useCallback(() => doAction('SIT_IN'), [doAction]);
  const sendChatMessage = useCallback((message) => sendChat(message), [sendChat]);

  const getDisplayPosition = useCallback((actualIndex) => {
    if (!game || currentPlayer.index === null) return 0;
    return ((actualIndex - currentPlayer.index + game.players.length) % game.players.length);
  }, [game, currentPlayer.index]);

  const getBlindStatus = useCallback((player) => {
    if (!game) return null;
    if (game.bigBlindUserId === player.username)   return 'big-blind';
    if (game.smallBlindUserId === player.username) return 'small-blind';
    return null;
  }, [game]);

  /* ── Loading ── */
  if (loading) {
    return (
      <div className="flex-1 flex flex-col h-[calc(100dvh-3.5rem)] bg-background items-center justify-center gap-4">
        <div className="w-24 h-14 rounded-full felt-surface animate-pulse" />
        <p className="text-text-muted text-sm">Joining table…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-4 p-6">
        <p className="text-danger text-base">{error}</p>
        <Button onClick={() => navigate('/lobby')}>Back to Lobby</Button>
      </div>
    );
  }

  if (!game?.players) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center gap-4 p-6">
        <p className="text-text-muted">Invalid game data.</p>
        <Button onClick={() => navigate('/lobby')}>Back to Lobby</Button>
      </div>
    );
  }

  const isWaiting = game.status === 'WAITING' || game.status === 'ENDED';

  return (
    <div className="flex h-[calc(100dvh-3.5rem)] bg-background overflow-hidden">
      {/* ── Left sidebar (desktop) ── */}
      <div className="hidden md:flex">
        <LeftSidebar
          open={leftOpen}
          onToggle={() => setLeftOpen(v => !v)}
          game={game}
          currentPlayer={currentPlayer}
          leaveTable={leaveTable}
          sitOut={sitOut}
          sitIn={sitIn}
          gameId={gameId}
        />
      </div>

      {/* ── Center: table ── */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Desktop betting controls above table bottom */}
        <div className="flex-1 relative flex flex-col">
          {/* The table takes up most of the height */}
          <div className="flex-1 flex items-center justify-center px-4 py-4 relative">
            <div className="w-full max-w-3xl">
              <div className="relative">
                <OvalTable
                  game={game}
                  currentPlayer={currentPlayer}
                  isMyTurn={isMyTurn}
                  getDisplayPosition={getDisplayPosition}
                  getBlindStatus={getBlindStatus}
                />
                {/* Start hand overlay if game is waiting */}
                {isWaiting && (
                  <StartHandOverlay
                    game={game}
                    onStart={startHand}
                    currentPlayer={currentPlayer}
                  />
                )}
              </div>
            </div>
          </div>

          {/* Desktop action bar */}
          <div className="hidden md:block px-6 pb-4">
            <div className="max-w-xl mx-auto">
              <BettingControls
                isMyTurn={isMyTurn}
                game={game}
                currentPlayer={currentPlayer}
                placeBet={placeBet}
                check={check}
                fold={fold}
              />
            </div>
          </div>
        </div>
      </div>

      {/* ── Right sidebar (desktop) ── */}
      <div className="hidden md:flex">
        <RightSidebar
          open={rightOpen}
          onToggle={() => setRightOpen(v => !v)}
          chatMessages={chatMessages}
          onSendChat={sendChatMessage}
        />
      </div>

      {/* ── Mobile action bar ── */}
      <div className="md:hidden">
        <MobileActionBar
          isMyTurn={isMyTurn}
          game={game}
          currentPlayer={currentPlayer}
          placeBet={placeBet}
          check={check}
          fold={fold}
        />
      </div>
    </div>
  );
};

export default PokerTable;
