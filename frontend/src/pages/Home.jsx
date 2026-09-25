import React, { useContext, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import GoogleSignInButton from '@/components/auth/GoogleSignInButton';
import { Loader2 } from 'lucide-react';

export default function Home() {
  const { guestLogin, googleLogin } = useAuth();
  const { isLoggedIn, login } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (isLoggedIn) {
      navigate('/lobby', { replace: true });
    }
  }, [isLoggedIn, navigate]);

  if (isLoggedIn) return null;

  const signIn = async (getToken) => {
    setError(null); setBusy(true);
    try {
      const { token } = await getToken();
      await login(token);
      navigate(location.state?.from || '/lobby', { replace: true });
    } catch (e) {
      setError(e.message || 'Sign-in failed. Try again.');
    } finally {
      setBusy(false);
    }
  };
  const handleGuest = () => signIn(guestLogin);
  const handleGoogle = (idToken) => signIn(() => googleLogin(idToken));

  return (
    <div className="min-h-dvh flex flex-col items-center justify-center gap-10 px-4">
      <div className="flex flex-col items-center gap-3 text-center">
        <h1
          className="font-display text-5xl font-bold text-neon-cyan neon-flicker"
          style={{ textShadow: 'var(--glow-md)' }}
        >
          BETRIX
        </h1>
        <p className="text-text-muted">Play-money Texas Hold'em with friends and bots.</p>
      </div>

      <div className="w-full max-w-xs flex flex-col gap-3">
        <GoogleSignInButton onCredential={handleGoogle} onError={setError} />

        <div className="relative my-1">
          <Separator />
          <span className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-background px-3 text-text-dim text-xs uppercase tracking-wider">or</span>
        </div>

        <Button variant="outline" size="lg" onClick={handleGuest} disabled={busy}>
          {busy && <Loader2 size={16} className="animate-spin" />}
          Play as guest
        </Button>

        {error && <p role="alert" className="text-danger text-sm text-center">{error}</p>}
      </div>
    </div>
  );
}
