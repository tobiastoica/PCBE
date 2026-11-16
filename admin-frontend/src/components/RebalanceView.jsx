import React, { useEffect, useState } from 'react';
import { Bot, CheckCircle, Clock, TrendingUp, AlertCircle, ArrowRight } from 'lucide-react';
import api from '../api';

export default function RebalanceView() {
  const [proposals, setProposals] = useState([]);
  const [demandData, setDemandData] = useState(null);
  const [targetHour, setTargetHour] = useState(8);
  const [selectedStation, setSelectedStation] = useState('station_a');
  const [loading, setLoading] = useState(true);
  const [approvingId, setApprovingId] = useState(null);
  const [msg, setMsg] = useState('');

  useEffect(() => {
    fetchProposals();
    fetchDemand();
  }, [targetHour, selectedStation]);

  const fetchProposals = async () => {
    setLoading(true);
    try {
      const res = await api.get('/api/v1/rebalance/pending', {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setProposals(res.data || []);
    } catch (err) {
      console.error('Failed to fetch rebalance proposals', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchDemand = async () => {
    try {
      const res = await api.get('/api/v1/analytics/demand', {
        params: { stationId: selectedStation, targetHour },
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setDemandData(res.data);
    } catch (err) {
      console.error('Failed to fetch demand prediction', err);
    }
  };

  const handleApprove = async (taskId) => {
    setApprovingId(taskId);
    setMsg('');
    try {
      await api.post(`/api/v1/rebalance/${taskId}/approve`, {}, {
        headers: { 'X-API-KEY': 'fleet-secret-key-2026' },
      });
      setMsg(`Proposal ${taskId} approved! Dispatched to Kafka.`);
      fetchProposals();
    } catch (err) {
      setMsg('Failed to approve proposal: ' + (err.response?.data?.message || err.message));
    } finally {
      setApprovingId(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-800/80 p-6 rounded-2xl border border-slate-700 shadow-xl">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-2 rounded-xl bg-purple-500/20 text-purple-400 font-bold">
              <Bot className="w-5 h-5" />
            </span>
            <h2 className="text-xl font-bold text-white">Autonomous Fleet Rebalancing & AI Dispatch</h2>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Agentic Rebalancing Loop: Scheduled background agents audit zone inventories via MCP and submit structured rebalancing orders for human dispatch review.
          </p>
        </div>

        <div className="px-4 py-2 rounded-xl bg-slate-900/90 border border-slate-700 font-mono text-xs">
          <span className="text-slate-400">Pending Review: </span>
          <span className="text-purple-400 font-bold text-sm">{proposals.length}</span>
        </div>
      </div>

      {msg && (
        <div className="p-3 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs rounded-xl flex items-center space-x-2">
          <CheckCircle className="w-4 h-4 flex-shrink-0" />
          <span>{msg}</span>
        </div>
      )}

      {/* Grid: Proposals List + Demand Prediction Card */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Proposals List (2 columns on lg) */}
        <div className="lg:col-span-2 space-y-4">
          <h3 className="font-bold text-sm text-slate-300 flex items-center space-x-2">
            <Clock className="w-4 h-4 text-purple-400" />
            <span>Pending Rebalancing Work Orders</span>
          </h3>

          {proposals.length === 0 ? (
            <div className="bg-slate-800/50 border border-slate-700 rounded-2xl p-8 text-center text-slate-400">
              <CheckCircle className="w-10 h-10 mx-auto mb-2 text-emerald-400/80" />
              <p className="text-sm font-semibold text-white">All Stations Optimally Balanced</p>
              <p className="text-xs mt-1">No deficit or surplus tickets pending operator approval.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {proposals.map((task) => (
                <div
                  key={task.taskId}
                  className="bg-slate-800/90 border border-slate-700 rounded-2xl p-5 shadow-lg space-y-3 hover:border-slate-600 transition"
                >
                  <div className="flex justify-between items-start">
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 font-mono text-[10px] font-bold border border-purple-500/30">
                        {task.taskId}
                      </span>
                      <span className="text-xs text-slate-400 font-mono">by {task.proposedBy}</span>
                    </div>
                    <span className="text-[11px] text-slate-400 font-mono">{task.createdAt}</span>
                  </div>

                  {/* Transfer Route */}
                  <div className="flex items-center space-x-3 bg-slate-900/80 p-3 rounded-xl border border-slate-700/60 text-xs font-mono">
                    <span className="text-amber-400 font-bold">{task.sourceStationId}</span>
                    <ArrowRight className="w-4 h-4 text-slate-500" />
                    <span className="text-emerald-400 font-bold">{task.targetStationId}</span>
                    <span className="text-slate-400 text-[11px] ml-auto">
                      Vehicles: <span className="text-white font-bold">{task.scooterIds?.join(', ') || 'N/A'}</span>
                    </span>
                  </div>

                  {/* LLM Justification */}
                  <div className="bg-slate-900/40 p-3 rounded-xl border border-slate-700/40 text-xs text-slate-300 italic">
                    "{task.justification}"
                  </div>

                  {/* Action */}
                  <div className="flex justify-end pt-1">
                    <button
                      onClick={() => handleApprove(task.taskId)}
                      disabled={approvingId === task.taskId}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold shadow transition disabled:opacity-50 flex items-center space-x-1.5"
                    >
                      <CheckCircle className="w-4 h-4" />
                      <span>{approvingId === task.taskId ? 'Approving...' : 'Approve & Dispatch'}</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Demand Forecasting Widget */}
        <div className="space-y-4">
          <h3 className="font-bold text-sm text-slate-300 flex items-center space-x-2">
            <TrendingUp className="w-4 h-4 text-emerald-400" />
            <span>Demand Prediction Analytics</span>
          </h3>

          <div className="bg-slate-800/90 border border-slate-700 rounded-2xl p-5 shadow-lg space-y-4">
            <p className="text-xs text-slate-400">
              Query historical demand models for station rush-hour logistics:
            </p>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-400 mb-1">Target Station:</label>
                <select
                  value={selectedStation}
                  onChange={(e) => setSelectedStation(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white font-mono"
                >
                  <option value="station_a">Station A (Victory Square)</option>
                  <option value="station_b">Station B (Central Park)</option>
                  <option value="station_c">Station C (Union Square)</option>
                  <option value="station_d">Station D (Bega Promenade)</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Target Hour of Day (0-23):</label>
                <div className="flex items-center space-x-3">
                  <input
                    type="range"
                    min="0"
                    max="23"
                    value={targetHour}
                    onChange={(e) => setTargetHour(parseInt(e.target.value))}
                    className="flex-1 accent-emerald-500"
                  />
                  <span className="font-mono text-sm font-bold text-emerald-400 w-12 text-right">
                    {targetHour.toString().padStart(2, '0')}:00
                  </span>
                </div>
              </div>
            </div>

            {demandData && (
              <div className="pt-3 border-t border-slate-700 space-y-2 text-xs">
                <div className="flex justify-between items-center bg-slate-900/60 p-3 rounded-xl border border-slate-700/50">
                  <span className="text-slate-400">Predicted Demand:</span>
                  <span className="font-mono font-bold text-emerald-400 text-sm">{demandData.predictedDemand} units</span>
                </div>
                <div className="flex justify-between items-center bg-slate-900/60 p-3 rounded-xl border border-slate-700/50">
                  <span className="text-slate-400">Historical Average:</span>
                  <span className="font-mono font-semibold text-slate-300 text-sm">{demandData.historicalAvg} units</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
