# Development Setup Guide

This comprehensive guide will help you set up a complete development environment for the Real-Time Analytics Pipeline. Follow these steps to get your local development environment running quickly and efficiently.

## 📋 **Prerequisites**

### System Requirements
- **Operating System**: macOS, Linux, or Windows with WSL2
- **RAM**: Minimum 8GB (16GB+ recommended)
- **Storage**: 10GB+ free disk space
- **CPU**: Multi-core processor (4+ cores recommended)
- **Network**: Stable internet connection for downloading dependencies

### Required Software

#### 1. **Docker & Docker Compose**
```bash
# Install Docker Desktop
# macOS: Download from https://www.docker.com/products/docker-desktop
# Linux: Follow official installation guide
# Windows: Use Docker Desktop with WSL2 backend

# Verify installation
docker --version
docker-compose --version
```

#### 2. **Java Development Kit (JDK 11)**
```bash
# macOS with Homebrew
brew install openjdk@11

# Linux (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-11-jdk

# Windows: Download from Oracle or use OpenJDK
# Verify installation
java -version
javac -version
```

#### 3. **Maven (for Java projects)**
```bash
# macOS with Homebrew
brew install maven

# Linux (Ubuntu/Debian)
sudo apt install maven

# Windows: Download from Apache Maven website
# Verify installation
mvn -version
```

#### 4. **Node.js & npm (for React dashboard)**
```bash
# macOS with Homebrew
brew install node

# Linux (Ubuntu/Debian)
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt-get install -y nodejs

# Windows: Download from Node.js website
# Verify installation
node --version
npm --version
```

#### 5. **Git**
```bash
# macOS with Homebrew
brew install git

# Linux (Ubuntu/Debian)
sudo apt install git

# Windows: Download from Git website
# Verify installation
git --version
```

## 🚀 **Quick Start Setup**

### 1. **Clone the Repository**
```bash
git clone <repository-url>
cd Real-Time-Analytics-Pipeline
```

### 2. **One-Command Setup**
```bash
# Start the entire pipeline
make start

# Or use the startup script directly
./scripts/start-pipeline.sh
```

### 3. **Verify Installation**
```bash
# Check all services are running
make health

# Expected output:
# Kafka UI: 200 ✅
# Flink Dashboard: 200 ✅
# InfluxDB: 200 ✅
# WebSocket Server: 200 ✅
# React Dashboard: 200 ✅
# Prometheus: 200 ✅
# Grafana: 200 ✅
```

## 🔧 **Detailed Development Setup**

### Step 1: Environment Configuration

#### Create Environment Files
```bash
# Create development environment file
cp .env.example .env.development

# Edit the configuration
nano .env.development
```

**Example `.env.development`**:
```bash
# Environment
ENVIRONMENT=development

# Kafka Configuration
KAFKA_BROKER_ID=1
KAFKA_NUM_PARTITIONS=12
KAFKA_REPLICATION_FACTOR=3

# Performance Settings
EVENT_RATE_PER_SECOND=200
SIMULATION_DURATION_MINUTES=60

# Monitoring
PROMETHEUS_RETENTION=7d
GRAFANA_ADMIN_PASSWORD=admin123

# Development Features
ENABLE_DEBUG_LOGGING=true
ENABLE_JMX_METRICS=true
```

#### Docker Configuration
```bash
# Create development-specific docker-compose override
cp docker-compose.yml docker-compose.override.yml

# Edit for development-specific settings
nano docker-compose.override.yml
```

**Example `docker-compose.override.yml`**:
```yaml
version: '3.8'

services:
  # Development-specific overrides
  kafka-1:
    environment:
      KAFKA_LOG4J_LOGGERS: "kafka.controller=INFO,kafka.producer.async.DefaultEventHandler=INFO,state.change.logger=INFO"
      KAFKA_JMX_OPTS: "-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9999"
    ports:
      - "9999:9999"  # JMX port for development
  
  flink-taskmanager:
    environment:
      FLINK_PROPERTIES: |
        taskmanager.numberOfTaskSlots: 4
        parallelism.default: 2
        metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
        metrics.reporter.prom.port: 9249
        logging.level: DEBUG
```

### Step 2: Build All Components

#### Build Java Components
```bash
# Build Flink streaming jobs
make build-flink

# Build event simulator
make build-simulator

# Build WebSocket server
make build-websocket
```

#### Build React Dashboard
```bash
# Install dependencies and build
make build-dashboard

# Or manually:
cd react-dashboard
npm install
npm run build
```

### Step 3: Start Development Services

