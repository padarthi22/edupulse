import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import './index.css';
import { AuthProvider, useAuth } from './lib/auth.jsx';
import Landing from './pages/Landing.jsx';
import Login from './pages/Login.jsx';
import Signup from './pages/Signup.jsx';
import StudentDashboard from './pages/StudentDashboard.jsx';
import TeacherDashboard from './pages/TeacherDashboard.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';
import StudentDetail from './pages/StudentDetail.jsx';
import Navbar from './components/Navbar.jsx';

function Guard({ children, roles }) {
  const { user, ready } = useAuth();
  if (!ready) return <div className="p-10 text-muted">Loading…</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

function roleHome(useAuth) {
  const { user } = useAuth();
  if (!user) return null;
  if (user.role === 'student') return <Navigate to="/student" replace />;
  if (user.role === 'teacher') return <Navigate to="/teacher" replace />;
  if (user.role === 'admin') return <Navigate to="/admin" replace />;
  return null;
}

function Shell() {
  const auth = useAuth();
  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/me" element={<Guard>{roleHome(auth)}</Guard>} />
          <Route path="/student" element={<Guard roles={['student']}><StudentDashboard /></Guard>} />
          <Route path="/teacher" element={<Guard roles={['teacher','admin']}><TeacherDashboard /></Guard>} />
          <Route path="/teacher/student/:id" element={<Guard roles={['teacher','admin']}><StudentDetail /></Guard>} />
          <Route path="/admin" element={<Guard roles={['admin']}><AdminDashboard /></Guard>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider>
      <BrowserRouter>
        <Shell />
      </BrowserRouter>
    </AuthProvider>
  </React.StrictMode>
);
