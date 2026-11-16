import React from 'react';
import { MapPin, Navigation, Server, Bot, History, LogIn, LogOut, ShieldCheck } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, user, onLoginClick, onLogout }) {
  const tabs = [
    { id: 'map', label: 'Fleet Map', icon: MapPin },
    { id: 'active', label: 'Active Fleet', icon: Navigation },
    { id: 'stations', label: 'Stations & Infra', icon: Server },
    { id: 'rebalance', label: 'AI Rebalance', icon: Bot },
    { id: 'history', label: 'Ride History', icon: History },
  ];

  return (
    <header className="bg-slate-800/90 backdrop-blur border-b border-slate-700 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between h-16">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 font-bold text-xl">
            ⚡
          </div>
          <div>
            <h1 className="font-bold text-lg text-white leading-tight">Urban Mobility Central</h1>
            <span className="text-xs text-slate-400 font-mono">SSoT & Fleet Backbone</span>
          </div>
        </div>

        {/* Tab Navigation */}
        <nav className="flex space-x-1 sm:space-x-2">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/20'
                    : 'text-slate-300 hover:text-white hover:bg-slate-700/60'
                }`}
              >
                <Icon className="w-4 h-4" />
                <span className="hidden md:inline">{tab.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Auth status */}
        <div className="flex items-center space-x-3">
          {user ? (
            <div className="flex items-center space-x-3">
              <div className="text-right hidden sm:block">
                <div className="text-sm font-semibold text-white flex items-center justify-end space-x-1">
                  <span>{user.fullName || user.username}</span>
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                </div>
                <div className="text-xs text-emerald-400 font-mono">{user.role}</div>
              </div>
              <button
                onClick={onLogout}
                className="p-2 rounded-lg bg-slate-700/60 hover:bg-rose-500/20 hover:text-rose-400 text-slate-300 border border-slate-600 transition"
                title="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          ) : (
            <button
              onClick={onLoginClick}
              className="flex items-center space-x-2 px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-sm transition"
            >
              <LogIn className="w-4 h-4" />
              <span>Login</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
