import { useEffect, useState } from 'react';
import { useAuth } from '../lib/auth.jsx';
import { api } from '../lib/api.js';

const Pill = ({ children, muted }) => <span className={`pill ${muted ? 'pill-muted' : ''}`}>{children}</span>;

const HistoryChart = ({ rows }) => {
  if (!rows || rows.length === 0) return <p className="text-sm text-muted">No predictions yet.</p>;
  const W = 320, H = 80;
  const ys = rows.map((r) => 1 - (r.riskScore ?? 0));
  const max = rows.length - 1 || 1;
  const sx = (i) => (W * i) / max;
  const sy = (y) => H * y;
  const d = rows.map((_, i) => `${i ? 'L' : 'M'}${sx(i).toFixed(1)},${sy(ys[i]).toFixed(1)}`).join(' ');
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-24 text-ink">
      <path d={d} fill="none" stroke="currentColor" strokeWidth="2" />
    </svg>
  );
};

export default function StudentDashboard() {
  const { user } = useAuth();
  const [latest, setLatest] = useState(null);
  const [history, setHistory] = useState([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!user) return;
    api.get(`/latest/${user.studentProfile}`).then((r) => {
      setLatest(r.data.latest);
      setHistory(r.data.history || []);
    });
  }, [user]);

  async function rerun() {
    if (!user) return;
    setBusy(true);
    try {
      await api.post(`/predict/${user.studentProfile}`);
      const r = await api.get(`/latest/${user.studentProfile}`);
      setLatest(r.data.latest); setHistory(r.data.history || []);
    } finally { setBusy(false); }
  }

  return (
    <div className="space-y-6">
      <div className="card flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h1>Welcome, {user?.name}</h1>
          <p className="text-sm text-muted">These signals are decision-support, not a label. Talk to your mentor if anything feels off.</p>
        </div>
        <button onClick={rerun} className="btn" disabled={busy}>{busy ? 'Recomputing…' : 'Recompute my check-in'}</button>
      </div>

      <section className="grid-cards">
        <div className="card">
          <p className="label">Current risk band</p>
          <div className="mt-1"><Pill muted={!latest}>{latest?.riskBand || '—'}</Pill></div>
          {latest?.riskScore != null && <p className="text-sm text-muted mt-1">Score {Math.round(latest.riskScore * 100)}%</p>}
        </div>
        <div className="card">
          <p className="label">Predicted grade band</p>
          <p className="text-xl font-semibold mt-1">{latest?.predictedGradeBand || '—'}</p>
          <p className="text-sm text-muted">Confidence: {latest?.confidence || '—'}</p>
        </div>
        <div className="card">
          <p className="label">Pass probability</p>
          <p className="text-xl font-semibold mt-1">{latest ? `${Math.round(latest.passProbability * 100)}%` : '—'}</p>
          <p className="text-sm text-muted">{latest?.computedAt ? new Date(latest.computedAt).toLocaleString() : 'never'}</p>
        </div>
      </section>

      <section className="card">
        <h2>Recent trend</h2>
        <HistoryChart rows={history} />
        <p className="text-xs text-muted mt-1">Higher line is better. Each point is one weekly check-in.</p>
      </section>

      {latest?.explanation && (
        <section className="card">
          <h2>Plain-language snapshot</h2>
          <p className="text-sm mt-1">{latest.explanation}</p>
        </section>
      )}

      <section className="card">
        <h2>Study buddy</h2>
        <p className="text-sm text-muted">Ask about a topic, plan your week, or talk through a stuck problem.</p>
        <ChatBox studentId={user?.studentProfile} />
      </section>
    </div>
  );
}

function ChatBox({ studentId }) {
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!studentId) return;
    api.get(`/chat/${studentId}/history`).then((r) => setMessages(r.data.turns || []));
  }, [studentId]);

  async function send() {
    if (!text.trim()) return;
    const next = [...messages, { role: 'user', content: text, at: new Date() }];
    setMessages(next); setText(''); setBusy(true);
    try {
      const { data } = await api.post(`/chat/${studentId}`, { message: text });
      setMessages([...next, { role: 'assistant', content: data.reply, at: new Date() }]);
    } finally { setBusy(false); }
  }

  return (
    <div className="space-y-2 mt-3">
      <div className="border border-line rounded-md p-3 h-64 overflow-auto bg-paper">
        {messages.length === 0 && <p className="text-sm text-muted">No messages yet. Say hello.</p>}
        {messages.map((m, i) => (
          <div key={i} className={`mb-2 ${m.role === 'user' ? 'text-right' : ''}`}>
            <div className={`inline-block rounded-md px-3 py-2 text-sm ${m.role === 'user' ? 'bg-ink text-white' : 'bg-white border border-line'}`}>
              {m.content}
            </div>
          </div>
        ))}
      </div>
      <div className="row">
        <input className="input" value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && send()} placeholder="Ask your study buddy…" />
        <button onClick={send} disabled={busy || !text.trim()} className="btn btn-primary">Send</button>
      </div>
    </div>
  );
}
