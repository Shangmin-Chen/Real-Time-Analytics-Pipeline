# Troubleshooting Guide

This comprehensive troubleshooting guide helps diagnose and resolve common issues with the Real-Time Analytics Pipeline. Each section provides step-by-step solutions with detailed explanations.

## 🚨 **Emergency Response Procedures**

### Pipeline Down - Immediate Actions

#### 1. **Quick Health Check**
```bash
# Check all services status
make health

# Expected output should show all services as "200 ✅"
# If any service shows "❌", proceed with specific troubleshooting
```

#### 2. **Check Service Logs**
```bash
# View logs for all services
make logs

# View logs for specific service
docker-compose logs -f kafka-1
docker-compose logs -f flink-jobmanager
docker-compose logs -f influxdb
```

#### 3. **Restart Failed Services**
```bash
# Restart specific service
docker-compose restart kafka-1

# Restart all services
make restart

# Force restart with cleanup
docker-compose down -v
make start
```

## 🔍 **Service-Specific Troubleshooting**

### Kafka Issues

#### Problem: Kafka Broker Not Starting

**Symptoms**:
- Broker fails to start in logs
- Connection refused errors
- Zookeeper connection issues

**Diagnosis**:
```bash
# Check Kafka logs
docker-compose logs kafka-1

# Check Zookeeper status
docker-compose logs zookeeper

# Check port availability
lsof -i :9092
lsof -i :2181
```

**Common Causes & Solutions**:

1. **Port Conflicts**:
```bash
# Kill processes using Kafka ports
sudo kill -9 $(lsof -t -i:9092)
sudo kill -9 $(lsof -t -i:2181)

# Restart Kafka
docker-compose restart kafka-1
```

2. **Zookeeper Connection Issues**:
```bash
# Check Zookeeper health
docker-compose exec zookeeper zkCli.sh -server localhost:2181

# Restart Zookeeper first, then Kafka
docker-compose restart zookeeper
sleep 30
docker-compose restart kafka-1
```

3. **Disk Space Issues**:
```bash
# Check disk space
df -h

# Clean Docker volumes
docker system prune -a -f
docker volume prune -f

# Restart services
docker-compose down -v
make start
```

#### Problem: Kafka Consumer Lag

**Symptoms**:
- High consumer lag in monitoring
- Slow data processing
- Backlog in topics

**Diagnosis**:
```bash
# Check consumer groups
docker-compose exec kafka-1 kafka-consumer-groups.sh \
  --bootstrap-server kafka-1:29092 --list

# Check consumer lag
docker-compose exec kafka-1 kafka-consumer-groups.sh \
  --bootstrap-server kafka-1:29092 \
  --group analytics-processing-group --describe
```

**Solutions**:

1. **Scale Flink TaskManagers**:
```bash
# Scale TaskManagers
make scale-flink

# Or manually:
docker-compose up -d --scale flink-taskmanager=3
```

2. **Increase Kafka Partitions**:
```bash
# Add partitions to topic
docker-compose exec kafka-1 kafka-topics.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --alter --partitions 16
```

3. **Optimize Consumer Settings**:
```bash
# Check Flink consumer configuration
# In docker-compose.yml, add:
environment:
  - KAFKA_CONSUMER_FETCH_MIN_BYTES=1048576
  - KAFKA_CONSUMER_FETCH_MAX_WAIT_MS=500
```

### Flink Issues

#### Problem: Flink Job Fails to Start

**Symptoms**:
- Job appears in FAILED state
- Checkpoint failures
- TaskManager connection issues

**Diagnosis**:
```bash
# Check Flink dashboard
open http://localhost:8081

# Check JobManager logs
docker-compose logs flink-jobmanager

# Check TaskManager logs
docker-compose logs flink-taskmanager
```

**Common Solutions**:

1. **Memory Issues**:
```bash
# Increase TaskManager memory
# In docker-compose.yml:
environment:
  - taskmanager.memory.process.size=6144m

# Restart Flink
docker-compose restart flink-jobmanager flink-taskmanager
```

