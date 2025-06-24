import React, { useState } from 'react';
import './App.css';

const initialForm = {
  firstname: '',
  lastname: '',
  email: '',
  password: '',
};

const API_BASE = process.env.REACT_APP_API_BASE || 'http://localhost:8080';

function App() {
  const [isLogin, setIsLogin] = useState(true);
  const [form, setForm] = useState(initialForm);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      const endpoint = isLogin ? `${API_BASE}/api/v1/auth/authenticate` : `${API_BASE}/api/v1/auth/register`;
      const body = isLogin
        ? { email: form.email, password: form.password }
        : form;
      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (res.ok) {
        setMessage(isLogin ? 'Login successful!' : 'User created! You can now log in.');
        if (isLogin && data.token) {
          localStorage.setItem('jwt_token', data.token);
        }
      } else {
        setMessage(data.message || 'Something went wrong.');
      }
    } catch (err) {
      setMessage('Network error.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="memshare-bg">
      <div className="welcome-banner">
        Welcome to <span>MemShare</span>
      </div>
      <div className="center-outer">
        <div className="auth-container">
          <div className="auth-card">
            <h2>{isLogin ? 'Login' : 'Register'}</h2>
            <form onSubmit={handleSubmit}>
              {!isLogin && (
                <>
                  <input
                    name="firstname"
                    placeholder="First Name"
                    value={form.firstname}
                    onChange={handleChange}
                    required
                  />
                  <input
                    name="lastname"
                    placeholder="Last Name"
                    value={form.lastname}
                    onChange={handleChange}
                    required
                  />
                </>
              )}
              <input
                name="email"
                type="email"
                placeholder="Email"
                value={form.email}
                onChange={handleChange}
                required
              />
              <input
                name="password"
                type="password"
                placeholder="Password"
                value={form.password}
                onChange={handleChange}
                required
              />
              <button type="submit" disabled={loading}>
                {loading ? 'Please wait...' : isLogin ? 'Login' : 'Register'}
              </button>
            </form>
            <div className="toggle-link">
              {isLogin ? (
                <>
                  Don&apos;t have an account?{' '}
                  <button onClick={() => { setIsLogin(false); setMessage(''); }}>
                    Register
                  </button>
                </>
              ) : (
                <>
                  Already have an account?{' '}
                  <button onClick={() => { setIsLogin(true); setMessage(''); }}>
                    Login
                  </button>
                </>
              )}
            </div>
            {message && <div className="message">{message}</div>}
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
