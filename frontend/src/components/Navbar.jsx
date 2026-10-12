import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const home = user?.role === 'student' ? '/student'
    : user?.role === 'admin' ? '/admin'
    : user?.role === 'teacher' ? '/teacher' : '/';
  return (
    <nav className="border-b border-line bg-white">
      <div className="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
        <Link to={home} className="font-semibold tracking-tight text-ink">EduPulse</Link>
        <div className="row text-sm">
          {user ? (
            <>
              <span className="text-muted">{user.name} · {user.role}</span>
              <Link to="/" className="btn btn-ghost">Home</Link>
              <button className="btn" onClick={() => { logout(); nav('/login'); }}>Sign out</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost">Sign in</Link>
              <Link to="/signup" className="btn btn-primary">Create account</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
