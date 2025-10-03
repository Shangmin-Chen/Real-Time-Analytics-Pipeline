## Getting Started

Follow these steps to run the Real-Time Analytics Pipeline locally.

### Prerequisites
- Docker Desktop
- Java 11+
- Node 18+

### 1) Clone and Prepare
```bash
git clone <your-fork-or-origin>
cd Real-Time-Analytics-Pipeline
```

### 2) Start the Stack
```bash
./scripts/start-pipeline.sh
```
This launches Kafka, Flink, Prometheus, Grafana, WebSocket server, and the dashboard via Docker Compose.

### 3) Seed Data
Run the event simulator to generate traffic into Kafka.

### 4) Open the Dashboard
Navigate to the dashboard URL logged by the compose output. You should see metrics updating in real time.

### 5) Develop
- Backend: open `flink-jobs/` or `websocket-server/`
- Frontend: `cd react-dashboard && npm install && npm start`

If anything fails, see `docs/troubleshooting-guide.md`.


