## Backend Overview (Flink + WebSocket Server)

This document explains the backend services so you can navigate, run, and change them confidently.

### Components
- Flink Streaming Jobs (`flink-jobs/`)
  - Ingests Kafka events, applies transformations/aggregations, and outputs metrics and alerts.
  - Tech: Java 11+, Apache Flink, Kafka, Avro
- WebSocket Server (`websocket-server/`)
  - Subscribes to processed topics and streams data to the UI via WebSocket.
  - Tech: Java 11+, WebSocket API, Kafka
- Event Simulator (`event-simulator/`)
  - Generates synthetic traffic into Kafka for local testing and demos.

### Source Layout
- `flink-jobs/src/main/java/com/analytics/pipeline/`
  - `AnalyticsStreamingJob.java`: Main job wiring, sources/sinks, and topology
  - `model/`: Avro-backed model types used in the pipeline (e.g., `AnalyticsEvent`, `ProcessedMetric`, `CEAlert`)
- `websocket-server/src/main/java/com/analytics/websocket/`
  - `WebSocketServer.java`: WebSocket endpoint and Kafka consumer loop
- `event-simulator/src/main/java/com/analytics/simulator/`
  - `EventSimulator.java`: Synthetic event producer

### Data Contracts
Avro schemas live in `schemas/`:
- `events.avsc`: raw ingest events
- `metrics.avsc`: processed metrics emitted by Flink
- `alerts.avsc`: alert payloads

These are referenced by Java model classes in `flink-jobs/model/` and by consumers (WebSocket server and UI).

### Typical Data Flow
1) Simulator publishes `AnalyticsEvent` to Kafka `events` topic
2) Flink job reads `events`, transforms, aggregates, and emits to `metrics` and `alerts` topics
3) WebSocket server consumes `metrics`/`alerts` and publishes JSON via WebSocket to the dashboard

### How to Run (Local)
1) Ensure Docker is running
2) From repo root, use `scripts/start-pipeline.sh` or `docker-compose.yml`
3) Start the event simulator once the stack is healthy

Details: see `docs/deployment-guide.md` and `docs/command-reference.md`.

### Making Changes Safely
- Update Avro schemas first; regenerate or validate models if structure changes
- Keep Flink operator state compatibility in mind when changing keyed state or serialization
- Prefer adding new Kafka topics when making breaking changes; migrate consumers incrementally
- Add metrics and logs for new operators for observability (Prometheus)

### Key Classes to Read First
- `AnalyticsStreamingJob.java` for the end-to-end pipeline
- `AnalyticsEvent`, `ProcessedMetric`, `CEAlert` models
- `WebSocketServer.java` to understand the UI data plane


