import { Link } from 'react-router-dom';

export default function Landing() {
  return (
    <div className="space-y-6">
      <section className="card">
        <h1>EduPulse</h1>
        <p className="mt-2 text-muted max-w-2xl text-sm">
          Decision-support for teachers and students. A small, explainable
          scoring baseline plus Gemini-powered explanations and personalised
          study plans — so you can act earlier, never auto-penalising a student.
        </p>
        <div className="row mt-4">
          <Link to="/signup" className="btn btn-primary">Create an account</Link>
          <Link to="/login" className="btn btn-ghost">Sign in</Link>
        </div>
      </section>

      <section className="grid-cards">
        <div className="card">
          <h2>For students</h2>
          <p className="text-sm text-muted mt-1">Current risk band, predicted grade, study-buddy chat grounded on your own data.</p>
        </div>
        <div className="card">
          <h2>For teachers</h2>
          <p className="text-sm text-muted mt-1">Per-class trends, top contributing factors, what-if simulator, weekly digest.</p>
        </div>
        <div className="card">
          <h2>For admins</h2>
          <p className="text-sm text-muted mt-1">Cohort fairness, model monitoring, AI-service usage.</p>
        </div>
      </section>

      <section className="card">
        <h2>How risk is decided</h2>
        <ol className="list-decimal pl-6 text-sm text-muted space-y-1 mt-2">
          <li>Aggregate per-student signals into a 12-feature vector (attendance, marks, engagement, wellbeing, assignments).</li>
          <li>Score the vector with a small, transparent weighted rule.</li>
          <li>Gemini explains the band and drafts an action plan.</li>
          <li>Mentors log interventions; the system measures if risk improves.</li>
        </ol>
      </section>
    </div>
  );
}
