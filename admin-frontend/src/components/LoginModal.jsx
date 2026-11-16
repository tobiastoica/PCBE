import React, { useState } from 'react';
import { X, Lock, User, AlertCircle } from 'lucide-react';
import api from '../api';

export default function LoginModal({ isOpen, onClose, onLoginSuccess }) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.post('/api/v1/auth/login', { username, password });
      localStorage.setItem('fleet_token', res.data.token);
      localStorage.setItem('fleet_user', JSON.stringify(res.data));
      onLoginSuccess(res.data);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid username or password. (Try admin / admin123)');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white p-1 rounded-lg transition"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="text-center mb-6">
          <div className="w-12 h-12 bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 rounded-xl flex items-center justify-center mx-auto mb-3">
            <Lock className="w-6 h-6" />
          </div>
          <h2 className="text-xl font-bold text-white">Central Server Access</h2>
          <p className="text-xs text-slate-400 mt-1">Authenticate to access administration & dispatch controls</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg flex items-center space-x-2 text-rose-400 text-sm">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
              Username
            </label>
            <div className="relative">
              <User className="w-5 h-5 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-lg text-white text-sm focus:outline-none focus:border-emerald-500 transition"
                placeholder="admin or rider"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1">
              Password
            </label>
            <div className="relative">
              <Lock className="w-5 h-5 absolute left-3 top-2.5 text-slate-400" />
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="w-full pl-10 pr-4 py-2 bg-slate-900/80 border border-slate-700 rounded-lg text-white text-sm focus:outline-none focus:border-emerald-500 transition"
                placeholder="••••••••"
              />
            </div>
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-lg shadow-lg shadow-emerald-600/30 transition disabled:opacity-50 text-sm"
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>
          </div>

          <div className="mt-4 p-3 bg-slate-900/50 rounded-lg border border-slate-700/60 text-xs text-slate-400 space-y-1">
            <div className="font-semibold text-slate-300">Default Demo Credentials:</div>
            <div>• Admin: <span className="text-emerald-400 font-mono">admin</span> / <span className="text-emerald-400 font-mono">admin123</span></div>
            <div>• Rider: <span className="text-emerald-400 font-mono">rider</span> / <span className="text-emerald-400 font-mono">rider123</span></div>
          </div>
        </form>
      </div>
    </div>
  );
}
