import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import FleetMapView from './components/FleetMapView';
import ActiveFleetView from './components/ActiveFleetView';
import StationManagementView from './components/StationManagementView';
import RebalanceView from './components/RebalanceView';
import RideHistoryView from './components/RideHistoryView';
import LoginModal from './components/LoginModal';

export default function App() {
  const [activeTab, setActiveTab] = useState('map');
  const [user, setUser] = useState(null);
  const [isLoginOpen, setIsLoginOpen] = useState(false);

  useEffect(() => {
    const savedUser = localStorage.getItem('fleet_user');
    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('fleet_user');
      }
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('fleet_token');
    localStorage.removeItem('fleet_user');
    setUser(null);
  };

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        user={user}
        onLoginClick={() => setIsLoginOpen(true)}
        onLogout={handleLogout}
      />

      <main className="flex-1">
        {activeTab === 'map' && <FleetMapView />}
        {activeTab === 'active' && <ActiveFleetView user={user} />}
        {activeTab === 'stations' && <StationManagementView />}
        {activeTab === 'rebalance' && <RebalanceView />}
        {activeTab === 'history' && <RideHistoryView />}
      </main>

      <LoginModal
        isOpen={isLoginOpen}
        onClose={() => setIsLoginOpen(false)}
        onLoginSuccess={(userData) => setUser(userData)}
      />
    </div>
  );
}