2. **Checkpoint Issues**:
```bash
# Clear checkpoint data
docker-compose exec flink-jobmanager rm -rf /opt/flink/checkpoints/*

# Restart Flink cluster
docker-compose restart flink-jobmanager flink-taskmanager
```

3. **Classpath Issues**:
```bash
# Rebuild Flink jobs
make build-flink

# Redeploy job
docker-compose exec flink-jobmanager flink run \
  /opt/flink/jobs/flink-streaming-jobs-1.0.0.jar
```

#### Problem: Flink Processing Latency

**Symptoms**:
- High processing latency
- Slow window computations
- Backpressure indicators

**Diagnosis**:
```bash
# Check Flink metrics
curl http://localhost:8081/jobs

# Check backpressure
# In Flink dashboard, go to Running Jobs → Job Details → Backpressure
```

**Solutions**:

1. **Increase Parallelism**:
```bash
# In docker-compose.yml:
environment:
  - parallelism.default=4
  - taskmanager.numberOfTaskSlots=8
```

2. **Optimize Checkpointing**:
```bash
# In docker-compose.yml:
environment:
  - state.checkpoints.interval=30000
  - state.checkpoints.min-pause=10000
```

3. **Tune Memory Settings**:
```bash
# In docker-compose.yml:
environment:
  - taskmanager.memory.managed.fraction=0.6
  - taskmanager.memory.network.fraction=0.2
```

### InfluxDB Issues

#### Problem: InfluxDB Connection Refused

**Symptoms**:
- Connection refused errors
- WebSocket server can't connect
- Data not being written

**Diagnosis**:
```bash
# Check InfluxDB status
docker-compose logs influxdb

# Check InfluxDB health
curl http://localhost:8086/health

# Check port availability
lsof -i :8086
```

**Solutions**:

1. **Restart InfluxDB**:
```bash
# Restart InfluxDB
docker-compose restart influxdb

# Wait for initialization
sleep 30
curl http://localhost:8086/health
```

2. **Reset InfluxDB Data**:
```bash
# Stop InfluxDB
docker-compose stop influxdb

# Remove data volume
docker volume rm real-time-analytics-pipeline_influxdb-data

# Restart InfluxDB
docker-compose start influxdb
```

3. **Check Memory Usage**:
```bash
# Check InfluxDB memory usage
docker stats influxdb

# Increase memory if needed
# In docker-compose.yml:
environment:
  - INFLUXDB_DATA_CACHE_MAX_MEMORY_SIZE=2147483648  # 2GB
```

#### Problem: Slow InfluxDB Writes

**Symptoms**:
- High write latency
- Write timeouts
- Data loss

**Diagnosis**:
```bash
# Check InfluxDB metrics
curl http://localhost:8086/metrics

# Check disk I/O
docker exec influxdb iostat -x 1 5
```

**Solutions**:

1. **Optimize Batch Size**:
```bash
# In Flink sink configuration:
# Increase batch size to 2000
batch-size: 2000
batch-timeout: 2s
```

2. **Increase Memory Cache**:
```bash
# In docker-compose.yml:
environment:
  - INFLUXDB_DATA_CACHE_MAX_MEMORY_SIZE=2147483648
  - INFLUXDB_DATA_CACHE_SNAPSHOT_MEMORY_SIZE=536870912
```

3. **Optimize Index**:
```bash
# In docker-compose.yml:
environment:
  - INFLUXDB_DATA_INDEX_VERSION=tsi1
  - INFLUXDB_DATA_INDEX_MAX_SERIES_PER_DATABASE=1000000
```

### WebSocket Server Issues

#### Problem: WebSocket Connection Failures

**Symptoms**:
- Dashboard not updating
- Connection errors in browser console
- WebSocket server not responding

**Diagnosis**:
```bash
# Check WebSocket server logs
docker-compose logs websocket-server

# Test WebSocket connection
curl -i -N -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: test" \
  -H "Sec-WebSocket-Version: 13" \
  http://localhost:8082
```

**Solutions**:

