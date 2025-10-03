## FAQ

### How do I run everything locally?
Use `./scripts/start-pipeline.sh`. See `docs/getting-started.md` for details.

### Where do schemas live?
In `schemas/` as Avro `.avsc` files.

### Why am I not seeing data in the dashboard?
Ensure the WebSocket server is running and connected, Flink job is active, and Kafka topics have traffic.

### Can I change the schema?
Yes, but prefer backward-compatible changes. Version topics when breaking changes are required.

### How are metrics collected?
Prometheus scrapes exporters and Flink metrics; Grafana visualizes them.


