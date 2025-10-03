# Command Reference

This document provides a comprehensive reference for all commands, scripts, and configuration options available in the Real-Time Analytics Pipeline.

## 📋 **Makefile Commands**

### Core Pipeline Commands

#### `make help`
Display all available commands with descriptions.
```bash
make help
# Shows categorized list of all available commands
```

#### `make start`
Start the entire analytics pipeline.
```bash
make start
# Equivalent to: ./scripts/start-pipeline.sh
```

#### `make stop`
Stop all services and containers.
```bash
make stop
# Equivalent to: docker-compose down
```

#### `make restart`
Stop and restart the entire pipeline.
```bash
make restart
# Equivalent to: make stop && make start
```

#### `make clean`
Clean up containers, volumes, and images.
```bash
make clean
# Equivalent to: docker-compose down -v --rmi all && docker system prune -f
```

#### `make clean-volumes`
Clean up only Docker volumes.
```bash
make clean-volumes
# Equivalent to: docker-compose down -v
```

### Infrastructure Commands

#### `make start-infrastructure`
Start core infrastructure services only.
```bash
make start-infrastructure
# Starts: zookeeper, kafka-1, kafka-2, kafka-3, schema-registry, influxdb
```

#### `make start-processing`
Start processing components.
```bash
make start-processing
# Starts: flink-jobmanager, flink-taskmanager, websocket-server
```

#### `make start-applications`
Start application services.
```bash
make start-applications
# Starts: react-dashboard, event-simulator
```

#### `make start-monitoring`
Start monitoring stack.
```bash
make start-monitoring
# Starts: prometheus, grafana
```

### Setup Commands

#### `make setup-topics`
Create Kafka topics with optimal configuration.
```bash
make setup-topics
# Equivalent to: ./scripts/setup-kafka-topics.sh
```

#### `make setup-schemas`
Register Avro schemas in Schema Registry.
```bash
make setup-schemas
# Registers schemas from schemas/ directory
```

### Build Commands

#### `make build`
Build all Docker images.
```bash
make build
# Builds all services in docker-compose.yml
```

#### `make build-flink`
Build Flink streaming jobs.
```bash
make build-flink
# cd flink-jobs && mvn clean package -DskipTests
```

#### `make build-simulator`
Build event simulator.
```bash
make build-simulator
# cd event-simulator && mvn clean package -DskipTests
```

#### `make build-websocket`
Build WebSocket server.
```bash
make build-websocket
# cd websocket-server && mvn clean package -DskipTests
```

#### `make build-dashboard`
Build React dashboard.
```bash
make build-dashboard
# cd react-dashboard && npm install && npm run build
```

### Testing Commands

#### `make test`
Run comprehensive performance tests.
```bash
make test
# Equivalent to: ./scripts/performance-test.sh
```

#### `make test-latency`
Test latency requirements.
```bash
make test-latency
# Tests: P95 latency < 500ms
```

#### `make test-throughput`
Test throughput requirements.
```bash
make test-throughput
# Tests: > 10K events/minute
```

### Monitoring Commands

#### `make logs`
Show logs for all services.
```bash
make logs
# Equivalent to: docker-compose logs -f
```

#### `make logs-kafka`
Show Kafka logs.
```bash
make logs-kafka
# Shows logs for: kafka-1, kafka-2, kafka-3
```

#### `make logs-flink`
Show Flink logs.
```bash
make logs-flink
# Shows logs for: flink-jobmanager, flink-taskmanager
```

#### `make logs-dashboard`
Show dashboard logs.
```bash
make logs-dashboard
# Shows logs for: react-dashboard
```

#### `make logs-simulator`
Show event simulator logs.
```bash
make logs-simulator
# Shows logs for: event-simulator
```

#### `make status`
Show status of all services.
```bash
make status
# Equivalent to: docker-compose ps
```

#### `make health`
Check health of all services.
```bash
make health
# Checks HTTP endpoints for all services
```

### Development Commands

#### `make dev-dashboard`
Start dashboard in development mode.
```bash
make dev-dashboard
# cd react-dashboard && npm start
```

#### `make dev-websocket`
Start WebSocket server in development mode.
```bash
make dev-websocket
# cd websocket-server && mvn spring-boot:run
```

#### `make dev-flink`
Start Flink job in development mode.
```bash
make dev-flink
# cd flink-jobs && mvn exec:java -Dexec.mainClass="com.analytics.pipeline.AnalyticsStreamingJob"
```