1. **Restart WebSocket Server**:
```bash
# Restart WebSocket server
docker-compose restart websocket-server

# Check connection
curl http://localhost:8082/health
```

2. **Check InfluxDB Connection**:
```bash
# Verify InfluxDB connectivity from WebSocket server
docker-compose exec websocket-server curl http://influxdb:8086/health
```

3. **Increase Connection Pool**:
```bash
# In WebSocket server configuration:
# Increase connection pool size
max-connections: 1000
connection-timeout: 30s
```

### React Dashboard Issues

#### Problem: Dashboard Not Loading

**Symptoms**:
- Blank page in browser
- JavaScript errors
- WebSocket connection failures

**Diagnosis**:
```bash
# Check dashboard logs
docker-compose logs react-dashboard

# Check if dashboard is serving files
curl http://localhost:3000

# Check browser console for errors
```

**Solutions**:

1. **Rebuild Dashboard**:
```bash
# Rebuild React dashboard
make build-dashboard

# Restart dashboard container
docker-compose restart react-dashboard
```

2. **Check WebSocket Connection**:
```bash
# Verify WebSocket server is accessible
curl http://localhost:8082

# Check environment variables
docker-compose exec react-dashboard env | grep REACT_APP
```

3. **Clear Browser Cache**:
```bash
# In browser:
# 1. Open Developer Tools (F12)
# 2. Right-click refresh button
# 3. Select "Empty Cache and Hard Reload"
```

## 📊 **Performance Issues**

### High Latency

#### Problem: End-to-End Latency >500ms

**Diagnosis**:
```bash
# Check latency metrics
curl "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(analytics_pipeline_latency_seconds_bucket[5m]))"

# Check each component latency
# Kafka produce latency
curl "http://localhost:9090/api/v1/query?query=rate(kafka_producer_request_latency_avg[5m])"

# Flink processing latency
curl "http://localhost:9090/api/v1/query?query=flink_taskmanager_job_latency_source_id"
```

**Solutions**:

1. **Optimize Kafka Settings**:
```bash
# In docker-compose.yml:
environment:
  - KAFKA_BATCH_SIZE=32768
  - KAFKA_LINGER_MS=2
  - KAFKA_COMPRESSION_TYPE=snappy
```

2. **Tune Flink Processing**:
```bash
# In docker-compose.yml:
environment:
  - taskmanager.memory.process.size=6144m
  - parallelism.default=4
  - state.checkpoints.interval=5000
```

3. **Optimize InfluxDB Writes**:
```bash
# In docker-compose.yml:
environment:
  - INFLUXDB_DATA_CACHE_MAX_MEMORY_SIZE=2147483648
  - INFLUXDB_DATA_WAL_FSYNC_DELAY=0s
```

### Low Throughput

#### Problem: Throughput <10K events/minute

**Diagnosis**:
```bash
# Check throughput metrics
curl "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_events_total[1m])*60"

# Check Kafka throughput
curl "http://localhost:9090/api/v1/query?query=rate(kafka_server_brokertopicmetrics_messagesin_total[1m])"

# Check Flink throughput
curl "http://localhost:9090/api/v1/query?query=flink_taskmanager_job_num_records_in_total"
```

**Solutions**:

1. **Scale Event Simulator**:
```bash
# Increase event generation rate
# In docker-compose.yml:
environment:
  - EVENT_RATE_PER_SECOND=500

# Scale simulator instances
make scale-simulator
```

2. **Increase Kafka Partitions**:
```bash
# Add partitions to topics
docker-compose exec kafka-1 kafka-topics.sh \
  --bootstrap-server kafka-1:29092 \
  --topic website-analytics-events \
  --alter --partitions 16
```

3. **Scale Flink Processing**:
```bash
# Scale TaskManagers
make scale-flink

# Increase parallelism
# In docker-compose.yml:
environment:
  - parallelism.default=6
  - taskmanager.numberOfTaskSlots=6
```

### High Error Rates

#### Problem: Error Rate >1%

