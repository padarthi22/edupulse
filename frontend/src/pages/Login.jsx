import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth.jsx';

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('teacher@edupulse.dev');
  const [password, setPassword] = useState('password123');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      const u = await login(email, password);
      nav(u.role === 'student' ? '/student' : u.role === 'admin' ? '/admin' : '/teacher');
    } catch (e) {
      setErr(e.response?.data?.error || 'sign_in_failed');
    } finally { setBusy(false); }
  }

  return (
    <div className="max-w-md mx-auto card">
      <h1>Sign in</h1>
      <p className="text-sm text-muted">Use the seeded credentials or your own.</p>
      <form onSubmit={submit} className="mt-4 space-y-3">
        <div><label className="label">Email</label>
          <input className="input" value={email} onChange={(e) => setEmail(e.target.value)} required /></div>
        <div><label className="label">Password</label>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
        <button disabled={busy} className="btn btn-primary w-full justify-center">{busy ? 'Signing in…' : 'Sign in'}</button>
        {err && <p className="text-sm">{err}</p>}
      </form>
      <p className="text-sm text-muted mt-3">No account? <Link to="/signup" className="underline">Create one</Link></p>
    </div>
  );
}
