## Repository Structure

High-level layout with purpose of each directory.

- `event-simulator/`: Publish synthetic events to Kafka for local testing
- `flink-jobs/`: Apache Flink streaming jobs and models (Java)
- `websocket-server/`: WebSocket gateway consuming Kafka and serving the UI
- `react-dashboard/`: React UI for real-time metrics and alerts
- `schemas/`: Avro schema definitions for events, metrics, and alerts
- `monitoring/`: Prometheus and Grafana configuration and dashboards
- `scripts/`: Helper scripts for setup and operations
- `docs/`: All documentation (start at `docs/README.md`)
- `docker-compose.yml`: Local orchestration
- `Makefile`: Common commands shortcuts


