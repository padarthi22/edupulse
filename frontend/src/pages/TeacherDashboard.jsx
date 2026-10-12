import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api.js';

const Pill = ({ children, muted }) => <span className={`pill ${muted ? 'pill-muted' : ''}`}>{children}</span>;

export default function TeacherDashboard() {
  const [classId, setClassId] = useState('10-A');
  const [classes, setClasses] = useState([]);
  const [data, setData] = useState(null);
  const [digest, setDigest] = useState(null);
  const [ingest, setIngest] = useState({ kind: 'attendance', file: null, status: null });

  useEffect(() => {
    api.get('/students').then((r) => setClasses([...new Set(r.data.map((s) => s.classId))].sort()));
  }, []);

  useEffect(() => {
    if (!classId) return;
    api.get(`/class/${classId}`).then((r) => setData(r.data));
    api.get(`/digest/${classId}`).then((r) => setDigest(r.data));
  }, [classId]);

  async function upload() {
    if (!ingest.file) return;
    const fd = new FormData();
    fd.append('file', ingest.file);
    setIngest({ ...ingest, status: 'uploading' });
    try {
      const { data } = await api.post(`/ingest/${ingest.kind}/csv`, fd, { headers: { 'Content-Type': 'multipart/form-data' } });
      setIngest({ ...ingest, status: `inserted ${data.inserted}, errors ${data.errors.length}` });
    } catch (e) {
      setIngest({ ...ingest, status: 'upload failed: ' + (e.response?.data?.error || e.message) });
    }
  }

  return (
    <div className="space-y-6">
      <div className="card flex flex-wrap items-center gap-3">
        <div>
          <h1>Teacher dashboard</h1>
          <p className="text-sm text-muted">Pick a class, see top risks, and ingest signals.</p>
        </div>
        <div className="ml-auto">
          <select className="input w-40" value={classId} onChange={(e) => setClassId(e.target.value)}>
            {classes.map((c) => <option key={c}>{c}</option>)}
          </select>
        </div>
      </div>

      <section className="grid-cards">
        <div className="card"><p className="label">Students</p><p className="text-xl font-semibold mt-1">{data?.counts?.students ?? '—'}</p></div>
        <div className="card"><p className="label">With prediction</p><p className="text-xl font-semibold mt-1">{data?.counts?.withPrediction ?? '—'}</p></div>
        <div className="card"><p className="label">High-risk</p><p className="text-xl font-semibold mt-1">{data?.bands?.High ?? 0}</p></div>
      </section>

      {digest && (
        <section className="card">
          <h2>Weekly digest</h2>
          <p className="text-sm mt-1">{digest.digest.headline}</p>
          <ul className="mt-3 list-disc pl-5 text-sm space-y-1">
            {(digest.digest.what_changed || []).map((x, i) => <li key={i}>{x}</li>)}
          </ul>
          <ul className="mt-2 list-disc pl-5 text-sm space-y-1">
            {(digest.digest.priorities || []).map((x, i) => <li key={i}>{x}</li>)}
          </ul>
        </section>
      )}

      <section className="card">
        <h2>Students</h2>
        <table className="table-clean mt-2">
          <thead><tr><th>Code</th><th>Name</th><th>Risk</th><th>Predicted</th><th></th></tr></thead>
          <tbody>
            {data?.rows?.map((s) => (
              <tr key={s.studentId}>
                <td>{s.studentCode}</td>
                <td>{s.name}</td>
                <td><Pill muted={s.riskBand === '—'}>{s.riskBand}</Pill></td>
                <td>{s.predictedGradeBand || '—'}</td>
                <td className="text-right"><Link to={`/teacher/student/${s.studentId}`} className="btn btn-ghost">Open</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="card">
        <h2>Ingest a CSV</h2>
        <p className="text-sm text-muted">Drop a CSV with header row matching the schema in the README.</p>
        <div className="row mt-3 flex-wrap">
          <select className="input w-44" value={ingest.kind} onChange={(e) => setIngest({ ...ingest, kind: e.target.value })}>
            <option value="attendance">Attendance</option>
            <option value="assessments">Assessments</option>
            <option value="assignments">Assignments</option>
            <option value="engagement">Engagement</option>
            <option value="wellbeing">Wellbeing</option>
          </select>
          <input type="file" accept=".csv" onChange={(e) => setIngest({ ...ingest, file: e.target.files?.[0] })} />
          <button onClick={upload} className="btn btn-primary" disabled={!ingest.file}>Upload</button>
        </div>
        {ingest.status && <p className="text-sm mt-2">{ingest.status}</p>}
      </section>
    </div>
  );
}
