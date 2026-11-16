import React, { useEffect, useState } from 'react';
import { History, CheckCircle2, AlertTriangle, RefreshCw } from 'lucide-react';
import api from '../api';

export default function RideHistoryView() {
  const [rides, setRides] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/v1/rides/history', {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setRides(res.data || []);
    } catch (err) {
      console.error('Failed to load ride history', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-800/80 p-6 rounded-2xl border border-slate-700 shadow-xl">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-2 rounded-xl bg-emerald-500/20 text-emerald-400 font-bold">
              <History className="w-5 h-5" />
            </span>
            <h2 className="text-xl font-bold text-white">Ride Audit Trail & History</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Historical records persisted in PostgreSQL SSoT upon consumption of <span className="font-mono text-emerald-400">RIDE_ENDED</span> Kafka events.
          </p>
        </div>

        <button
          onClick={fetchHistory}
          disabled={loading}
          className="flex items-center space-x-2 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white text-xs font-semibold rounded-xl transition"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {rides.length === 0 ? (
        <div className="bg-slate-800/50 border border-slate-700 rounded-2xl p-12 text-center text-slate-400">
          <History className="w-12 h-12 mx-auto mb-3 text-slate-500" />
          <h3 className="font-semibold text-white">No Historical Rides Logged Yet</h3>
          <p className="text-xs mt-1">Completed rides emitted from edge nodes will be audited and displayed here.</p>
        </div>
      ) : (
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl shadow-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-slate-400 uppercase font-mono tracking-wider border-b border-slate-700">
                <tr>
                  <th className="py-3.5 px-4">Ride ID</th>
                  <th className="py-3.5 px-4">Scooter</th>
                  <th className="py-3.5 px-4">User</th>
                  <th className="py-3.5 px-4">Start Time</th>
                  <th className="py-3.5 px-4">End Coordinates</th>
                  <th className="py-3.5 px-4">Fare</th>
                  <th className="py-3.5 px-4">Parking Compliance</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/60">
                {rides.map((r) => (
                  <tr key={r.rideId} className="hover:bg-slate-700/30 transition">
                    <td className="py-3.5 px-4 font-mono font-semibold text-white">{r.rideId}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">{r.scooterId}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-300">{r.userId}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400 text-[11px]">{r.startTime}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-400 text-[11px]">
                      {r.endLat ? `${r.endLat.toFixed(4)}, ${r.endLon.toFixed(4)}` : 'In progress'}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-bold text-emerald-400">
                      {r.fareCents ? `${(r.fareCents / 100).toFixed(2)} RON` : 'N/A'}
                    </td>
                    <td className="py-3.5 px-4">
                      {r.isCompliantParking === true ? (
                        <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 font-mono text-[10px] font-bold border border-emerald-500/30">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>COMPLIANT</span>
                        </span>
                      ) : r.isCompliantParking === false ? (
                        <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full bg-rose-500/20 text-rose-400 font-mono text-[10px] font-bold border border-rose-500/30">
                          <AlertTriangle className="w-3.5 h-3.5" />
                          <span>{r.parkingViolationReason || 'VIOLATION'}</span>
                        </span>
                      ) : (
                        <span className="text-slate-500 font-mono text-[10px]">PENDING</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
