import { useEffect, useState } from 'react';
import { api } from '../lib/api.js';

export default function AdminDashboard() {
  const [fairness, setFairness] = useState(null);
  const [metrics, setMetrics] = useState(null);

  useEffect(() => {
    api.get('/admin/fairness').then((r) => setFairness(r.data.fairness));
    api.get('/admin/metrics').then((r) => setMetrics(r.data));
  }, []);

  return (
    <div className="space-y-6">
      <div className="card"><h1>Admin analytics</h1></div>

      <section className="card">
        <h2>Cohort fairness</h2>
        {!fairness && <p className="text-sm text-muted">Loading…</p>}
        {fairness && Object.entries(fairness).map(([key, groups]) => (
          <div key={key} className="mt-4">
            <p className="label">{key}</p>
            <table className="table-clean">
              <thead><tr><th>Group</th><th>n</th><th>Avg risk</th><th>High rate</th></tr></thead>
              <tbody>
                {groups.map((g) => (
                  <tr key={g.group}>
                    <td>{g.group}</td>
                    <td>{g.n}</td>
                    <td>{g.avgRisk != null ? `${Math.round(g.avgRisk * 100)}%` : '—'}</td>
                    <td>{Math.round(g.highRate * 100)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        <p className="text-xs text-muted mt-4">Look for large High-rate differences across groups — that&apos;s the bias signal.</p>
      </section>

      <section className="card">
        <h2>Model + AI</h2>
        {!metrics && <p className="text-sm text-muted">Loading…</p>}
        {metrics && (
          <div className="grid-cards mt-2 text-sm">
            <div><p className="label">Trained rows</p><p className="text-xl font-semibold">{metrics.trainedRows ?? '—'}</p></div>
            <div><p className="label">Accuracy</p><p className="text-xl font-semibold">{metrics.metrics?.accuracy ?? '—'}</p></div>
            <div><p className="label">Recall (at-risk)</p><p className="text-xl font-semibold">{metrics.metrics?.recallAtRisk ?? '—'}</p></div>
            <div><p className="label">Model version</p><p>{metrics.modelVersion}</p></div>
            <div><p className="label">AI mode</p><p>{metrics.ai?.demo ? 'Demo (no key)' : 'Live Gemini'}</p></div>
            <div><p className="label">Tokens today</p><p>{metrics.ai?.tokens ?? 0}</p></div>
          </div>
        )}
      </section>
    </div>
  );
}