### Scaling Commands

#### `make scale-simulator`
Scale event simulator instances.
```bash
make scale-simulator
# docker-compose up -d --scale event-simulator=3
```

#### `make scale-flink`
Scale Flink TaskManagers.
```bash
make scale-flink
# docker-compose up -d --scale flink-taskmanager=3
```

### Utility Commands

#### `make backup-data`
Backup InfluxDB data.
```bash
make backup-data
# Creates timestamped backup of InfluxDB data
```

#### `make restore-data`
Restore InfluxDB data (requires BACKUP_FILE variable).
```bash
BACKUP_FILE=20241201_120000 make restore-data
# Restores from specified backup file
```

#### `make docs`
Generate documentation.
```bash
make docs
# Displays available documentation files
```

#### `make quickstart`
Show quick start guide for new users.
```bash
make quickstart
# Displays step-by-step quick start instructions
```

## 🔧 **Script Commands**

### Pipeline Startup Script

#### `./scripts/start-pipeline.sh`
Complete pipeline startup with health checks.
```bash
./scripts/start-pipeline.sh
```

**Features**:
- Pre-flight system checks
- Orchestrated service startup
- Health validation
- Service URL display

**Environment Variables**:
```bash
export KAFKA_BROKER_ID=1
export EVENT_RATE_PER_SECOND=200
export SIMULATION_DURATION_MINUTES=60
```

### Kafka Topic Setup Script

#### `./scripts/setup-kafka-topics.sh`
Create Kafka topics with optimal configuration.
```bash
./scripts/setup-kafka-topics.sh
```

**Created Topics**:
- `website-analytics-events` (12 partitions, 3 replicas)
- `processed-metrics` (6 partitions, 3 replicas, compacted)
- `cep-alerts` (3 partitions, 3 replicas)

**Configuration**:
```bash
KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
REPLICATION_FACTOR=3
PARTITIONS=12
```

### Performance Testing Script

#### `./scripts/performance-test.sh`
Comprehensive performance testing suite.
```bash
./scripts/performance-test.sh
```

**Test Scenarios**:
1. Event Generation Rate Test
2. End-to-End Latency Test
3. Error Rate Test
4. System Resource Usage Test
5. WebSocket Connection Performance Test

**Expected Results**:
- Throughput: >10K events/minute
- Latency: <500ms P95
- Error Rate: <1%
- Memory Usage: <80%
- CPU Usage: <80%

## 🐳 **Docker Commands**

### Container Management

#### `docker-compose up`
Start services defined in docker-compose.yml.
```bash
# Start all services
docker-compose up

# Start in background
docker-compose up -d

# Start specific services
docker-compose up -d kafka-1 kafka-2 kafka-3

# Force recreate containers
docker-compose up -d --force-recreate
```

#### `docker-compose down`
Stop and remove containers.
```bash
# Stop and remove containers
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Stop and remove images
docker-compose down --rmi all
```

#### `docker-compose restart`
Restart services.
```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart kafka-1
```

#### `docker-compose scale`
Scale service instances.
```bash
# Scale Flink TaskManagers
docker-compose up -d --scale flink-taskmanager=3

# Scale event simulator
docker-compose up -d --scale event-simulator=2
```

### Container Operations

#### `docker-compose exec`
Execute commands in running containers.
```bash
# Execute bash shell
docker-compose exec kafka-1 bash

# Execute specific command
docker-compose exec kafka-1 kafka-topics.sh --list

# Execute with environment variables
docker-compose exec -e KAFKA_BROKER_ID=1 kafka-1 bash
```

#### `docker-compose logs`
View container logs.
```bash
# View all logs
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# View specific service logs
docker-compose logs kafka-1

# View logs with timestamps
docker-compose logs -t kafka-1

# View last 100 lines
docker-compose logs --tail=100 kafka-1
```

#### `docker-compose ps`
List running containers.
```bash
# List all containers
docker-compose ps

# List with more details
docker-compose ps -a
```

### Docker System Commands

#### `docker system`
Docker system management commands.
```bash
# Show Docker system information
docker system info

# Show disk usage
docker system df

# Clean up unused resources
docker system prune

# Clean up all resources
docker system prune -a

# Clean up volumes
docker volume prune

# Clean up networks
docker network prune
```

