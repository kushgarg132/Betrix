import React, { useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AuthContext } from '@/context/AuthContext';
import { Button } from '@/components/ui/button';
import PageWrapper from '@/components/layout/PageWrapper';
import { Zap, Shield, BarChart3, Users, ArrowRight, Star } from 'lucide-react';

const FEATURES = [
  {
    icon: Zap,
    title: 'Real-Time Action',
    desc: 'Sub-second updates via WebSocket subscriptions. Every bet, fold, and raise lands instantly.',
    color: 'text-gold',
    bg: 'bg-gold-muted',
  },
  {
    icon: Shield,
    title: 'Secure & Fair',
    desc: 'JWT-authenticated sessions, server-side hand evaluation, and tamper-proof game state.',
    color: 'text-success',
    bg: 'bg-success-muted',
  },
  {
    icon: BarChart3,
    title: 'Track Your Edge',
    desc: 'Hands played, win rate, and net profit across every session — all in your profile.',
    color: 'text-info',
    bg: 'bg-blue-900/20',
  },
  {
    icon: Users,
    title: 'Up to 8 Players',
    desc: 'Fill a full table or play heads-up. Choose your stakes from micro to high-roller.',
    color: 'text-warning',
    bg: 'bg-amber-900/20',
  },
];

const STATS = [
  { label: 'Active Tables', value: '12+' },
  { label: 'Hands Dealt Today', value: '4.2K' },
  { label: 'Players Online', value: '87' },
  { label: 'Biggest Pot', value: '$12K' },
];

function TablePreview() {
  return (
    <div className="relative w-full max-w-[480px] mx-auto" style={{ paddingTop: '55%' }}>
      <div className="absolute inset-0">
        <div
          className="absolute inset-0 rounded-full"
          style={{
            background: 'linear-gradient(180deg, #8b6428 0%, #6b4c1e 50%, #4a3010 100%)',
            boxShadow: '0 8px 48px rgba(0,0,0,0.9), 0 0 0 3px #3d2800',
          }}
        />
        <div
          className="absolute felt-surface rounded-full"
          style={{ inset: 10, boxShadow: 'inset 0 0 60px rgba(0,0,0,0.5)' }}
        />
        <div
          className="absolute rounded-full pointer-events-none"
          style={{ inset: 24, border: '1px solid rgba(255,255,255,0.06)' }}
        />
        <div className="absolute inset-0 flex items-center justify-center gap-2">
          {[
            { rank: 'A', suit: '♠', red: false },
            { rank: 'K', suit: '♥', red: true },
            { rank: 'Q', suit: '♦', red: true },
            { rank: 'J', suit: '♣', red: false },
            { rank: '10', suit: '♠', red: false },
          ].map((c, i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0, y: -10, rotateY: 90 }}
              animate={{ opacity: 1, y: 0, rotateY: 0 }}
              transition={{ delay: 0.4 + i * 0.1, duration: 0.4, type: 'spring' }}
              className="bg-white rounded shadow-lg flex items-center justify-center"
              style={{ width: 36, height: 52, perspective: 600 }}
            >
              <div className={`text-xs font-bold leading-tight text-center ${c.red ? 'text-red-600' : 'text-gray-900'}`}>
                <div>{c.rank}</div>
                <div>{c.suit}</div>
              </div>
            </motion.div>
          ))}
        </div>
        {[
          { left: '50%', top: '10%' },
          { left: '83%', top: '25%' },
          { left: '83%', top: '75%' },
          { left: '50%', top: '90%' },
          { left: '17%', top: '75%' },
          { left: '17%', top: '25%' },
        ].map((pos, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, scale: 0.5 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 + i * 0.07, type: 'spring' }}
            className="absolute w-9 h-9 rounded-full bg-surface-elevated border-2 border-border flex items-center justify-center text-xs font-bold text-text-muted"
            style={{ left: pos.left, top: pos.top, transform: 'translate(-50%, -50%)' }}
          >
            P{i + 1}
          </motion.div>
        ))}
      </div>
    </div>
  );
}

