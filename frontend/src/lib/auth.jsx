import { createContext, useContext, useEffect, useState } from 'react';
import { api } from './api.js';

const Ctx = createContext({ user: null, ready: false, login: () => {}, signup: () => {}, logout: () => {} });

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const raw = localStorage.getItem('edupulse_user');
    if (raw) setUser(JSON.parse(raw));
    setReady(true);
  }, []);

  async function login(email, password) {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('edupulse_token', data.token);
    localStorage.setItem('edupulse_user', JSON.stringify(data.user));
    setUser(data.user);
    return data.user;
  }
  async function signup(payload) {
    const { data } = await api.post('/auth/signup', payload);
    localStorage.setItem('edupulse_token', data.token);
    localStorage.setItem('edupulse_user', JSON.stringify(data.user));
    setUser(data.user);
    return data.user;
  }
  function logout() {
    localStorage.removeItem('edupulse_token');
    localStorage.removeItem('edupulse_user');
    setUser(null);
  }
  return <Ctx.Provider value={{ user, ready, login, signup, logout }}>{children}</Ctx.Provider>;
}

export function useAuth() { return useContext(Ctx); }
