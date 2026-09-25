import React, { useEffect, useRef, useState } from 'react';
import { turnProgress } from '@/lib/turnProgress';

/** Countdown ring around the acting seat. Re-renders ~4x a second; stops when there is no deadline. */
export default function TurnRing({ deadline, timeoutSeconds, size = 56, isHero = false }) {
  const [now, setNow] = useState(() => Date.now());
  const buzzed = useRef(null);

  useEffect(() => {
    if (!deadline) return;
    const id = setInterval(() => setNow(Date.now()), 250);
    return () => clearInterval(id);
  }, [deadline]);

  useEffect(() => {
    if (isHero && deadline && buzzed.current !== deadline) {
      buzzed.current = deadline;
      navigator.vibrate?.(60);
    }
  }, [isHero, deadline]);

  const { fraction, urgent, remainingMs } = turnProgress(deadline, timeoutSeconds, now);
  const r = size / 2 - 3;
  const c = 2 * Math.PI * r;
  const color = urgent ? 'var(--color-warning)' : 'var(--color-neon-cyan)';

  return (
    <svg width={size} height={size} className="absolute -inset-1 -rotate-90 pointer-events-none" aria-hidden="true">
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--color-border-strong)" strokeWidth="3" />
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={color} strokeWidth="3" strokeLinecap="round"
        strokeDasharray={c} strokeDashoffset={c * (1 - fraction)}
        style={{
          filter: urgent ? undefined : `drop-shadow(0 0 4px ${color})`,
          transition: 'stroke-dashoffset 250ms linear',
        }} />
      {remainingMs != null && urgent && <title>{Math.ceil(remainingMs / 1000)}s left</title>}
    </svg>
  );
}
