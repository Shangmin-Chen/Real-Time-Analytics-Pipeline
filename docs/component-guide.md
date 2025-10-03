# Component Guide

This comprehensive guide explains each component of the Real-Time Analytics Pipeline, their responsibilities, implementation details, and how they interact with each other.

## 📁 **Project Structure**

```
Real-Time-Analytics-Pipeline/
├── docker-compose.yml              # Service orchestration
├── Makefile                        # Development commands
├── scripts/                        # Automation scripts
│   ├── start-pipeline.sh          # Pipeline startup
│   ├── setup-kafka-topics.sh      # Kafka topic creation
│   └── performance-test.sh         # Performance testing
├── schemas/                        # Avro schema definitions
│   ├── events.avsc                # Analytics event schema
│   ├── metrics.avsc               # Processed metrics schema
│   └── alerts.avsc                # CEP alerts schema
├── flink-jobs/                     # Stream processing jobs
│   ├── src/main/java/             # Java source code
│   └── pom.xml                    # Maven configuration
├── event-simulator/                # Event generation service
│   ├── src/main/java/             # Java source code
│   └── pom.xml                    # Maven configuration
├── websocket-server/               # Real-time data server
│   ├── src/main/java/             # Java source code
│   └── pom.xml                    # Maven configuration
├── react-dashboard/                # Frontend dashboard
│   ├── src/                       # React source code
│   └── package.json               # Node.js dependencies
├── monitoring/                     # Observability stack
│   ├── prometheus.yml             # Metrics configuration
│   └── grafana/                   # Dashboard definitions
└── docs/                          # Documentation
```

## 🏗️ **Infrastructure Components**

### 1. **Apache Kafka Cluster**

**Location**: `docker-compose.yml` (kafka-1, kafka-2, kafka-3)

**Purpose**: Distributed event streaming platform that handles high-throughput, fault-tolerant event processing.

**Key Configuration**:
```yaml
# High availability setup
KAFKA_BROKER_ID: 1/2/3
KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:29092

# Performance optimization
KAFKA_BATCH_SIZE: 16384
KAFKA_LINGER_MS: 5
KAFKA_COMPRESSION_TYPE: 'snappy'
KAFKA_NUM_PARTITIONS: 12
KAFKA_DEFAULT_REPLICATION_FACTOR: 3
```

**Topics Created**:
- `website-analytics-events` (12 partitions) - Raw events
- `processed-metrics` (6 partitions) - Aggregated metrics
- `cep-alerts` (3 partitions) - Pattern-based alerts

**Responsibilities**:
- Buffering events with high durability
- Enabling parallel processing via partitioning
- Providing exactly-once semantics
- Supporting schema evolution

### 2. **Apache Zookeeper**

**Location**: `docker-compose.yml` (zookeeper)

**Purpose**: Coordination service for Kafka cluster management.

**Key Configuration**:
```yaml
ZOOKEEPER_CLIENT_PORT: 2181
ZOOKEEPER_TICK_TIME: 2000
```

**Responsibilities**:
- Kafka broker coordination
- Topic metadata management
- Consumer group coordination
- Cluster state maintenance

### 3. **Schema Registry**

**Location**: `docker-compose.yml` (schema-registry)

**Purpose**: Centralized schema management for Avro serialization.

**Key Configuration**:
```yaml
SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: 'kafka-1:29092,kafka-2:29092,kafka-3:29092'
SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
```

**Responsibilities**:
- Schema versioning and compatibility
- Client schema caching
- Schema evolution support
- Data governance

## ⚙️ **Processing Components**

### 4. **Apache Flink JobManager**

**Location**: `docker-compose.yml` (flink-jobmanager)

**Purpose**: Central coordinator for Flink streaming jobs.

**Key Configuration**:
```yaml
# Memory and performance tuning
jobmanager.memory.process.size: 2048m
taskmanager.memory.process.size: 4096m
taskmanager.memory.managed.fraction: 0.4

# Checkpointing for fault tolerance
state.backend: rocksdb
state.checkpoints.dir: file:///opt/flink/checkpoints
execution.checkpointing.interval: 10000
execution.checkpointing.mode: EXACTLY_ONCE
```

**Responsibilities**:
- Job scheduling and coordination
- Checkpoint coordination
- Resource management
- Job recovery and failover

### 5. **Apache Flink TaskManager**

**Location**: `docker-compose.yml` (flink-taskmanager)

**Purpose**: Worker nodes that execute streaming jobs.

**Key Configuration**:
```yaml
taskmanager.numberOfTaskSlots: 4
parallelism.default: 2
metrics.reporter.prom.class: org.apache.flink.metrics.prometheus.PrometheusReporter
```