export default function Home() {
  const { isLoggedIn } = useContext(AuthContext);
  const navigate = useNavigate();

  return (
    <div className="flex-1 flex flex-col">
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-br from-background via-background to-felt-dark/20 pointer-events-none" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_60%_50%,rgba(26,92,46,0.08),transparent_70%)] pointer-events-none" />
        <PageWrapper>
          <div className="py-16 lg:py-24 grid lg:grid-cols-2 gap-12 lg:gap-8 items-center">
            <motion.div
              initial={{ opacity: 0, x: -24 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5 }}
              className="space-y-6"
            >
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-gold-muted border border-border-gold text-gold text-xs font-semibold">
                <Star size={12} fill="currentColor" />
                Premium Texas Hold'em
              </div>
              <h1 className="font-serif text-4xl sm:text-5xl lg:text-6xl font-bold text-text leading-[1.1] text-balance">
                Real Money Poker,{' '}
                <span className="gold-text">Real Stakes</span>
              </h1>
              <p className="text-text-muted text-lg leading-relaxed max-w-lg">
                Jump into a live table in seconds. Play Texas Hold'em with real players,
                real chips, and real-time action — from micro to high-roller stakes.
              </p>
              <div className="flex flex-wrap gap-3 pt-2">
                {isLoggedIn ? (
                  <Button size="xl" onClick={() => navigate('/lobby')} className="gap-2">
                    Find a Table <ArrowRight size={18} />
                  </Button>
                ) : (
                  <>
                    <Button size="xl" onClick={() => navigate('/register')} className="gap-2">
                      Play Now — Free <ArrowRight size={18} />
                    </Button>
                    <Button size="xl" variant="outline" onClick={() => navigate('/login')}>
                      Sign In
                    </Button>
                  </>
                )}
              </div>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 24 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5, delay: 0.15 }}
              className="lg:pl-8"
            >
              <TablePreview />
            </motion.div>
          </div>
        </PageWrapper>
      </section>

      {/* Stats strip */}
      <section className="border-y border-border bg-surface/60">
        <PageWrapper>
          <div className="py-5 grid grid-cols-2 sm:grid-cols-4 gap-4">
            {STATS.map((s, i) => (
              <motion.div
                key={s.label}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.4 + i * 0.05 }}
                className="text-center"
              >
                <div className="text-2xl font-bold font-mono gold-text">{s.value}</div>
                <div className="text-text-dim text-xs mt-0.5">{s.label}</div>
              </motion.div>
            ))}
          </div>
        </PageWrapper>
      </section>

      {/* Features bento grid */}
      <section className="py-16">
        <PageWrapper>
          <div className="text-center mb-10">
            <h2 className="font-serif text-3xl font-bold text-text mb-3">Built for Serious Players</h2>
            <p className="text-text-muted max-w-md mx-auto text-sm">
              Professional-grade poker with the speed and reliability you need to play your best game.
            </p>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {FEATURES.map((f, i) => (
              <motion.div
                key={f.title}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 * i, duration: 0.4 }}
                className="bg-surface border border-border rounded-[var(--radius-xl)] p-5 hover:border-border-gold transition-colors"
              >
                <div className={`inline-flex items-center justify-center w-10 h-10 rounded-[var(--radius-lg)] ${f.bg} mb-4`}>
                  <f.icon size={20} className={f.color} />
                </div>
                <h3 className="font-semibold text-text mb-1.5">{f.title}</h3>
                <p className="text-text-muted text-sm leading-relaxed">{f.desc}</p>
              </motion.div>
            ))}
          </div>
        </PageWrapper>
      </section>

      {/* CTA */}
      {!isLoggedIn && (
        <section className="py-16 bg-gradient-to-t from-surface/40 to-transparent">
          <PageWrapper>
            <div className="text-center space-y-5">
              <h2 className="font-serif text-3xl font-bold text-text">Ready to Play?</h2>
              <p className="text-text-muted text-sm">Create a free account and be at a table in under 60 seconds.</p>
              <Button size="xl" onClick={() => navigate('/register')} className="gap-2 mx-auto">
                Start Playing Free <ArrowRight size={18} />
              </Button>
            </div>
          </PageWrapper>
        </section>
      )}

      <footer className="border-t border-border py-6 mt-auto">
        <PageWrapper>
          <p className="text-center text-text-dim text-xs">
            © {new Date().getFullYear()} Betrix · Premium Texas Hold'em · Play responsibly
          </p>
        </PageWrapper>
      </footer>
    </div>
  );
}
