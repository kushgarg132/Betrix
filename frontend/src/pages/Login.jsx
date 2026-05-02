import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '@/context/AuthContext';
import { useAuth } from '@/hooks/useAuth';
import AuthLayout from '@/components/layout/AuthLayout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { toast } from 'sonner';
import { Eye, EyeOff, Loader2 } from 'lucide-react';

export default function Login() {
  const navigate = useNavigate();
  const { login: ctxLogin } = useContext(AuthContext);
  const { login, guestLogin } = useAuth();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [guestLoading, setGuestLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password) { setError('Enter your username and password.'); return; }
    setError('');
    setLoading(true);
    try {
      const data = await login(username.trim(), password);
      await ctxLogin(data.token);
      toast.success('Welcome back!');
      navigate('/lobby');
    } catch (err) {
      setError(err?.message || 'Invalid credentials. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleGuest = async () => {
    setGuestLoading(true);
    setError('');
    try {
      const data = await guestLogin();
      await ctxLogin(data.token);
      toast.success('Joined as guest!');
      navigate('/lobby');
    } catch (err) {
      setError(err?.message || 'Guest login failed. Please try again.');
    } finally {
      setGuestLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Sign in"
      subtitle="Enter your credentials to access your account."
      altText="New here?"
      altLink="/register"
      altLinkLabel="Create account"
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="username">Username</Label>
          <Input
            id="username"
            type="text"
            placeholder="your_username"
            value={username}
            onChange={e => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="password">Password</Label>
          <div className="relative">
            <Input
              id="password"
              type={showPw ? 'text' : 'password'}
              placeholder="••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete="current-password"
              className="pr-10"
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPw(v => !v)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-text-dim hover:text-text-muted transition-colors"
              aria-label={showPw ? 'Hide password' : 'Show password'}
            >
              {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
        </div>
        {error && <p role="alert" className="text-danger text-sm">{error}</p>}
        <Button type="submit" className="w-full" size="lg" disabled={loading}>
          {loading && <Loader2 size={16} className="animate-spin" />}
          {loading ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>

      <div className="relative my-5">
        <Separator />
        <span className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-background px-3 text-text-dim text-xs uppercase tracking-wider">or</span>
      </div>

      <Button variant="surface" className="w-full" size="lg" onClick={handleGuest} disabled={guestLoading} aria-label="Continue as guest player">
        {guestLoading && <Loader2 size={16} className="animate-spin" />}
        {guestLoading ? 'Joining…' : 'Continue as Guest'}
      </Button>
    </AuthLayout>
  );
}