**Diagnosis**:
```bash
# Check error metrics
curl "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_errors_total[5m])/rate(analytics_pipeline_events_total[5m])*100"

# Check specific error types
curl "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_errors_total[5m])"

# Check component error rates
curl "http://localhost:9090/api/v1/query?query=rate(kafka_consumer_consumer_fetch_manager_records_consumed_total[5m])"
```

**Solutions**:

1. **Add Error Handling**:
```java
// In Flink jobs, add try-catch blocks
public class ResilientEventProcessor implements ProcessFunction<AnalyticsEvent, ProcessedMetric> {
    @Override
    public void processElement(AnalyticsEvent event, Context ctx, Collector<ProcessedMetric> out) {
        try {
            ProcessedMetric metric = processEvent(event);
            out.collect(metric);
        } catch (Exception e) {
            log.error("Failed to process event: {}", event.getEventId(), e);
            // Send to dead letter queue or retry
        }
    }
}
```

2. **Implement Retry Logic**:
```java
// Add retry mechanism for external calls
public class RetryableInfluxDBSink implements SinkFunction<ProcessedMetric> {
    private final RetryPolicy retryPolicy = RetryPolicy.builder()
        .maxAttempts(3)
        .delay(Duration.ofSeconds(1))
        .build();
    
    @Override
    public void invoke(ProcessedMetric metric, Context context) {
        retryPolicy.execute(() -> writeToInfluxDB(metric));
    }
}
```

3. **Add Circuit Breaker**:
```java
// Implement circuit breaker pattern
public class CircuitBreakerInfluxDBSink implements SinkFunction<ProcessedMetric> {
    private final CircuitBreaker circuitBreaker = CircuitBreaker.builder()
        .failureThreshold(5)
        .timeout(Duration.ofSeconds(30))
        .build();
    
    @Override
    public void invoke(ProcessedMetric metric, Context context) {
        circuitBreaker.execute(() -> writeToInfluxDB(metric));
    }
}
```

## 🔧 **System Resource Issues**

### Memory Issues

#### Problem: Out of Memory Errors

**Diagnosis**:
```bash
# Check memory usage
docker stats

# Check JVM memory
docker-compose exec kafka-1 jstat -gc 1

# Check system memory
free -h
```

**Solutions**:

1. **Increase Container Memory**:
```bash
# In docker-compose.yml:
services:
  kafka-1:
    deploy:
      resources:
        limits:
          memory: 4G
        reservations:
          memory: 2G
```

2. **Optimize JVM Settings**:
```bash
# In docker-compose.yml:
environment:
  - KAFKA_HEAP_OPTS=-Xmx3g -Xms3g
  - KAFKA_JVM_PERFORMANCE_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=20
```

3. **Scale Services**:
```bash
# Scale services to distribute load
make scale-flink
make scale-simulator
```

### CPU Issues

#### Problem: High CPU Usage

**Diagnosis**:
```bash
# Check CPU usage
docker stats

# Check top processes
docker-compose exec kafka-1 top

# Check system CPU
htop
```

**Solutions**:

1. **Optimize Processing Logic**:
```java
// Use efficient data structures
private final Map<String, Long> eventCounts = new ConcurrentHashMap<>();

// Batch processing
public void processBatch(List<AnalyticsEvent> events) {
    events.parallelStream()
        .collect(groupingBy(AnalyticsEvent::getEventType))
        .forEach(this::processEventType);
}
```

2. **Tune JVM Settings**:
```bash
# In docker-compose.yml:
environment:
  - KAFKA_JVM_PERFORMANCE_OPTS=-XX:+UseParallelGC -XX:ParallelGCThreads=4
```

3. **Scale Services**:
```bash
# Scale based on CPU usage
docker-compose up -d --scale flink-taskmanager=4
```

### Disk Issues

#### Problem: Disk Space Full

**Diagnosis**:
```bash
# Check disk usage
df -h

# Check Docker disk usage
docker system df

# Check volume usage
docker volume ls
```

**Solutions**:

