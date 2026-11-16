import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { Battery, Zap, RefreshCw, Search, Layers, Radio } from 'lucide-react';
import api from '../api';

export default function FleetMapView() {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersLayerRef = useRef(null);

  const [scooters, setScooters] = useState([]);
  const [stations, setStations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [radiusMeters, setRadiusMeters] = useState(2500);
  const [filterBattery, setFilterBattery] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedItem, setSelectedItem] = useState(null);
  const [mapTheme, setMapTheme] = useState('dark');

  // Default Center: Timisoara Center (Victory Square)
  const defaultCenter = [45.7538, 21.2257];

  // Initialize Leaflet Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    if (!mapInstanceRef.current) {
      const map = L.map(mapContainerRef.current, {
        center: defaultCenter,
        zoom: 15,
        zoomControl: true,
      });

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19,
      }).addTo(map);

      markersLayerRef.current = L.layerGroup().addTo(map);
      mapInstanceRef.current = map;

      // Invalidate map size after layout is stabilized
      const timer = setTimeout(() => {
        map.invalidateSize();
      }, 300);

      const handleResize = () => map.invalidateSize();
      window.addEventListener('resize', handleResize);

      fetchData();

      return () => {
        clearTimeout(timer);
        window.removeEventListener('resize', handleResize);
      };
    } else {
      setTimeout(() => {
        mapInstanceRef.current?.invalidateSize();
      }, 100);
      fetchData();
    }
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      // 1. Fetch stationary available scooters from Central Discovery API (Redis-backed)
      const resNearby = await api.get('/api/v1/scooters/nearby', {
        params: {
          lat: defaultCenter[0],
          lon: defaultCenter[1],
          radiusMeters: 10000,
        },
      });
      setScooters(resNearby.data || []);

      // 2. Fetch stations from Fleet Overview
      try {
        const resOverview = await api.get('/api/v1/admin/fleet-overview', {
          headers: { 'X-API-KEY': 'fleet-secret-key-2026' }
        });
        if (resOverview.data && resOverview.data.stations) {
          setStations(resOverview.data.stations);
        }
      } catch (e) {
        console.warn('Overview fetch error, continuing with scooters');
      }
    } catch (err) {
      console.error('Failed to fetch fleet data', err);
    } finally {
      setLoading(false);
    }
  };

  // Re-render markers on map when data or filters change
  useEffect(() => {
    if (!mapInstanceRef.current || !markersLayerRef.current) return;

    markersLayerRef.current.clearLayers();

    // 1. Render Stations
    stations.forEach((st) => {
      const stationIcon = L.divIcon({
        className: 'custom-station-pin',
        html: `
          <div class="relative flex items-center justify-center w-10 h-10 rounded-xl bg-blue-600 border-2 border-white shadow-xl text-white font-bold text-xs cursor-pointer transform hover:scale-110 transition">
            <span class="text-sm">⚡</span>
            <span class="absolute -top-2 -right-2 bg-blue-900 border border-blue-400 text-white text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold">${st.dockedCount}/${st.capacity}</span>
          </div>
        `,
        iconSize: [40, 40],
        iconAnchor: [20, 20],
      });

      const marker = L.marker([st.lat, st.lon], { icon: stationIcon });
      marker.on('click', () => {
        setSelectedItem({ type: 'station', data: st });
      });
      marker.bindTooltip(`<b>${st.name}</b><br/>Docked: ${st.dockedCount} / Capacity: ${st.capacity}`, {
        direction: 'top',
        className: 'bg-slate-800 text-white border-slate-700 font-sans text-xs',
      });
      marker.addTo(markersLayerRef.current);
    });

    // 2. Filter & Render Stationary Scooters
    const filteredScooters = scooters.filter((sc) => {
      const matchesSearch =
        sc.scooterId.toLowerCase().includes(searchQuery.toLowerCase()) ||
        sc.model.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (sc.stationName && sc.stationName.toLowerCase().includes(searchQuery.toLowerCase()));
      const matchesBattery = sc.batteryLevel >= filterBattery;
      return matchesSearch && matchesBattery;
    });

    filteredScooters.forEach((sc) => {
      let pinColor = 'bg-emerald-500 border-emerald-300';
      if (sc.batteryLevel < 30) pinColor = 'bg-rose-500 border-rose-300';
      else if (sc.batteryLevel < 60) pinColor = 'bg-amber-500 border-amber-300';

      const scooterIcon = L.divIcon({
        className: 'custom-scooter-pin',
        html: `
          <div class="relative flex items-center justify-center w-8 h-8 rounded-full ${pinColor} border-2 shadow-lg text-slate-950 font-bold text-xs cursor-pointer transform hover:scale-125 transition">
            <span>🛴</span>
            <span class="absolute -bottom-2 bg-slate-900 border border-slate-700 text-white text-[9px] px-1 rounded font-mono font-bold">${sc.batteryLevel}%</span>
          </div>
        `,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      const marker = L.marker([sc.lat, sc.lon], { icon: scooterIcon });
      marker.on('click', () => {
        setSelectedItem({ type: 'scooter', data: sc });
      });
      marker.bindTooltip(`<b>${sc.model}</b> (${sc.scooterId})<br/>Battery: ${sc.batteryLevel}%`, {
        direction: 'top',
        className: 'bg-slate-800 text-white border-slate-700 font-sans text-xs',
      });
      marker.addTo(markersLayerRef.current);
    });
  }, [scooters, stations, searchQuery, filterBattery]);

  return (
    <div className="relative w-full h-[calc(100vh-4rem)] flex flex-col">
      {/* Top Filter and Stats Bar */}
      <div className="bg-slate-800/95 border-b border-slate-700 p-3 sm:px-6 flex flex-wrap items-center justify-between gap-3 z-20 shadow-md">
        <div className="flex items-center space-x-3 flex-1 min-w-[280px]">
          <div className="relative flex-1 max-w-sm">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search scooter ID or station..."
              className="w-full pl-9 pr-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-sm text-white placeholder-slate-400 focus:outline-none focus:border-emerald-500"
            />
          </div>

          <div className="flex items-center space-x-2">
            <span className="text-xs text-slate-400 font-medium">Battery:</span>
            <select
              value={filterBattery}
              onChange={(e) => setFilterBattery(Number(e.target.value))}
              className="bg-slate-900 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-white focus:outline-none"
            >
              <option value="0">All levels</option>
              <option value="30">≥ 30%</option>
              <option value="60">≥ 60%</option>
              <option value="80">≥ 80%</option>
            </select>
          </div>
        </div>

        {/* Global summary chips */}
        <div className="flex items-center space-x-4 text-xs font-mono">
          <div className="flex items-center space-x-1.5 bg-slate-900/80 px-3 py-1.5 rounded-lg border border-slate-700">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-slate-400">Available:</span>
            <span className="text-emerald-400 font-bold text-sm">{scooters.length}</span>
          </div>

          <div className="flex items-center space-x-1.5 bg-slate-900/80 px-3 py-1.5 rounded-lg border border-slate-700">
            <span className="text-blue-400 font-bold">⚡</span>
            <span className="text-slate-400">Stations:</span>
            <span className="text-blue-400 font-bold text-sm">{stations.length}</span>
          </div>

          <button
            onClick={fetchData}
            disabled={loading}
            className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition disabled:opacity-50"
            title="Refresh Map Pins"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>

          <button
            onClick={() => setMapTheme(prev => prev === 'dark' ? 'streets' : 'dark')}
            className="flex items-center space-x-1.5 px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition"
            title="Toggle Map Style"
          >
            <Layers className="w-3.5 h-3.5" />
            <span>{mapTheme === 'dark' ? '🌙 Dark' : '☀️ Streets'}</span>
          </button>
        </div>
      </div>

      {/* Main Map Canvas */}
      <div className={`relative flex-1 w-full h-full ${mapTheme === 'dark' ? 'dark-map' : ''}`}>
        <div ref={mapContainerRef} className="w-full h-full" />

        {/* Floating Detail Overlay Drawer */}
        {selectedItem && (
          <div className="absolute bottom-6 left-6 z-30 w-80 bg-slate-800/95 backdrop-blur border border-slate-700 rounded-2xl p-5 shadow-2xl animate-fade-in">
            <div className="flex items-center justify-between border-b border-slate-700 pb-3 mb-3">
              <div className="flex items-center space-x-2">
                <span className="text-xl">{selectedItem.type === 'station' ? '⚡' : '🛴'}</span>
                <div>
                  <h3 className="font-bold text-sm text-white">
                    {selectedItem.type === 'station' ? selectedItem.data.name : selectedItem.data.model}
                  </h3>
                  <span className="text-[10px] text-slate-400 font-mono">
                    {selectedItem.type === 'station' ? selectedItem.data.stationId : selectedItem.data.scooterId}
                  </span>
                </div>
              </div>
              <button
                onClick={() => setSelectedItem(null)}
                className="text-slate-400 hover:text-white p-1 rounded-md text-xs font-mono"
              >
                ✕
              </button>
            </div>

            {selectedItem.type === 'scooter' ? (
              <div className="space-y-2.5 text-xs">
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Battery Level:</span>
                  <span className="font-bold text-emerald-400 flex items-center space-x-1">
                    <Battery className="w-4 h-4" />
                    <span>{selectedItem.data.batteryLevel}%</span>
                  </span>
                </div>
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Status:</span>
                  <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-mono text-[10px] font-bold">
                    {selectedItem.data.status}
                  </span>
                </div>
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Assigned Station:</span>
                  <span className="font-medium text-slate-200">{selectedItem.data.stationName || selectedItem.data.stationId}</span>
                </div>
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Coordinates:</span>
                  <span className="font-mono text-slate-300 text-[11px]">
                    {selectedItem.data.lat.toFixed(4)}, {selectedItem.data.lon.toFixed(4)}
                  </span>
                </div>
              </div>
            ) : (
              <div className="space-y-2.5 text-xs">
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Docked Capacity:</span>
                  <span className="font-bold text-blue-400 font-mono">
                    {selectedItem.data.dockedCount} / {selectedItem.data.capacity}
                  </span>
                </div>
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Edge Port:</span>
                  <span className="font-mono text-slate-300">:{selectedItem.data.port}</span>
                </div>
                <div className="flex justify-between items-center bg-slate-900/60 p-2 rounded-lg">
                  <span className="text-slate-400">Node Status:</span>
                  <span className="px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 font-mono text-[10px] font-bold">
                    {selectedItem.data.status}
                  </span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
