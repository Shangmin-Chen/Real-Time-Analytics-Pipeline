## Operations Runbook

Actionable procedures for common operational tasks and incidents.

### Start/Stop
- Start: `./scripts/start-pipeline.sh`
- Stop: `docker compose down`

### Verify Health
- Grafana dashboard `analytics-pipeline`: look for rising backpressure or lag
- Prometheus alerts: review active alerts
- WebSocket: dashboard connection status is green

### Common Incidents
- No data in UI
  - Check WebSocket server logs
  - Verify Kafka topics have messages
  - Ensure Flink job is running and not backpressured
- High latency
  - Inspect Flink checkpointing and operator backpressure
  - Scale parallelism or provision more resources
- Schema mismatch
  - Compare Avro versions; ensure consumers were updated and deployed

### Scaling
- Increase Flink task parallelism and Kafka partitions
- Horizontal scale WebSocket server instances

### Change Management
- Version Avro schemas when breaking changes are required
- Blue/green deploy consumers for safe migration