#### `docker stats`
Display resource usage statistics.
```bash
# Show stats for all containers
docker stats

# Show stats without streaming
docker stats --no-stream

# Show stats for specific containers
docker stats kafka-1 flink-taskmanager
```

## ☕ **Java/Maven Commands**

### Maven Build Commands

#### `mvn clean`
Clean build artifacts.
```bash
cd flink-jobs
mvn clean

cd ../event-simulator
mvn clean

cd ../websocket-server
mvn clean
```

#### `mvn compile`
Compile source code.
```bash
mvn compile
```

#### `mvn test`
Run unit tests.
```bash
mvn test

# Run specific test
mvn test -Dtest=AnalyticsStreamingJobTest

# Run with coverage
mvn test jacoco:report
```

#### `mvn package`
Build JAR file.
```bash
mvn package

# Skip tests
mvn package -DskipTests

# Create executable JAR
mvn package assembly:single
```

#### `mvn install`
Install to local repository.
```bash
mvn install
```

### Flink-Specific Commands

#### `mvn exec:java`
Run Java application.
```bash
cd flink-jobs
mvn exec:java -Dexec.mainClass="com.analytics.pipeline.AnalyticsStreamingJob"
```

#### `mvn spring-boot:run`
Run Spring Boot application.
```bash
cd websocket-server
mvn spring-boot:run
```

## 📦 **Node.js/npm Commands**

### React Dashboard Commands

#### `npm install`
Install dependencies.
```bash
cd react-dashboard
npm install

# Install specific version
npm install react@^18.2.0

# Install dev dependencies
npm install --save-dev @types/react
```

#### `npm start`
Start development server.
```bash
cd react-dashboard
npm start

# Start with custom port
PORT=3001 npm start
```

#### `npm run build`
Build for production.
```bash
cd react-dashboard
npm run build

# Analyze bundle size
npm run build -- --analyze
```

#### `npm test`
Run tests.
```bash
cd react-dashboard
npm test

# Run with coverage
npm test -- --coverage

# Run in watch mode
npm test -- --watch
```

#### `npm run eject`
Eject from Create React App.
```bash
cd react-dashboard
npm run eject
# Warning: This is a one-way operation
```

## 🗄️ **Database Commands**

### Kafka Commands

#### `kafka-topics.sh`
Manage Kafka topics.
```bash
# List topics
docker-compose exec kafka-1 kafka-topics.sh \
  --bootstrap-server kafka-1:29092 --list

# Create topic
docker-compose exec kafka-1 kafka-topics.sh \
  --bootstrap-server kafka-1:29092 \
  --create --topic test-topic \
  --partitions 3 --replication-factor 3

# Describe topic
docker-compose exec kafka-1 kafka-topics.sh \
  --bootstrap-server kafka-1:29092 \
  --describe --topic website-analytics-events

# Delete topic
docker-compose exec kafka-1 kafka-topics.sh \
  --bootstrap-server kafka-1:29092 \
  --delete --topic test-topic
```

#### `kafka-console-producer.sh`
Produce messages from console.
```bash
# Produce to topic
docker-compose exec kafka-1 kafka-console-producer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events

# Produce with key
docker-compose exec kafka-1 kafka-console-producer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --property "parse.key=true" \
  --property "key.separator=:"
```

#### `kafka-console-consumer.sh`
Consume messages from console.
```bash
# Consume from topic
docker-compose exec kafka-1 kafka-console-consumer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --from-beginning

# Consume with group
docker-compose exec kafka-1 kafka-console-consumer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --group test-group

# Consume with key
docker-compose exec kafka-1 kafka-console-consumer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --property "print.key=true"
```

#### `kafka-consumer-groups.sh`
Manage consumer groups.
```bash
# List consumer groups
docker-compose exec kafka-1 kafka-consumer-groups.sh \
  --bootstrap-server kafka-1:29092 --list

# Describe consumer group
docker-compose exec kafka-1 kafka-consumer-groups.sh \
  --bootstrap-server kafka-1:29092 \
  --group analytics-processing-group --describe

# Reset consumer group offset
docker-compose exec kafka-1 kafka-consumer-groups.sh \
  --bootstrap-server kafka-1:29092 \
  --group analytics-processing-group \
  --reset-offsets --to-earliest \
  --topic website-analytics-events --execute
```

### InfluxDB Commands

