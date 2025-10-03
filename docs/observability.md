## Observability (Prometheus + Grafana)

Monitor the health and performance of the pipeline using Prometheus and Grafana.

### What We Export
- Flink job metrics (task throughput, backpressure, checkpoints)
- WebSocket server metrics (message rate, errors)
- System metrics (container CPU/memory)

### Configuration
- Prometheus: `monitoring/prometheus.yml`
- Alert rules: `monitoring/rules/analytics-alerts.yml`
- Grafana: dashboards under `monitoring/grafana/`

### How to Use
1) Start the stack (`scripts/start-pipeline.sh`)
2) Open Grafana (see compose logs for URL and credentials)
3) Load `analytics-pipeline` dashboard

### Adding Metrics
- Backend: expose counters/gauges/timers; ensure Prometheus scrape annotations
- Flink: rely on built-in metrics; add custom operator metrics when needed


