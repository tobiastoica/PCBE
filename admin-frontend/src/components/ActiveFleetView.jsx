import React, { useEffect, useState } from 'react';
import { Navigation, Battery, Radio, Gauge, MapPin, RefreshCw, AlertCircle } from 'lucide-react';
import api from '../api';

export default function ActiveFleetView({ user }) {
  const [activeScooters, setActiveScooters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [inspectModal, setInspectModal] = useState(null);
  const [inspectLoading, setInspectLoading] = useState(false);

  useEffect(() => {
    fetchActive();
    const interval = setInterval(fetchActive, 10000);
    return () => clearInterval(interval);
  }, []);

  const fetchActive = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/v1/admin/scooters/active', {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setActiveScooters(res.data || []);
    } catch (err) {
      console.error('Failed to fetch active fleet', err);
    } finally {
      setLoading(false);
    }
  };

  const handleInspectLive = async (scooterId) => {
    setInspectLoading(true);
    try {
      const res = await api.get(`/api/v1/admin/scooters/${scooterId}/live-location`, {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setInspectModal(res.data);
    } catch (err) {
      alert('Could not inspect live telemetry: ' + (err.response?.data?.message || err.message));
    } finally {
      setInspectLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-800/80 p-6 rounded-2xl border border-slate-700 shadow-xl">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-2 rounded-xl bg-amber-500/20 text-amber-400 font-bold">
              <Navigation className="w-5 h-5 animate-pulse" />
            </span>
            <h2 className="text-xl font-bold text-white">Active Moving Fleet</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Separation of concerns: High-frequency 3s GPS telemetry is processed by Edge Stations. On-demand live inspections query the managing edge node directly.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <div className="px-4 py-2 rounded-xl bg-slate-900/90 border border-slate-700 font-mono text-xs">
            <span className="text-slate-400">Active Sessions: </span>
            <span className="text-amber-400 font-bold text-sm">{activeScooters.length}</span>
          </div>

          <button
            onClick={fetchActive}
            disabled={loading}
            className="flex items-center space-x-2 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold rounded-xl transition"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Active Scooters Table */}
      {activeScooters.length === 0 ? (
        <div className="bg-slate-800/50 border border-slate-700/80 rounded-2xl p-12 text-center text-slate-400">
          <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-slate-700/50 flex items-center justify-center text-slate-300">
            🛴
          </div>
          <h3 className="font-semibold text-white">No Scooters Currently in Motion</h3>
          <p className="text-xs mt-1">All fleet units are stationary and docked at charging stations.</p>
        </div>
      ) : (
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl shadow-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-slate-400 uppercase font-mono tracking-wider border-b border-slate-700">
                <tr>
                  <th className="py-3.5 px-4">Scooter</th>
                  <th className="py-3.5 px-4">Status</th>
                  <th className="py-3.5 px-4">Battery</th>
                  <th className="py-3.5 px-4">Rider / Session</th>
                  <th className="py-3.5 px-4">Managing Station</th>
                  <th className="py-3.5 px-4">Last Position</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/60 font-sans">
                {activeScooters.map((sc) => (
                  <tr key={sc.scooterId} className="hover:bg-slate-700/30 transition">
                    <td className="py-3.5 px-4 font-semibold text-white flex items-center space-x-2">
                      <span className="text-base">🛴</span>
                      <div>
                        <div>{sc.model}</div>
                        <div className="font-mono text-[10px] text-slate-400">{sc.scooterId}</div>
                      </div>
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="px-2.5 py-1 rounded-full bg-amber-500/20 text-amber-300 font-mono text-[10px] font-bold border border-amber-500/30 flex items-center space-x-1 w-max">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-ping"></span>
                        <span>{sc.status}</span>
                      </span>
                    </td>
                    <td className="py-3.5 px-4 font-mono font-semibold text-slate-200">
                      <div className="flex items-center space-x-1">
                        <Battery className="w-4 h-4 text-emerald-400" />
                        <span>{sc.batteryLevel}%</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">
                      <div>{sc.userId}</div>
                      <div className="text-[10px] text-slate-400">{sc.activeRideId}</div>
                    </td>
                    <td className="py-3.5 px-4 font-medium text-slate-300">
                      <div>{sc.managingStationId}</div>
                      <div className="font-mono text-[10px] text-slate-500">{sc.managingStationUrl}</div>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-400 text-[11px]">
                      {sc.currentLat?.toFixed(4)}, {sc.currentLon?.toFixed(4)}
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => handleInspectLive(sc.scooterId)}
                        disabled={inspectLoading}
                        className="px-3 py-1.5 bg-emerald-600/90 hover:bg-emerald-500 text-white rounded-lg text-xs font-medium shadow transition inline-flex items-center space-x-1"
                      >
                        <Radio className="w-3.5 h-3.5" />
                        <span>Inspect Live</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* On-Demand Live Telemetry Inspection Modal */}
      {inspectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl w-full max-w-lg p-6 shadow-2xl relative">
            <div className="flex items-center justify-between border-b border-slate-700 pb-3 mb-4">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  <Radio className="w-5 h-5 animate-pulse" />
                </div>
                <div>
                  <h3 className="font-bold text-white text-base">Live Telemetry Inspector</h3>
                  <span className="text-xs text-slate-400 font-mono">Proxied from {inspectModal.managingStation}</span>
                </div>
              </div>
              <button
                onClick={() => setInspectModal(null)}
                className="text-slate-400 hover:text-white p-1 rounded-md text-xs font-mono"
              >
                ✕
              </button>
            </div>

            <div className="grid grid-cols-2 gap-3 text-xs mb-4">
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-700">
                <span className="text-slate-400">Scooter ID:</span>
                <div className="font-mono text-white font-bold text-sm mt-0.5">{inspectModal.scooterId}</div>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-700">
                <span className="text-slate-400">Instant Speed:</span>
                <div className="font-mono text-emerald-400 font-bold text-sm mt-0.5 flex items-center space-x-1">
                  <Gauge className="w-4 h-4" />
                  <span>{inspectModal.speedKmh} km/h</span>
                </div>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-700">
                <span className="text-slate-400">Current Battery:</span>
                <div className="font-mono text-amber-400 font-bold text-sm mt-0.5 flex items-center space-x-1">
                  <Battery className="w-4 h-4" />
                  <span>{inspectModal.batteryLevel}%</span>
                </div>
              </div>
              <div className="bg-slate-900/80 p-3 rounded-xl border border-slate-700">
                <span className="text-slate-400">Managing Node:</span>
                <div className="font-mono text-blue-400 font-bold text-sm mt-0.5">{inspectModal.managingStation}</div>
              </div>
            </div>

            <div className="bg-slate-900/90 p-4 rounded-xl border border-slate-700 text-xs space-y-2 mb-4">
              <div className="flex justify-between items-center text-slate-300">
                <span className="flex items-center space-x-1">
                  <MapPin className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Exact GPS Coordinates:</span>
                </span>
                <span className="font-mono font-bold text-white">
                  {inspectModal.lat?.toFixed(6)}, {inspectModal.lon?.toFixed(6)}
                </span>
              </div>
              <div className="flex justify-between items-center text-slate-400 text-[11px]">
                <span>Last Telemetry Ping:</span>
                <span className="font-mono">{inspectModal.lastPingTimestamp}</span>
              </div>
            </div>

            <button
              onClick={() => setInspectModal(null)}
              className="w-full py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg text-xs font-semibold transition"
            >
              Close Inspector
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
