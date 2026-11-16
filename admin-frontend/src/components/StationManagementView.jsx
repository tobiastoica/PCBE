import React, { useEffect, useState } from 'react';
import { Server, Plus, ArrowRightLeft, Shield, CheckCircle2, AlertCircle } from 'lucide-react';
import api from '../api';

export default function StationManagementView() {
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);

  // Spawn Modal State
  const [isSpawnOpen, setIsSpawnOpen] = useState(false);
  const [spawnForm, setSpawnForm] = useState({
    stationId: '',
    name: '',
    lat: 45.755,
    lon: 21.23,
    capacity: 20,
    port: 8086,
  });
  const [spawnLoading, setSpawnLoading] = useState(false);
  const [spawnResult, setSpawnResult] = useState(null);

  // Transfer Modal State
  const [isTransferOpen, setIsTransferOpen] = useState(false);
  const [transferForm, setTransferForm] = useState({
    scooterId: 'sc_101',
    fromStationId: 'station_a',
    toStationId: 'station_b',
    reason: 'Prevent station saturation',
  });
  const [transferLoading, setTransferLoading] = useState(false);
  const [transferMsg, setTransferMsg] = useState('');

  useEffect(() => {
    fetchStations();
  }, []);

  const fetchStations = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/v1/admin/fleet-overview', {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      if (res.data && res.data.stations) {
        setStations(res.data.stations);
      }
    } catch (err) {
      console.error('Failed to load stations', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSpawnSubmit = async (e) => {
    e.preventDefault();
    setSpawnLoading(true);
    setSpawnResult(null);
    try {
      const res = await api.post('/api/v1/admin/stations/spawn', spawnForm, {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setSpawnResult({ success: true, data: res.data });
      fetchStations();
      setTimeout(() => {
        setIsSpawnOpen(false);
        setSpawnResult(null);
      }, 2000);
    } catch (err) {
      setSpawnResult({
        success: false,
        message: err.response?.data?.message || err.message,
      });
    } finally {
      setSpawnLoading(false);
    }
  };

  const handleTransferSubmit = async (e) => {
    e.preventDefault();
    setTransferLoading(true);
    setTransferMsg('');
    try {
      await api.post('/api/v1/admin/scooters/transfer', transferForm, {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setTransferMsg('Transfer command dispatched to Kafka successfully!');
      fetchStations();
      setTimeout(() => {
        setIsTransferOpen(false);
        setTransferMsg('');
      }, 1500);
    } catch (err) {
      setTransferMsg('Failed to transfer: ' + (err.response?.data?.message || err.message));
    } finally {
      setTransferLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-800/80 p-6 rounded-2xl border border-slate-700 shadow-xl">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-2 rounded-xl bg-blue-500/20 text-blue-400 font-bold">
              <Server className="w-5 h-5" />
            </span>
            <h2 className="text-xl font-bold text-white">Charging Stations & Infrastructure</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Dynamic Edge Node Orchestration: The Central Server interfaces with the host Docker socket to provision on-demand station containers.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={() => setIsTransferOpen(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold rounded-xl transition"
          >
            <ArrowRightLeft className="w-4 h-4 text-amber-400" />
            <span>Relocate Scooter</span>
          </button>

          <button
            onClick={() => setIsSpawnOpen(true)}
            className="flex items-center space-x-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl shadow-lg shadow-emerald-600/30 transition"
          >
            <Plus className="w-4 h-4" />
            <span>Spawn New Station</span>
          </button>
        </div>
      </div>

      {/* Stations Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        {stations.map((st) => {
          const occupancy = Math.round((st.dockedCount / st.capacity) * 100);
          return (
            <div
              key={st.stationId}
              className="bg-slate-800/90 border border-slate-700/80 rounded-2xl p-5 shadow-lg hover:border-slate-600 transition flex flex-col justify-between"
            >
              <div>
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center space-x-2">
                    <div className="w-9 h-9 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center text-blue-400 text-lg">
                      ⚡
                    </div>
                    <div>
                      <h3 className="font-bold text-sm text-white">{st.name}</h3>
                      <span className="text-[10px] text-slate-400 font-mono">{st.stationId}</span>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-mono text-[9px] font-bold border border-emerald-500/30">
                    {st.status}
                  </span>
                </div>

                <div className="space-y-3 my-4">
                  {/* Occupancy bar */}
                  <div>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-slate-400">Docked Capacity:</span>
                      <span className="font-mono font-bold text-white">
                        {st.dockedCount} / {st.capacity}
                      </span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-2 overflow-hidden border border-slate-700">
                      <div
                        className={`h-full rounded-full transition-all ${
                          occupancy > 80 ? 'bg-amber-500' : 'bg-blue-500'
                        }`}
                        style={{ width: `${Math.min(occupancy, 100)}%` }}
                      ></div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[11px] pt-1 font-mono">
                    <div className="bg-slate-900/60 p-2 rounded-lg border border-slate-700/50">
                      <span className="text-slate-400 text-[10px] block">Port:</span>
                      <span className="text-white font-semibold">:{st.port}</span>
                    </div>
                    <div className="bg-slate-900/60 p-2 rounded-lg border border-slate-700/50">
                      <span className="text-slate-400 text-[10px] block">Coordinates:</span>
                      <span className="text-slate-300 truncate block">
                        {st.lat?.toFixed(3)}, {st.lon?.toFixed(3)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="pt-2 border-t border-slate-700/50 text-[10px] font-mono text-slate-400 flex items-center justify-between">
                <span>Container:</span>
                <span className="text-slate-300 truncate max-w-[120px]">{st.dockerContainerId || 'running'}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Spawn Station Modal */}
      {isSpawnOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
            <h3 className="font-bold text-base text-white mb-1">Spawn Charging Station</h3>
            <p className="text-xs text-slate-400 mb-4">
              Instantiates a new container via the Docker daemon socket and records the station into PostgreSQL SSoT.
            </p>

            {spawnResult && (
              <div
                className={`mb-4 p-3 rounded-lg flex items-center space-x-2 text-xs ${
                  spawnResult.success
                    ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400'
                    : 'bg-rose-500/10 border border-rose-500/30 text-rose-400'
                }`}
              >
                {spawnResult.success ? <CheckCircle2 className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
                <span>{spawnResult.success ? 'Station container spawned successfully!' : spawnResult.message}</span>
              </div>
            )}

            <form onSubmit={handleSpawnSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-300 mb-1">Station ID (e.g. station_e):</label>
                <input
                  type="text"
                  required
                  value={spawnForm.stationId}
                  onChange={(e) => setSpawnForm({ ...spawnForm, stationId: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  placeholder="station_e"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1">Station Name:</label>
                <input
                  type="text"
                  required
                  value={spawnForm.name}
                  onChange={(e) => setSpawnForm({ ...spawnForm, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                  placeholder="Iulius Town Station"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">Latitude:</label>
                  <input
                    type="number"
                    step="0.0001"
                    required
                    value={spawnForm.lat}
                    onChange={(e) => setSpawnForm({ ...spawnForm, lat: parseFloat(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">Longitude:</label>
                  <input
                    type="number"
                    step="0.0001"
                    required
                    value={spawnForm.lon}
                    onChange={(e) => setSpawnForm({ ...spawnForm, lon: parseFloat(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">Max Capacity:</label>
                  <input
                    type="number"
                    required
                    value={spawnForm.capacity}
                    onChange={(e) => setSpawnForm({ ...spawnForm, capacity: parseInt(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">Container Port:</label>
                  <input
                    type="number"
                    required
                    value={spawnForm.port}
                    onChange={(e) => setSpawnForm({ ...spawnForm, port: parseInt(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                </div>
              </div>

              <div className="flex space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsSpawnOpen(false)}
                  className="flex-1 py-2 bg-slate-700 text-white rounded-lg hover:bg-slate-600 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={spawnLoading}
                  className="flex-1 py-2 bg-emerald-600 text-white font-semibold rounded-lg hover:bg-emerald-500 transition disabled:opacity-50"
                >
                  {spawnLoading ? 'Spawning...' : 'Deploy Container'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Relocate Scooter Modal */}
      {isTransferOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-md p-6 shadow-2xl relative">
            <h3 className="font-bold text-base text-white mb-1">Relocate Scooter</h3>
            <p className="text-xs text-slate-400 mb-4">
              Dispatches an administrative transfer command via Kafka topic <span className="font-mono text-emerald-400">admin-transfer-commands</span>.
            </p>

            {transferMsg && (
              <div className="mb-4 p-3 rounded-lg bg-blue-500/10 border border-blue-500/30 text-blue-400 text-xs">
                {transferMsg}
              </div>
            )}

            <form onSubmit={handleTransferSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-300 mb-1">Scooter ID:</label>
                <input
                  type="text"
                  required
                  value={transferForm.scooterId}
                  onChange={(e) => setTransferForm({ ...transferForm, scooterId: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  placeholder="sc_101"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">From Station:</label>
                  <input
                    type="text"
                    required
                    value={transferForm.fromStationId}
                    onChange={(e) => setTransferForm({ ...transferForm, fromStationId: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">To Station:</label>
                  <input
                    type="text"
                    required
                    value={transferForm.toStationId}
                    onChange={(e) => setTransferForm({ ...transferForm, toStationId: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-300 mb-1">Reason / Notes:</label>
                <input
                  type="text"
                  value={transferForm.reason}
                  onChange={(e) => setTransferForm({ ...transferForm, reason: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>

              <div className="flex space-x-3 pt-3">
                <button
                  type="button"
                  onClick={() => setIsTransferOpen(false)}
                  className="flex-1 py-2 bg-slate-700 text-white rounded-lg hover:bg-slate-600 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={transferLoading}
                  className="flex-1 py-2 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-500 transition disabled:opacity-50"
                >
                  {transferLoading ? 'Dispatching...' : 'Dispatch Transfer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
