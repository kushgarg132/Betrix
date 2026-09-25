import React, { useEffect, useRef } from 'react';

const GSI_SRC = 'https://accounts.google.com/gsi/client';
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

function loadGsi() {
  if (window.google?.accounts?.id) return Promise.resolve();
  const existing = document.querySelector(`script[src="${GSI_SRC}"]`);
  return new Promise((resolve, reject) => {
    const s = existing || Object.assign(document.createElement('script'), { src: GSI_SRC, async: true, defer: true });
    s.addEventListener('load', resolve, { once: true });
    s.addEventListener('error', () => reject(new Error('Could not load Google sign-in.')), { once: true });
    if (!existing) document.head.appendChild(s);
  });
}

/** Google's own rendered button (brand rules), dark variant. Calls onCredential with the ID token. */
export default function GoogleSignInButton({ onCredential, onError }) {
  const ref = useRef(null);

  useEffect(() => {
    if (!CLIENT_ID) { onError?.('Google sign-in is not configured.'); return; }
    let cancelled = false;
    loadGsi().then(() => {
      if (cancelled || !ref.current) return;
      window.google.accounts.id.initialize({
        client_id: CLIENT_ID,
        callback: ({ credential }) => onCredential(credential),
      });
      window.google.accounts.id.renderButton(ref.current, {
        theme: 'filled_black', size: 'large', shape: 'pill', text: 'continue_with', width: 320,
      });
    }).catch((e) => onError?.(e.message));
    return () => { cancelled = true; };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return <div ref={ref} className="flex justify-center min-h-11" />;
}
