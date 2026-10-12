import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth.jsx';

export default function Signup() {
  const { signup } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState({
    email:'', name:'', password:'', role:'teacher',
    classId:'', studentCode:'',
    aiConsent:true, wellbeingConsent:true,
  });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const toggle = (k) => () => setForm((f) => ({ ...f, [k]: !f[k] }));

  async function submit(e) {
    e.preventDefault();
    setBusy(true); setErr(null);
    try {
      const u = await signup({
        email: form.email, name: form.name, password: form.password, role: form.role,
        classId: form.classId || undefined, studentCode: form.studentCode || undefined,
        consent: new Set([
          ...(form.aiConsent ? ['ai_features'] : []),
          ...(form.wellbeingConsent ? ['wellbeing'] : []),
        ]),
      });
      nav(u.role === 'student' ? '/student' : u.role === 'admin' ? '/admin' : '/teacher');
    } catch (e) {
      setErr(e.response?.data?.error || 'sign_up_failed');
    } finally { setBusy(false); }
  }

  return (
    <div className="max-w-xl mx-auto card">
      <h1>Create an account</h1>
      <p className="text-sm text-muted">Consent is required for AI features and wellbeing storage.</p>
      <form onSubmit={submit} className="mt-4 space-y-3">
        <div className="grid sm:grid-cols-2 gap-3">
          <div><label className="label">Email</label><input className="input" value={form.email} onChange={set('email')} required /></div>
          <div><label className="label">Full name</label><input className="input" value={form.name} onChange={set('name')} required /></div>
          <div><label className="label">Password</label><input className="input" type="password" value={form.password} onChange={set('password')} required /></div>
          <div>
            <label className="label">Role</label>
            <select className="input" value={form.role} onChange={set('role')}>
              <option value="student">Student</option>
              <option value="teacher">Teacher / mentor</option>
              <option value="admin">Admin / counselor</option>
            </select>
          </div>
          {form.role === 'student' && (
            <>
              <div><label className="label">Class</label><input className="input" value={form.classId} onChange={set('classId')} /></div>
              <div><label className="label">Student code</label><input className="input" value={form.studentCode} onChange={set('studentCode')} /></div>
            </>
          )}
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.aiConsent} onChange={toggle('aiConsent')} />
          Consent to AI features (explanations and support plans) — pseudonymised data only.
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={form.wellbeingConsent} onChange={toggle('wellbeingConsent')} />
          Consent to store wellbeing check-ins (encrypted at rest).
        </label>
        <button disabled={busy} className="btn btn-primary w-full justify-center">{busy ? 'Creating…' : 'Create account'}</button>
        {err && <p className="text-sm">{err}</p>}
      </form>
      <p className="text-sm text-muted mt-3">Already have one? <Link to="/login" className="underline">Sign in</Link></p>
    </div>
  );
}