**Responsibilities**:
- Stream processing execution
- State management
- Checkpoint execution
- Metrics collection

### 6. **Flink Streaming Jobs**

**Location**: `flink-jobs/src/main/java/com/analytics/pipeline/`

**Purpose**: Core business logic for real-time analytics processing.

**Main Classes**:

#### `AnalyticsStreamingJob.java`
```java
// Main entry point for stream processing
public class AnalyticsStreamingJob {
    // Real-time metrics (5-second windows)
    // Business metrics (1-minute windows)  
    // Session analytics (session windows)
    // CEP pattern detection
}
```

**Key Processing Logic**:
- **Windowed Aggregations**: Tumbling, sliding, and session windows
- **Complex Event Processing**: Pattern detection for alerts
- **Exactly-Once Processing**: Transactional guarantees
- **Multi-Stream Processing**: Parallel processing of different event types

#### `AnalyticsEvent.java`
```java
// Core event model with nested data structures
public class AnalyticsEvent {
    private String eventId;
    private Long timestamp;
    private String eventType;
    private PageViewData pageViewData;
    private SessionData sessionData;
    private ConversionData conversionData;
}
```

**Processing Patterns**:
1. **Real-Time Metrics**: 5-second tumbling windows for immediate insights
2. **Business Metrics**: 1-minute sliding windows for trend analysis
3. **Session Analytics**: Event-time session windows for user journey tracking
4. **CEP Patterns**: Complex pattern detection for anomaly alerts

## 💾 **Storage Components**

### 7. **InfluxDB**

**Location**: `docker-compose.yml` (influxdb)

**Purpose**: High-performance time-series database for analytics metrics.

**Key Configuration**:
```yaml
# Performance optimization
INFLUXDB_DATA_CACHE_MAX_MEMORY_SIZE: 1073741824  # 1GB
INFLUXDB_DATA_INDEX_VERSION: tsi1
DOCKER_INFLUXDB_INIT_BUCKET: metrics
```

**Data Organization**:
```
Organization: analytics
Bucket: metrics
Retention: 30 days (configurable)
Shard Duration: 24 hours
```

**Responsibilities**:
- High-frequency metric ingestion
- Time-series data compression
- Efficient aggregation queries
- Automatic data retention

**Schema Design**:
```influxql
# Page views per second
page_views_per_second,metric=real_time value=150.5

# Active users
active_users,metric=real_time value=1250

# Conversion rate
conversion_rate,metric=business value=0.025

# Session duration
average_session_duration,session_id=abc123 value=180.5
```

## 🌐 **Application Components**

### 8. **Event Simulator**

**Location**: `event-simulator/src/main/java/com/analytics/simulator/`

**Purpose**: Generates realistic website analytics events for testing and demonstration.

**Key Features**:
```java
public class EventSimulator {
    // Configurable event rates (default: 200 events/second)
    private static final int EVENTS_PER_SECOND = 200;
    
    // Realistic user behavior patterns
    private final Map<String, UserSession> activeSessions;
    
    // Geographic and device diversity
    private final List<String> countries, cities, devices, browsers;
}
```

**Event Generation Logic**:
1. **User Session Management**: Maintains active user sessions
2. **Realistic Data**: Generates geographically distributed events
3. **Event Type Distribution**: 70% page views, 15% sessions, 10% conversions
4. **Performance Optimization**: Efficient Kafka producer configuration

**Generated Event Types**:
- **PageView Events**: URL, title, referrer, load time
- **Session Events**: User agent, IP, location, device info
- **Conversion Events**: Type, value, product, category

### 9. **WebSocket Server**

**Location**: `websocket-server/src/main/java/com/analytics/websocket/`

**Purpose**: Provides real-time data streaming to dashboard clients.

**Key Features**:
```java
public class WebSocketServer {
    // InfluxDB integration for metrics retrieval
    private final InfluxDBClient influxDB;
    
    // Client connection management
    private final Map<Session, ClientSubscription> activeSessions;
    
    // Scheduled metrics broadcasting
    private final ScheduledExecutorService scheduler;
}
```

**Responsibilities**:
- Real-time metrics broadcasting (5-second intervals)
- Client connection management
- InfluxDB query optimization
- WebSocket connection handling

**Data Flow**:
1. Query InfluxDB for latest metrics
2. Aggregate and format data
3. Broadcast to all connected clients
4. Handle client subscriptions and disconnections

### 10. **React Dashboard**

**Location**: `react-dashboard/src/`

**Purpose**: Real-time analytics visualization and user interface.

**Key Components**:

