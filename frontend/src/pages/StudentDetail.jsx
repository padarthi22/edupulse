import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../lib/api.js';

const FIELDS = [
  ['attendance_rate','Attendance rate'],
  ['attendance_trend','Attendance trend'],
  ['late_rate','Late rate'],
  ['assessment_avg_pct','Average assessment %'],
  ['assessment_trend','Assessment trend'],
  ['assignment_on_time_rate','Assignments on time'],
  ['engagement_minutes_per_week','LMS minutes / wk'],
  ['engagement_login_trend','Login trend'],
  ['participation_per_week','Participation / wk'],
  ['wellbeing_mood','Mood'],
  ['wellbeing_stress','Stress'],
  ['wellbeing_sleep','Sleep (h)'],
];

export default function StudentDetail() {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [plan, setPlan] = useState(null);
  const [intv, setIntv] = useState([]);
  const [busy, setBusy] = useState(false);
  const [deltas, setDeltas] = useState({ attendance_rate: 0, assessment_avg_pct: 0 });
  const [whatif, setWhatif] = useState(null);

  async function refreshAll() {
    setBusy(true);
    try {
      const [p, i] = await Promise.all([api.post(`/predict/${id}`), api.get(`/interventions/${id}`)]);
      setData(p.data); setIntv(i.data.interventions);
    } finally { setBusy(false); }
  }
  useEffect(() => { refreshAll(); }, [id]);

  async function makePlan() {
    setBusy(true);
    try { const { data } = await api.post(`/support/${id}`, {}); setPlan(data.plan); }
    finally { setBusy(false); }
  }
  async function addIntervention(type) {
    await api.post(`/interventions/${id}`, { type, notes: '' });
    const i = await api.get(`/interventions/${id}`); setIntv(i.data.interventions);
  }
  async function closeIntervention() {
    await api.post(`/interventions/${id}`, { type: 'mentor-meeting', notes: 'closed', status: 'done' });
    const i = await api.get(`/interventions/${id}`); setIntv(i.data.interventions);
  }
  async function runWhatIf() {
    const cleaned = Object.fromEntries(Object.entries(deltas).filter(([, v]) => Number(v)));
    const { data } = await api.post(`/whatif/${id}`, { deltas: cleaned });
    setWhatif(data);
  }

  return (
    <div className="space-y-6">
      <div className="card flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1>Student detail</h1>
          <p className="text-sm text-muted">Decision-support only. Use mentor judgement to act.</p>
        </div>
        <button onClick={refreshAll} className="btn" disabled={busy}>{busy ? 'Working…' : 'Recompute'}</button>
      </div>

      <section className="grid-cards">
        <div className="card"><p className="label">Risk</p>
          <p className="text-xl font-semibold">{data?.ml?.riskBand} · {Math.round((data?.ml?.riskScore || 0) * 100)}%</p></div>
        <div className="card"><p className="label">Pass probability</p>
          <p className="text-xl font-semibold">{data?.ml ? `${Math.round(data.ml.passProbability * 100)}%` : '—'}</p></div>
        <div className="card"><p className="label">Predicted grade band</p>
          <p className="text-xl font-semibold">{data?.ml?.predictedGradeBand || '—'}</p></div>
      </section>

      <section className="card">
        <h2>Factors feeding in</h2>
        <div className="grid sm:grid-cols-2 gap-2 mt-2">
          {data?.features && Object.entries(data.features).map(([k, v]) => {
            const friendly = (FIELDS.find(([fk]) => fk === k) || [, k])[1];
            return (
              <div key={k} className="row justify-between border border-line rounded-md px-3 py-1.5">
                <span className="text-sm">{friendly}</span>
                <span className="text-sm font-mono">{typeof v === 'number' ? v.toFixed(2) : v}</span>
              </div>
            );
          })}
        </div>
      </section>

      <section className="card">
        <h2>Plain-language explanation</h2>
        <p className="text-sm mt-1">{data?.prediction?.explanation || 'No explanation yet — recompute to populate.'}</p>
      </section>

      <section className="card">
        <div className="row justify-between">
          <h2>Personalised support plan</h2>
          <button onClick={makePlan} className="btn btn-primary" disabled={busy}>Generate</button>
        </div>
        {plan ? (
          <div className="mt-3 grid sm:grid-cols-2 gap-4 text-sm">
            <div>
              <p className="label">Weekly schedule</p>
              <ul className="list-disc pl-5 mt-1 space-y-1">{(plan.weekly_schedule || []).map((x, i) => <li key={i}>{x}</li>)}</ul>
            </div>
            <div>
              <p className="label">Revision priorities</p>
              <ul className="list-disc pl-5 mt-1 space-y-1">{(plan.revision_priorities || []).map((x, i) => <li key={i}>{x}</li>)}</ul>
            </div>
            <div>
              <p className="label">Recommended resources</p>
              <ul className="list-disc pl-5 mt-1 space-y-1">{(plan.recommended_resources || []).map((x, i) => <li key={i}>{x}</li>)}</ul>
            </div>
            <div>
              <p className="label">Mentor talking points</p>
              <ul className="list-disc pl-5 mt-1 space-y-1">{(plan.mentor_talking_points || []).map((x, i) => <li key={i}>{x}</li>)}</ul>
            </div>
            <div className="sm:col-span-2">
              <p className="label">Draft parent message</p>
              <p className="mt-1 whitespace-pre-wrap">{plan.draft_parent_message}</p>
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted mt-2">Click Generate to draft a plan grounded on this student&apos;s signals.</p>
        )}
      </section>

      <section className="card">
        <h2>What-if simulator</h2>
        <p className="text-sm text-muted">Adjust the inputs and see how the predicted risk shifts.</p>
        <div className="grid sm:grid-cols-2 gap-3 mt-3">
          <div>
            <label className="label">Attendance rate (delta, +0.1 = +10%)</label>
            <input className="input" type="number" step="0.01" value={deltas.attendance_rate}
              onChange={(e) => setDeltas({ ...deltas, attendance_rate: Number(e.target.value) })} />
          </div>
          <div>
            <label className="label">Assessment % (delta)</label>
            <input className="input" type="number" step="1" value={deltas.assessment_avg_pct}
              onChange={(e) => setDeltas({ ...deltas, assessment_avg_pct: Number(e.target.value) })} />
          </div>
        </div>
        <div className="row mt-3">
          <button onClick={runWhatIf} className="btn btn-primary">Simulate</button>
        </div>
        {whatif && (
          <div className="mt-4 grid sm:grid-cols-3 gap-3 text-sm">
            <div className="border border-line rounded-md p-3">
              <p className="label">Current</p>
              <p>{whatif.baseline?.riskBand} · {Math.round((whatif.baseline?.riskScore || 0) * 100)}%</p>
            </div>
            <div className="border border-line rounded-md p-3">
              <p className="label">Projected</p>
              <p>{whatif.riskBand} · {Math.round(whatif.riskScore * 100)}%</p>
            </div>
            <div className="border border-line rounded-md p-3">
              <p className="label">Pass prob.</p>
              <p>{Math.round(whatif.passProbability * 100)}%</p>
            </div>
          </div>
        )}
      </section>

      <section className="card">
        <div className="row justify-between">
          <h2>Intervention tracker</h2>
          <div className="row">
            <button onClick={() => addIntervention('mentor-meeting')} className="btn">+ Meeting</button>
            <button onClick={() => addIntervention('study-plan')} className="btn">+ Study plan</button>
            <button onClick={() => addIntervention('parent-call')} className="btn">+ Parent call</button>
          </div>
        </div>
        <table className="table-clean mt-2">
          <thead><tr><th>Type</th><th>Status</th><th>Pre-risk</th><th>Post-risk</th><th>Δ</th><th></th></tr></thead>
          <tbody>
            {intv.map((i) => (
              <tr key={i.id}>
                <td>{i.type}</td>
                <td>{i.status}</td>
                <td>{i.preRiskScore != null ? Math.round(i.preRiskScore * 100) + '%' : '—'}</td>
                <td>{i.postRiskScore != null ? Math.round(i.postRiskScore * 100) + '%' : '—'}</td>
                <td>{i.delta != null ? `${Math.round(i.delta * 100)}%` : '—'}</td>
                <td className="text-right">
                  {i.status !== 'done' && <button onClick={closeIntervention} className="btn btn-ghost">Close</button>}
                </td>
              </tr>
            ))}
            {intv.length === 0 && <tr><td colSpan={6} className="py-2 text-muted">No interventions yet.</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  );
}