1. **Clean Docker Resources**:
```bash
# Clean unused containers and images
docker system prune -a -f

# Clean unused volumes
docker volume prune -f

# Clean build cache
docker builder prune -f
```

2. **Optimize Data Retention**:
```bash
# Reduce Kafka retention
# In docker-compose.yml:
environment:
  - KAFKA_LOG_RETENTION_HOURS=24

# Reduce InfluxDB retention
# In InfluxDB configuration:
retention-policy: 7d
```

3. **Add Disk Monitoring**:
```bash
# Add disk usage monitoring
# In prometheus.yml:
- job_name: 'node-exporter'
  static_configs:
    - targets: ['node-exporter:9100']
```

## 📋 **Common Error Messages & Solutions**

### Kafka Errors

#### `java.net.ConnectException: Connection refused`
```bash
# Solution: Check if Kafka is running
docker-compose ps kafka-1
docker-compose logs kafka-1

# Restart if needed
docker-compose restart kafka-1
```

#### `org.apache.kafka.common.errors.NotLeaderForPartitionException`
```bash
# Solution: Restart Kafka cluster
docker-compose restart kafka-1 kafka-2 kafka-3

# Wait for cluster to stabilize
sleep 60
```

#### `java.lang.OutOfMemoryError: Java heap space`
```bash
# Solution: Increase Kafka heap size
# In docker-compose.yml:
environment:
  - KAFKA_HEAP_OPTS=-Xmx4g -Xms4g
```

### Flink Errors

#### `org.apache.flink.runtime.JobException: Job failed`
```bash
# Solution: Check Flink logs
docker-compose logs flink-jobmanager
docker-compose logs flink-taskmanager

# Clear checkpoints and restart
docker-compose exec flink-jobmanager rm -rf /opt/flink/checkpoints/*
docker-compose restart flink-jobmanager flink-taskmanager
```

#### `java.lang.OutOfMemoryError: Metaspace`
```bash
# Solution: Increase metaspace size
# In docker-compose.yml:
environment:
  - taskmanager.memory.jvm-metaspace.size=512m
```

### InfluxDB Errors

#### `dial tcp: lookup influxdb: no such host`
```bash
# Solution: Check network connectivity
docker network ls
docker-compose exec websocket-server ping influxdb

# Restart services
docker-compose restart influxdb websocket-server
```

#### `write failed: "max series per database exceeded"`
```bash
# Solution: Increase series limit
# In docker-compose.yml:
environment:
  - INFLUXDB_DATA_MAX_SERIES_PER_DATABASE=1000000
```

## 🆘 **Getting Help**

### Log Collection for Support

```bash
# Collect all logs
mkdir -p troubleshooting-logs
docker-compose logs > troubleshooting-logs/all-services.log
docker-compose logs kafka-1 > troubleshooting-logs/kafka.log
docker-compose logs flink-jobmanager > troubleshooting-logs/flink-jobmanager.log
docker-compose logs flink-taskmanager > troubleshooting-logs/flink-taskmanager.log
docker-compose logs influxdb > troubleshooting-logs/influxdb.log
docker-compose logs websocket-server > troubleshooting-logs/websocket.log

# Collect system information
docker system info > troubleshooting-logs/docker-info.log
docker stats --no-stream > troubleshooting-logs/docker-stats.log
df -h > troubleshooting-logs/disk-usage.log
free -h > troubleshooting-logs/memory-usage.log
```

### Performance Data Collection

```bash
# Collect performance metrics
curl "http://localhost:9090/api/v1/query?query=up" > troubleshooting-logs/prometheus-up.json
curl "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_events_total[1m])" > troubleshooting-logs/throughput.json
curl "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(analytics_pipeline_latency_seconds_bucket[5m]))" > troubleshooting-logs/latency.json

# Collect Flink job information
curl http://localhost:8081/jobs > troubleshooting-logs/flink-jobs.json
curl http://localhost:8081/overview > troubleshooting-logs/flink-overview.json
```

This troubleshooting guide provides comprehensive solutions for common issues. For issues not covered here, collect the relevant logs and system information using the commands provided in the "Getting Help" section.