#### Start Infrastructure First
```bash
# Start core infrastructure
make start-infrastructure

# Wait for services to be ready
make health
```

#### Start Processing Components
```bash
# Start processing components
make start-processing

# Check Flink is running
curl http://localhost:8081/overview
```

#### Start Applications
```bash
# Start applications
make start-applications

# Check dashboard is accessible
curl http://localhost:3000
```

### Step 4: Verify Data Flow

#### Check Event Generation
```bash
# View event simulator logs
make logs-simulator

# Expected output:
# Starting event simulation...
# Events per second: 200
# Generated event: {"eventId":"...","eventType":"PAGE_VIEW",...}
```

#### Check Kafka Topics
```bash
# List Kafka topics
docker-compose exec kafka-1 kafka-topics.sh --bootstrap-server kafka-1:29092 --list

# Expected output:
# __consumer_offsets
# website-analytics-events
# processed-metrics
# cep-alerts
```

#### Check Flink Processing
```bash
# View Flink dashboard
open http://localhost:8081

# Check running jobs
curl http://localhost:8081/jobs
```

#### Check InfluxDB Data
```bash
# Access InfluxDB UI
open http://localhost:8086

# Login with:
# Username: admin
# Password: adminpassword
```

## 🛠️ **Development Workflows**

### 1. **Java Development**

#### Project Structure
```
flink-jobs/
├── src/main/java/com/analytics/pipeline/
│   ├── AnalyticsStreamingJob.java      # Main Flink job
│   ├── model/                          # Data models
│   │   ├── AnalyticsEvent.java
│   │   ├── ProcessedMetric.java
│   │   └── CEAlert.java
│   └── sinks/                          # Custom sinks
├── src/test/java/                      # Test classes
├── pom.xml                             # Maven configuration
└── Dockerfile                          # Container definition
```

#### Development Commands
```bash
# Run tests
cd flink-jobs
mvn test

# Build and package
mvn clean package

# Run locally (for debugging)
mvn exec:java -Dexec.mainClass="com.analytics.pipeline.AnalyticsStreamingJob"

# Deploy to Flink cluster
make deploy-flink-job
```

#### Debugging Setup
```bash
# Enable remote debugging
# Add to docker-compose.override.yml
flink-taskmanager:
  environment:
    FLINK_PROPERTIES: |
      taskmanager.jvm.args: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
  ports:
    - "5005:5005"  # Debug port
```

### 2. **React Development**

#### Project Structure
```
react-dashboard/
├── src/
│   ├── components/                     # React components
│   │   ├── MetricCard.js
│   │   ├── RealTimeChart.js
│   │   └── AlertsPanel.js
│   ├── hooks/                          # Custom hooks
│   │   └── useWebSocket.js
│   ├── App.js                          # Main application
│   └── index.js                        # Entry point
├── public/
├── package.json
└── Dockerfile
```

#### Development Commands
```bash
# Start development server
cd react-dashboard
npm start

# Run tests
npm test

# Build for production
npm run build

# Analyze bundle size
npm run analyze
```

#### Hot Reload Setup
```bash
# For hot reload during development
cd react-dashboard
npm start

# This will start the development server on port 3000
# with hot reload enabled
```

### 3. **Docker Development**

#### Development Containers
```bash
# Start specific service for development
docker-compose up -d kafka-1

# View logs in real-time
docker-compose logs -f kafka-1

# Execute commands in running container
docker-compose exec kafka-1 bash

# Restart specific service
docker-compose restart flink-taskmanager
```

#### Debugging Containers
```bash
# View container resource usage
docker stats

# Inspect container configuration
docker-compose config

# View container logs
docker-compose logs --tail=100 kafka-1
```

## 🧪 **Testing Setup**

### 1. **Unit Testing**

#### Java Tests
```bash
# Run all tests
cd flink-jobs
mvn test

# Run specific test class
mvn test -Dtest=AnalyticsStreamingJobTest

# Run with coverage
mvn test jacoco:report
```

#### React Tests
```bash
# Run tests
cd react-dashboard
npm test

# Run with coverage
npm test -- --coverage

# Run in watch mode
npm test -- --watch
```

### 2. **Integration Testing**

#### End-to-End Tests
```bash
# Run performance tests
make test

# Run specific test
./scripts/performance-test.sh

# Check test results
cat test-results.json
```

#### Load Testing
```bash
# Start load test
make scale-simulator

# Monitor performance
make logs-prometheus

# Check metrics
curl http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_events_total[1m])
```

## 📊 **Monitoring Development**

