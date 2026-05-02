import React from 'react';
import { Link } from 'react-router-dom';

export default function AuthLayout({ children, title, subtitle, altText, altLink, altLinkLabel }) {
  return (
    <div className="flex-1 grid md:grid-cols-2 min-h-[calc(100dvh-3.5rem)]">
      {/* Brand panel (left on desktop) */}
      <div className="hidden md:flex flex-col items-center justify-center relative overflow-hidden bg-background">
        {/* Felt table art */}
        <div className="relative w-[360px] h-[200px]">
          {/* Table rim */}
          <div
            className="absolute inset-0 rounded-full"
            style={{
              background: 'linear-gradient(180deg, #8b6428 0%, #6b4c1e 50%, #4a3010 100%)',
              boxShadow: '0 4px 32px rgba(0,0,0,0.7), 0 0 0 6px #3d2800',
            }}
          />
          {/* Felt */}
          <div
            className="absolute inset-3 rounded-full felt-surface"
            style={{ boxShadow: 'inset 0 0 40px rgba(0,0,0,0.4)' }}
          />
          {/* Community cards placeholder */}
          <div className="absolute inset-0 flex items-center justify-center gap-2">
            {['A♠', 'K♥', 'Q♦', 'J♣', '10♠'].map((card, i) => (
              <div
                key={i}
                className="w-9 h-12 rounded bg-white shadow-lg flex items-center justify-center"
                style={{
                  animationName: 'fadeInUp',
                  animationDuration: '0.4s',
                  animationDelay: `${i * 0.08}s`,
                  animationFillMode: 'both',
                  fontSize: '11px',
                  fontWeight: 700,
                  color: card.includes('♥') || card.includes('♦') ? '#dc2626' : '#1c1c1c',
                }}
              >
                {card}
              </div>
            ))}
          </div>
        </div>

        <div className="mt-10 text-center px-8">
          <h1 className="font-serif text-4xl font-bold text-text mb-3">
            Play <span className="gold-text">Premium</span> Poker
          </h1>
          <p className="text-text-muted text-sm leading-relaxed max-w-xs">
            Real-money Texas Hold'em with live tables, real-time action,
            and a community of serious players.
          </p>
        </div>

        {/* Decorative chips */}
        <div className="absolute bottom-12 left-12 w-8 h-8 rounded-full bg-danger border-2 border-white/20 shadow-lg opacity-60" />
        <div className="absolute bottom-8 left-20 w-6 h-6 rounded-full bg-blue-600 border-2 border-white/20 shadow-lg opacity-50" />
        <div className="absolute top-16 right-16 w-7 h-7 rounded-full bg-gold border-2 border-white/20 shadow-lg opacity-60" />
      </div>

      {/* Form panel (right on desktop, full on mobile) */}
      <div className="flex items-center justify-center px-6 py-10 bg-background">
        <div className="w-full max-w-[380px]">
          <div className="mb-7">
            <Link to="/" className="inline-flex items-center gap-2 mb-6 group md:hidden">
              <div className="w-7 h-7 rounded-[var(--radius-sm)] bg-gold flex items-center justify-center">
                <span className="text-text-inverse font-bold text-xs">B</span>
              </div>
              <span className="font-serif text-xl font-bold text-text">Betrix</span>
            </Link>
            <h2 className="text-2xl font-semibold text-text">{title}</h2>
            {subtitle && <p className="text-text-muted text-sm mt-1">{subtitle}</p>}
          </div>

          {children}

          {altText && (
            <p className="mt-5 text-center text-sm text-text-muted">
              {altText}{' '}
              <Link to={altLink} className="text-gold hover:text-gold-light font-medium transition-colors">
                {altLinkLabel}
              </Link>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