#### `influx`
InfluxDB CLI commands.
```bash
# Connect to InfluxDB
docker-compose exec influxdb influx

# Show databases
> SHOW DATABASES

# Use database
> USE analytics

# Show measurements
> SHOW MEASUREMENTS

# Query data
> SELECT * FROM page_views_per_second LIMIT 10

# Create retention policy
> CREATE RETENTION POLICY "30_days" ON "analytics" DURATION 30d REPLICATION 1

# Drop measurement
> DROP MEASUREMENT page_views_per_second
```

### Flink Commands

#### `flink`
Flink CLI commands.
```bash
# List jobs
docker-compose exec flink-jobmanager flink list

# Submit job
docker-compose exec flink-jobmanager flink run \
  /opt/flink/jobs/flink-streaming-jobs-1.0.0.jar

# Cancel job
docker-compose exec flink-jobmanager flink cancel <job-id>

# Savepoint job
docker-compose exec flink-jobmanager flink savepoint <job-id>

# Stop job
docker-compose exec flink-jobmanager flink stop <job-id>
```

## 📊 **Monitoring Commands**

### Prometheus Commands

#### `curl` Prometheus API
Query Prometheus metrics.
```bash
# Check Prometheus health
curl http://localhost:9090/-/healthy

# Query metrics
curl "http://localhost:9090/api/v1/query?query=up"

# Query range
curl "http://localhost:9090/api/v1/query_range?query=up&start=2024-01-01T00:00:00Z&end=2024-01-01T01:00:00Z&step=15s"

# List targets
curl http://localhost:9090/api/v1/targets

# List rules
curl http://localhost:9090/api/v1/rules
```

### Grafana Commands

#### `curl` Grafana API
Manage Grafana resources.
```bash
# Check Grafana health
curl http://localhost:3001/api/health

# List dashboards
curl -u admin:admin http://localhost:3001/api/search?type=dash-db

# Get dashboard
curl -u admin:admin http://localhost:3001/api/dashboards/uid/<dashboard-uid>

# Create dashboard
curl -u admin:admin -X POST \
  -H "Content-Type: application/json" \
  -d @dashboard.json \
  http://localhost:3001/api/dashboards/db
```

## 🌐 **WebSocket Commands**

### WebSocket Testing

#### `wscat` (WebSocket Client)
Test WebSocket connections.
```bash
# Install wscat
npm install -g wscat

# Connect to WebSocket
wscat -c ws://localhost:8082

# Send message
{"action": "subscribe", "metrics": ["pageViewsPerSecond", "activeUsers"]}
```

#### `curl` WebSocket
Test WebSocket with curl.
```bash
# Test WebSocket connection
curl -i -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: test" \
  -H "Sec-WebSocket-Version: 13" \
  http://localhost:8082
```

## 🔧 **System Commands**

### Process Management

#### `ps`
List running processes.
```bash
# List all processes
ps aux

# List Docker processes
ps aux | grep docker

# List Java processes
ps aux | grep java
```

#### `top` / `htop`
Monitor system resources.
```bash
# Monitor processes
top

# Monitor with better interface
htop

# Monitor specific process
top -p <pid>
```

#### `kill`
Terminate processes.
```bash
# Kill process by PID
kill <pid>

# Force kill process
kill -9 <pid>

# Kill all Java processes
pkill java
```

### Network Commands

#### `netstat`
Display network connections.
```bash
# List all connections
netstat -tulpn

# List listening ports
netstat -tlnp

# Check specific port
netstat -tlnp | grep :9092
```

#### `lsof`
List open files and ports.
```bash
# List processes using port
lsof -i :9092

# List all network connections
lsof -i

# List files opened by process
lsof -p <pid>
```

#### `ss`
Display socket statistics.
```bash
# List all sockets
ss -tulpn

# List listening sockets
ss -tlnp

# Check specific port
ss -tlnp | grep :9092
```

### File System Commands

#### `df`
Display disk space usage.
```bash
# Show disk usage
df -h

# Show inode usage
df -i
```

#### `du`
Display directory space usage.
```bash
# Show directory usage
du -h

# Show total usage
du -sh

# Show top 10 largest directories
du -h | sort -hr | head -10
```

#### `find`
Find files and directories.
```bash
# Find large files
find . -type f -size +100M

# Find log files
find . -name "*.log"

# Find and delete old files
find . -name "*.log" -mtime +7 -delete
```

This command reference provides comprehensive coverage of all available commands in the Real-Time Analytics Pipeline. Use this as a quick reference for daily operations and troubleshooting.