### 1. **Local Monitoring Setup**

#### Prometheus Configuration
```bash
# Access Prometheus
open http://localhost:9090

# Check targets
curl http://localhost:9090/api/v1/targets

# Query metrics
curl "http://localhost:9090/api/v1/query?query=up"
```

#### Grafana Dashboards
```bash
# Access Grafana
open http://localhost:3001

# Login with:
# Username: admin
# Password: admin

# Import dashboards from monitoring/grafana/dashboards/
```

### 2. **Debugging Tools**

#### Kafka Tools
```bash
# Kafka console consumer
docker-compose exec kafka-1 kafka-console-consumer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --from-beginning

# Kafka console producer
docker-compose exec kafka-1 kafka-console-producer.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events
```

#### Flink Tools
```bash
# Flink CLI
docker-compose exec flink-jobmanager flink list

# Submit job
docker-compose exec flink-jobmanager flink run \
  /opt/flink/jobs/flink-streaming-jobs-1.0.0.jar
```

#### InfluxDB Tools
```bash
# InfluxDB CLI
docker-compose exec influxdb influx

# Query data
> USE analytics
> SHOW MEASUREMENTS
> SELECT * FROM page_views_per_second LIMIT 10
```

## 🔧 **Development Tools & IDE Setup**

### 1. **VS Code Setup**

#### Extensions
```json
{
  "recommendations": [
    "ms-vscode.vscode-docker",
    "redhat.java",
    "vscjava.vscode-java-pack",
    "ms-vscode.vscode-json",
    "bradlc.vscode-tailwindcss",
    "esbenp.prettier-vscode"
  ]
}
```

#### Settings
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "docker.defaultRegistryPath": "localhost:5000",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.organizeImports": true
  }
}
```

### 2. **IntelliJ IDEA Setup**

#### Project Import
1. Open IntelliJ IDEA
2. Import project from `flink-jobs/` directory
3. Select "Import project from external model" → "Maven"
4. Configure SDK to JDK 11
5. Enable annotation processing

#### Run Configurations
```xml
<!-- Flink Job Run Configuration -->
<configuration>
  <option name="MAIN_CLASS_NAME" value="com.analytics.pipeline.AnalyticsStreamingJob" />
  <option name="PROGRAM_PARAMETERS" value="--bootstrap.servers localhost:9092" />
  <option name="VM_PARAMETERS" value="-Dlog4j.configuration=file:log4j.properties" />
</configuration>
```

### 3. **Docker Desktop Setup**

#### Resource Allocation
```json
{
  "cpu": 4,
  "memory": 8192,
  "disk": 100,
  "experimental": false,
  "kubernetes": {
    "enabled": false
  }
}
```

## 🚨 **Troubleshooting Common Issues**

### 1. **Docker Issues**

#### Port Conflicts
```bash
# Check port usage
lsof -i :9092
lsof -i :8081
lsof -i :3000

# Kill processes using ports
sudo kill -9 $(lsof -t -i:9092)
```

#### Memory Issues
```bash
# Check Docker memory usage
docker system df
docker system prune -f

# Increase Docker memory limit in Docker Desktop
```

#### Network Issues
```bash
# Reset Docker network
docker network prune -f
docker-compose down
docker-compose up -d
```

### 2. **Java Issues**

#### Maven Build Failures
```bash
# Clean Maven cache
mvn clean
rm -rf ~/.m2/repository

# Rebuild
mvn clean install
```

#### Classpath Issues
```bash
# Check Java version
java -version
javac -version

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk
```

### 3. **Node.js Issues**

#### npm Install Failures
```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

#### Port Conflicts
```bash
# Check if port 3000 is in use
lsof -i :3000

# Kill process or use different port
PORT=3001 npm start
```

## 📚 **Additional Resources**

### Documentation
- [Docker Documentation](https://docs.docker.com/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Flink Documentation](https://flink.apache.org/docs/)
- [React Documentation](https://reactjs.org/docs/)
- [InfluxDB Documentation](https://docs.influxdata.com/)

### Community
- [Stack Overflow](https://stackoverflow.com/questions/tagged/apache-kafka)
- [Flink User Mailing List](https://flink.apache.org/community.html)
- [InfluxDB Community](https://community.influxdata.com/)

### Learning Resources
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Flink Training](https://training.ververica.com/)
- [React Tutorial](https://reactjs.org/tutorial/tutorial.html)

This development setup guide provides everything needed to start developing with the Real-Time Analytics Pipeline. Follow the steps in order, and you'll have a fully functional development environment in minutes.
