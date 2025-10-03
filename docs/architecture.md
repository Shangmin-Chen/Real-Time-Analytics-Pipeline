# Streaming Architecture Design

## Kafka Topic Strategy

### Topic Design Principles

1. **Domain-Driven Partitioning**: Separate topics by event domain
2. **Temporal Partitioning**: Time-based data organization
3. **Throughput Optimization**: Parallel processing capabilities
4. **Retention Policies**: Balanced storage and performance

### Core Topics

#### 1. Raw Events Topic
```
Topic: website-analytics-events
Partitions: 12 (for 10K+ events/min with 3x replication)
Replication Factor: 3
Retention: 7 days
Compaction: Disabled (append-only)
```

**Partitioning Strategy**: `user_id.hashCode() % 12`
- Ensures user session ordering
- Enables parallel processing
- Maintains data locality

#### 2. Processed Metrics Topic
```
Topic: processed-metrics
Partitions: 6
Replication Factor: 3
Retention: 1 day
Compaction: Enabled
```

#### 3. CEP Alerts Topic
```
Topic: cep-alerts
Partitions: 3
Replication Factor: 3
Retention: 3 days
Compaction: Disabled
```

### Topic Schema Design

#### Event Types
1. **PageViewEvent**: Page navigation tracking
2. **SessionEvent**: User session lifecycle
3. **ConversionEvent**: Business goal completions
4. **CustomEvent**: Extensible event framework

#### Partitioning Logic
```java
// PageView events: user_id based
int partition = Math.abs(userId.hashCode()) % partitions;

// Session events: session_id based  
int partition = Math.abs(sessionId.hashCode()) % partitions;

// Conversion events: user_id based (for user journey tracking)
int partition = Math.abs(userId.hashCode()) % partitions;
```

## Flink Processing Architecture

### Stream Processing Topology

```
Raw Events ──┐
             ├─► Flink Job ──► Windowed Aggregations ──► InfluxDB
CEP Events ──┘                │
                              └─► CEP Patterns ──► Alert Topics
```

### Windowed Aggregations

#### 1. Real-time Metrics (5-second windows)
- Page views per second
- Active users
- Bounce rate
- Average session duration

#### 2. Business Metrics (1-minute windows)
- Conversion rates
- Revenue per user
- Popular content
- Geographic distribution

#### 3. Session Analytics (Session windows)
- Complete user journeys
- Multi-page conversion funnels
- User behavior patterns

### Complex Event Processing (CEP)

#### Pattern Detection
1. **High Bounce Rate**: >80% single-page sessions
2. **Conversion Funnel**: Multi-step conversion tracking
3. **Anomaly Detection**: Unusual traffic patterns
4. **Fraud Detection**: Suspicious user behavior

#### CEP Pattern Examples
```sql
-- High-value user detection
PATTERN (login A) -> (view_product B) -> (add_to_cart C) -> (checkout D)
WHERE A.user_type = 'premium' AND B.product_category = 'electronics'
WITHIN 10 MINUTES

-- Session abandonment alert
PATTERN (session_start A) -> (page_view B) -> (session_timeout C)
WHERE B.page_depth = 1 AND C.idle_time > 5 MINUTES
WITHIN 15 MINUTES
```

## Fault Tolerance & Exactly-Once Processing

### Kafka Transactions
- **Producer Transactions**: Atomic multi-topic writes
- **Consumer Transactions**: Exactly-once processing
- **Transaction Coordinator**: Manages transaction state

### Flink Checkpointing
- **Checkpoint Interval**: 10 seconds
- **State Backend**: RocksDB (persistent)
- **Checkpoint Storage**: Distributed filesystem
- **Recovery Time**: <30 seconds

### Failure Recovery
1. **Kafka Broker Failure**: Automatic failover
2. **Flink Task Failure**: State recovery from checkpoints
3. **InfluxDB Failure**: Buffered writes with retry
4. **Network Partitions**: Circuit breaker patterns

## Performance Optimization

### Latency Targets
- **Kafka Produce**: <10ms
- **Flink Processing**: <100ms per window
- **InfluxDB Write**: <50ms
- **WebSocket Update**: <50ms
- **Total Pipeline**: <500ms

### Throughput Optimization
- **Parallel Processing**: 12 Flink tasks per job
- **Batch Writes**: InfluxDB batch size 1000
- **Compression**: Snappy compression for Kafka
- **Memory Tuning**: Optimized heap sizes

### Resource Allocation
```yaml
Kafka:
  Heap: 2GB per broker
  Direct Memory: 1GB
  
Flink:
  TaskManager Memory: 4GB
  Parallelism: 12
  
InfluxDB:
  Memory: 2GB
  Cache: 1GB
```

## Data Flow Architecture

### Event Producers
- **Simulation Service**: Generates realistic website events
- **Load Testing**: Validates performance under load
- **Schema Validation**: Ensures data quality

### Stream Processing Pipeline
1. **Event Ingestion**: Kafka consumer groups
2. **Schema Validation**: Avro deserialization
3. **Windowed Processing**: Time-based aggregations
4. **CEP Analysis**: Pattern detection
5. **Result Publishing**: Multi-sink writes

### Storage & Serving
- **InfluxDB**: Time-series metrics storage
- **WebSocket Server**: Real-time data streaming
- **React Dashboard**: Live visualization
- **API Gateway**: RESTful data access

## Monitoring & Observability

### Key Metrics
- **Throughput**: Events/second, records/minute
- **Latency**: P50, P95, P99 processing times
- **Error Rates**: Failed records, retries
- **Resource Usage**: CPU, memory, disk I/O

### Alerting Thresholds
- **Latency >500ms**: Critical alert
- **Error Rate >1%**: Warning alert
- **Throughput <8K/min**: Performance alert
- **Resource Usage >80%**: Capacity alert

### Dashboard Components
- **Pipeline Health**: Overall system status
- **Real-time Metrics**: Live event processing
- **Performance Trends**: Historical analysis
- **Alert Management**: Active notifications
