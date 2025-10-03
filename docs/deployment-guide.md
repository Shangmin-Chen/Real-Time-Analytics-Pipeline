# Real-Time Analytics Pipeline - Deployment Guide

## Quick Start

### Prerequisites

- **Docker & Docker Compose**: Latest versions installed
- **System Resources**: 
  - Minimum 8GB RAM (16GB+ recommended)
  - 10GB+ free disk space
  - Multi-core CPU (4+ cores recommended)
- **Network**: Ports 3000, 8080-8086, 9090-9094 available

### One-Command Deployment

```bash
# Clone and start the entire pipeline
git clone <repository-url>
cd Real-Time-Analytics-Pipeline
./scripts/start-pipeline.sh
```

The script will:
1. ✅ Verify system requirements
2. ✅ Start infrastructure (Kafka, InfluxDB, Flink)
3. ✅ Create optimized Kafka topics
4. ✅ Deploy processing components
5. ✅ Launch monitoring stack
6. ✅ Start applications (Dashboard, Event Simulator)

### Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| **React Dashboard** | http://localhost:3000 | Real-time analytics visualization |
| **Kafka UI** | http://localhost:8080 | Kafka cluster management |
| **Flink Dashboard** | http://localhost:8081 | Stream processing monitoring |
| **InfluxDB UI** | http://localhost:8086 | Time-series data browser |
| **Grafana** | http://localhost:3001 | Advanced monitoring dashboards |
| **Prometheus** | http://localhost:9090 | Metrics collection |

## Architecture Overview

### Data Flow
```
Event Simulator → Kafka Topics → Flink Processing → InfluxDB → WebSocket → React Dashboard
                      ↓
                CEP Patterns → Alert Topics → Monitoring
```

### Performance Characteristics
- **Latency**: <500ms end-to-end
- **Throughput**: 10K+ events/minute
- **Fault Tolerance**: Exactly-once processing
- **Scalability**: Horizontal scaling ready

## Detailed Configuration

### Kafka Configuration

**Cluster Setup**:
- 3 Kafka brokers for high availability
- 12 partitions per topic for parallel processing
- Replication factor 3 for fault tolerance
- Snappy compression for efficiency

**Topic Structure**:
```bash
# Raw events (12 partitions)
website-analytics-events

# Processed metrics (6 partitions, compacted)
processed-metrics

# CEP alerts (3 partitions)
cep-alerts
```

### Flink Configuration

**Processing Settings**:
- Parallelism: 12 tasks
- Checkpoint interval: 10 seconds
- State backend: RocksDB (persistent)
- Exactly-once semantics enabled

**Window Types**:
- **Real-time**: 5-second tumbling windows
- **Business**: 1-minute sliding windows
- **Session**: Event-time session windows

### InfluxDB Configuration

**Storage Settings**:
- Retention: 30 days (configurable)
- Shard duration: 24 hours
- Compression: enabled
- Cache: 1GB memory

## Monitoring & Observability

### Key Metrics

**Pipeline Performance**:
- End-to-end latency (P50, P95, P99)
- Events processed per minute
- Error rates and failure counts
- Resource utilization (CPU, memory, disk)

**Business Metrics**:
- Page views per second
- Active users count
- Conversion rates
- Session durations

**System Health**:
- Service availability
- Kafka consumer lag
- Flink checkpoint success rate
- InfluxDB write performance

### Alerting Rules

**Critical Alerts**:
- Latency >500ms for >2 minutes
- Error rate >1% for >2 minutes
- Service down for >1 minute

**Warning Alerts**:
- Throughput <8K events/min for >5 minutes
- High resource usage (>80%)
- Consumer lag >10K messages

## Performance Optimization

### Latency Optimization

1. **Kafka Settings**:
   ```properties
   linger.ms=5
   batch.size=16384
   compression.type=snappy
   ```

2. **Flink Settings**:
   ```properties
   taskmanager.memory.process.size=4096m
   parallelism.default=12
   checkpointing.interval=10000
   ```

3. **InfluxDB Settings**:
   ```properties
   cache-max-memory-size=1073741824
   batch-size=1000
   ```

### Throughput Optimization

1. **Parallelism**: 12 Flink tasks across 3 TaskManagers
2. **Batching**: Optimized batch sizes for all components
3. **Compression**: Snappy compression throughout pipeline
4. **Resource Allocation**: Tuned memory and CPU limits

## Troubleshooting

### Common Issues

**High Latency**:
```bash
# Check Flink metrics
curl http://localhost:8081/overview

# Monitor Kafka consumer lag
docker-compose exec kafka-1 kafka-consumer-groups.sh --bootstrap-server kafka-1:29092 --describe --all-groups
```

**Low Throughput**:
```bash
# Check event simulator logs
docker-compose logs -f event-simulator

# Monitor Kafka topic metrics
curl http://localhost:9090/api/v1/query?query=kafka_topic_partitions
```

**Memory Issues**:
```bash
# Check system resources
docker stats

# Adjust memory limits in docker-compose.yml
```

### Performance Testing

```bash
# Run comprehensive performance test
./scripts/performance-test.sh

# Expected results:
# ✅ Throughput: >10K events/minute
# ✅ Latency: <500ms P95
# ✅ Error Rate: <1%
```

## Scaling Guidelines

### Horizontal Scaling

**Kafka**:
- Add more brokers to cluster
- Increase partition count for topics
- Adjust replication factor

**Flink**:
- Add TaskManager instances
- Increase parallelism per job
- Scale based on checkpoint performance

**InfluxDB**:
- Use clustering for high availability
- Implement data sharding
- Add read replicas

### Vertical Scaling

**Resource Tuning**:
```yaml
# Increase memory limits
flink-taskmanager:
  environment:
    - taskmanager.memory.process.size=8192m

# Increase Kafka heap
kafka-1:
  environment:
    - KAFKA_HEAP_OPTS=-Xmx4g -Xms4g
```

## Security Considerations

### Network Security
- Use internal Docker networks
- Expose only necessary ports
- Implement firewall rules

### Data Security
- Enable Kafka SASL/SSL for production
- Use InfluxDB authentication
- Implement API rate limiting

### Monitoring Security
- Secure Grafana with authentication
- Protect Prometheus endpoints
- Audit log access

## Production Deployment

### Environment Variables
```bash
# Production settings
export KAFKA_HEAP_OPTS="-Xmx4g -Xms4g"
export FLINK_MEMORY="8192m"
export INFLUXDB_CACHE_SIZE="2g"
export EVENT_RATE_PER_SECOND="500"
```

### Health Checks
```bash
# Pipeline health endpoint
curl http://localhost:8082/health

# Service status
docker-compose ps
```

### Backup Strategy
- Kafka topic replication
- InfluxDB data snapshots
- Flink savepoint backups
- Configuration version control

## Maintenance

### Regular Tasks
- Monitor disk usage and cleanup old data
- Update container images monthly
- Review and tune performance metrics
- Test disaster recovery procedures

### Updates
```bash
# Update all services
docker-compose pull
docker-compose up -d

# Rolling updates for zero downtime
docker-compose up -d --no-deps service-name
```

This deployment guide provides everything needed to run a production-ready real-time analytics pipeline with sub-500ms latency and 10K+ events/minute throughput.
