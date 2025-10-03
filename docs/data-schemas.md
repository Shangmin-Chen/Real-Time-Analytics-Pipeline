## Data Schemas (Avro)

Canonical event contracts are defined in `schemas/` and used by producers, Flink jobs, and consumers.

### Files
- `events.avsc`: Raw ingest events from the simulator and real producers
- `metrics.avsc`: Aggregated/processed metrics emitted by Flink jobs
- `alerts.avsc`: Alert payloads for anomalies or threshold breaches

### Versioning Guidance
- Treat Avro schemas as public APIs; prefer backward-compatible changes
- Add optional fields instead of removing/renaming existing ones
- When breaking changes are unavoidable, version topic names and migrate consumers

### Validation & Tooling
- Validate schemas with CI and local scripts
- Ensure Java model classes and UI deserializers are updated together

### Mapping to Code
- Java models under `flink-jobs/.../model/` mirror these schemas
- The WebSocket server converts records to JSON for the UI


