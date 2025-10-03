## Frontend Overview (React Dashboard)

This document explains the dashboard so you can run, extend, and debug it.

### What It Does
- Connects to the WebSocket server
- Displays streaming metrics in charts and cards
- Surfaces alerts and connection status

### Source Layout (`react-dashboard/`)
- `src/App.js`: App composition
- `src/components/`
  - `RealTimeChart.js`: Live chart for metrics
  - `MetricCard.js`: KPI tiles
  - `AlertsPanel.js`: Active alerts
  - `ConnectionStatus.js`: WS connectivity UI
  - `PerformanceMetrics.js`: Client-side perf
- `src/hooks/useWebSocket.js`: Connection management and message handling
- `public/index.html`, `index.js`, `index.css`: App bootstrap and styles

### Data Format
The UI receives JSON derived from `metrics.avsc` and `alerts.avsc` via the WebSocket server. Expect fields that mirror the Avro schemas.

### Local Development
1) Ensure the backend and WebSocket server are running
2) `cd react-dashboard && npm install && npm start`
3) The dev server proxies API/WS according to `nginx.conf` and environment

### Extending the UI
- Add a component under `src/components/` and wire it in `App.js`
- Subscribe to specific message types in `useWebSocket.js`
- Keep components presentational; parse/normalize data in the hook

### Troubleshooting
- If no data appears, check WebSocket connection and backend topics
- Validate message payloads in the browser devtools network tab
- See `docs/troubleshooting-guide.md` for deeper guidance


