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

export default function Register() {
  const navigate = useNavigate();
  const { login: ctxLogin } = useContext(AuthContext);
  const { register, guestLogin } = useAuth();

  const [form, setForm] = useState({ name: '', username: '', email: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [guestLoading, setGuestLoading] = useState(false);
  const [errors, setErrors] = useState([]);

  const set = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (errors.length) setErrors([]);
  };

  const validate = () => {
    const errs = [];
    if (!form.name.trim())        errs.push('Enter your full name.');
    if (!form.username.trim())    errs.push('Enter a username.');
    else if (form.username.length < 3) errs.push('Username must be at least 3 characters.');
    if (!form.email.trim())       errs.push('Enter your email address.');
    if (!form.password)           errs.push('Enter a password.');
    else if (form.password.length < 6) errs.push('Password must be at least 6 characters.');
    return errs;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (errs.length) { setErrors(errs); return; }
    setLoading(true);
    setErrors([]);
    try {
      await register(form.name.trim(), form.username.trim(), form.password, form.email.trim());
      toast.success('Account created! Please sign in.');
      navigate('/login');
    } catch (err) {
      setErrors([err?.message || 'Registration failed. Try a different username or email.']);
    } finally {
      setLoading(false);
    }
  };

  const handleGuest = async () => {
    setGuestLoading(true);
    try {
      const data = await guestLogin();
      await ctxLogin(data.token);
      toast.success('Joined as guest!');
      navigate('/lobby');
    } catch (err) {
      setError(err?.message || 'Guest login failed.');
    } finally {
      setGuestLoading(false);
    }
  };

  return (
    <AuthLayout
      title="Create account"
      subtitle="Start playing in under a minute."
      altText="Already have an account?"
      altLink="/login"
      altLinkLabel="Sign in"
    >
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="name">Full Name</Label>
            <Input id="name" placeholder="Jane Smith" value={form.name} onChange={set('name')} autoComplete="name" autoFocus />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="username">Username</Label>
            <Input id="username" placeholder="jsmith42" value={form.username} onChange={set('username')} autoComplete="username" />
          </div>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" placeholder="jane@example.com" value={form.email} onChange={set('email')} autoComplete="email" />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="password">Password</Label>
          <div className="relative">
            <Input
              id="password"
              type={showPw ? 'text' : 'password'}
              placeholder="Min. 6 characters"
              value={form.password}
              onChange={set('password')}
              autoComplete="new-password"
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
        {errors.length > 0 && (
          <ul role="alert" className="text-danger text-sm space-y-0.5 list-disc list-inside">
            {errors.map((e, i) => <li key={i}>{e}</li>)}
          </ul>
        )}
        <Button type="submit" className="w-full" size="lg" disabled={loading}>
          {loading && <Loader2 size={16} className="animate-spin" />}
          {loading ? 'Creating account…' : 'Create Account'}
        </Button>
      </form>

      <div className="relative my-5">
        <Separator />
        <span className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-background px-3 text-text-dim text-xs uppercase tracking-wider">or</span>
      </div>

      <Button variant="surface" className="w-full" size="lg" onClick={handleGuest} disabled={guestLoading}>
        {guestLoading && <Loader2 size={16} className="animate-spin" />}
        {guestLoading ? 'Joining…' : 'Continue as Guest'}
      </Button>
    </AuthLayout>
  );
}