#### `App.js` - Main Application
```javascript
// WebSocket connection management
const { metrics, isConnected, connectionStatus } = useWebSocket();

// Real-time metrics display
const metricCards = [
  { title: 'Page Views/sec', value: metrics.pageViewsPerSecond },
  { title: 'Active Users', value: metrics.activeUsers },
  { title: 'Conversion Rate', value: metrics.conversionRate },
  { title: 'Avg Session Duration', value: metrics.averageSessionDuration }
];
```

#### `useWebSocket.js` - WebSocket Hook
```javascript
// Custom hook for WebSocket management
export function useWebSocket() {
  // Connection management with auto-reconnection
  // Message handling and parsing
  // Error handling and status reporting
}
```

#### `MetricCard.js` - Metric Display
```javascript
// Animated metric cards with real-time updates
// Performance indicators and status
// Responsive design for different screen sizes
```

#### `RealTimeChart.js` - Data Visualization
```javascript
// Real-time chart updates using Recharts
// Multiple metric visualization
// Historical data display
```

**Dashboard Features**:
- **Real-Time Updates**: WebSocket-based live data
- **Interactive Charts**: Time-series visualization
- **Performance Metrics**: Pipeline health monitoring
- **Alert Management**: CEP alerts and notifications
- **Responsive Design**: Works on desktop and mobile

## 📊 **Monitoring Components**

### 11. **Prometheus**

**Location**: `monitoring/prometheus.yml`

**Purpose**: Metrics collection and storage for monitoring.

**Key Configuration**:
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-1:9092', 'kafka-2:9092', 'kafka-3:9092']
  - job_name: 'flink'
    static_configs:
      - targets: ['flink-taskmanager:9249']
```

**Collected Metrics**:
- **System Metrics**: CPU, memory, disk usage
- **Application Metrics**: Throughput, latency, error rates
- **Business Metrics**: Events processed, conversions, active users

### 12. **Grafana**

**Location**: `monitoring/grafana/`

**Purpose**: Metrics visualization and dashboard creation.

**Key Features**:
- **Analytics Pipeline Dashboard**: Real-time pipeline monitoring
- **System Overview Dashboard**: Infrastructure health
- **Custom Alerts**: Threshold-based notifications
- **Performance Tracking**: Historical trend analysis

## 🔧 **Development Tools**

### 13. **Kafka UI**

**Location**: `docker-compose.yml` (kafka-ui)

**Purpose**: Web-based Kafka cluster management interface.

**Features**:
- Topic browsing and management
- Message inspection
- Consumer group monitoring
- Broker health status

### 14. **Makefile**

**Location**: `Makefile`

**Purpose**: Development workflow automation.

**Key Commands**:
```bash
make start          # Start entire pipeline
make stop           # Stop all services
make logs           # View service logs
make test           # Run performance tests
make health         # Check service health
```

### 15. **Scripts**

**Location**: `scripts/`

**Purpose**: Automation and operational tasks.

#### `start-pipeline.sh`
- Orchestrated service startup
- Health checks and validation
- Service dependency management
- Performance verification

#### `setup-kafka-topics.sh`
- Kafka topic creation with optimal configuration
- Partition and replication setup
- Schema registry integration
- Topic validation

#### `performance-test.sh`
- Comprehensive performance testing
- Latency and throughput validation
- Resource usage monitoring
- Performance regression detection

## 🔄 **Component Interactions**

### Data Flow Sequence
```
1. Event Simulator → Kafka Topics
2. Kafka Topics → Flink Processing
3. Flink Processing → InfluxDB Storage
4. InfluxDB Storage → WebSocket Server
5. WebSocket Server → React Dashboard
6. All Components → Prometheus Metrics
7. Prometheus Metrics → Grafana Dashboards
```

### Service Dependencies
```
Zookeeper → Kafka Cluster
Kafka Cluster → Schema Registry
Kafka Cluster → Flink Jobs
Flink Jobs → InfluxDB
InfluxDB → WebSocket Server
WebSocket Server → React Dashboard
All Services → Prometheus → Grafana
```

## 🛠️ **Configuration Management**

### Environment Variables
```bash
# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=kafka-1:29092,kafka-2:29092,kafka-3:29092

# InfluxDB Configuration
INFLUXDB_URL=http://influxdb:8086
INFLUXDB_TOKEN=analytics-token-123

# WebSocket Configuration
WEBSOCKET_PORT=8082

# Performance Tuning
EVENT_RATE_PER_SECOND=200
SIMULATION_DURATION_MINUTES=60
```

### Docker Compose Configuration
- Service orchestration and dependencies
- Network configuration and service discovery
- Volume management for data persistence
- Resource limits and health checks

This component guide provides a comprehensive understanding of each part of the system. Each component is designed to be independently deployable while working together to create a cohesive, high-performance streaming analytics platform.
