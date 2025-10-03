#!/bin/bash

# Performance testing script for analytics pipeline
# Tests latency and throughput requirements

set -e

echo "Starting performance tests for analytics pipeline..."
echo "Targets: <500ms latency, >10K events/minute throughput"
echo ""

# Configuration
KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
TEST_DURATION=300  # 5 minutes
EVENT_RATE=200     # events per second
EXPECTED_THROUGHPUT=12000  # events per minute
MAX_LATENCY=500    # milliseconds

# Function to check if services are ready
check_service() {
    local service_name=$1
    local url=$2
    
    echo "Checking $service_name..."
    for i in {1..30}; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo "$service_name is ready!"
            return 0
        fi
        echo "Waiting for $service_name... ($i/30)"
        sleep 10
    done
    echo "ERROR: $service_name is not ready after 5 minutes"
    return 1
}

# Check all services
echo "Checking service availability..."
check_service "Kafka" "http://localhost:8080" || exit 1
check_service "Flink" "http://localhost:8081" || exit 1
check_service "InfluxDB" "http://localhost:8086" || exit 1
check_service "WebSocket Server" "http://localhost:8082" || exit 1
check_service "React Dashboard" "http://localhost:3000" || exit 1

echo ""
echo "All services are ready! Starting performance tests..."
echo ""

# Test 1: Event Generation Rate
echo "Test 1: Event Generation Rate"
echo "Generating $EVENT_RATE events/second for $((TEST_DURATION/60)) minutes..."

# Start event simulator with high rate
docker-compose exec -d event-simulator java -jar /app/target/event-simulator-1.0.0.jar

# Wait for test duration
sleep $TEST_DURATION

# Check throughput
echo "Checking throughput..."
THROUGHPUT=$(curl -s "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_events_total[1m])*60" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
THROUGHPUT_INT=$(echo "$THROUGHPUT" | cut -d. -f1)

echo "Measured throughput: $THROUGHPUT_INT events/minute"
if [ "$THROUGHPUT_INT" -ge "$EXPECTED_THROUGHPUT" ]; then
    echo "✅ THROUGHPUT TEST PASSED: $THROUGHPUT_INT >= $EXPECTED_THROUGHPUT"
else
    echo "❌ THROUGHPUT TEST FAILED: $THROUGHPUT_INT < $EXPECTED_THROUGHPUT"
fi

# Test 2: End-to-End Latency
echo ""
echo "Test 2: End-to-End Latency"
LATENCY_P95=$(curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(analytics_pipeline_latency_seconds_bucket[5m]))" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "1")
LATENCY_MS=$(echo "$LATENCY_P95 * 1000" | bc)

echo "Measured 95th percentile latency: ${LATENCY_MS}ms"
if (( $(echo "$LATENCY_MS < $MAX_LATENCY" | bc -l) )); then
    echo "✅ LATENCY TEST PASSED: ${LATENCY_MS}ms < ${MAX_LATENCY}ms"
else
    echo "❌ LATENCY TEST FAILED: ${LATENCY_MS}ms >= ${MAX_LATENCY}ms"
fi

# Test 3: Error Rate
echo ""
echo "Test 3: Error Rate"
ERROR_RATE=$(curl -s "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_errors_total[5m])/rate(analytics_pipeline_events_total[5m])*100" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
ERROR_RATE_PERCENT=$(echo "$ERROR_RATE" | cut -d. -f1)

echo "Measured error rate: ${ERROR_RATE_PERCENT}%"
if [ "$ERROR_RATE_PERCENT" -le "1" ]; then
    echo "✅ ERROR RATE TEST PASSED: ${ERROR_RATE_PERCENT}% <= 1%"
else
    echo "❌ ERROR RATE TEST FAILED: ${ERROR_RATE_PERCENT}% > 1%"
fi

# Test 4: System Resource Usage
echo ""
echo "Test 4: System Resource Usage"

# Check memory usage
MEMORY_USAGE=$(curl -s "http://localhost:9090/api/v1/query?query=(1-(node_memory_MemAvailable_bytes/node_memory_MemTotal_bytes))*100" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
MEMORY_PERCENT=$(echo "$MEMORY_USAGE" | cut -d. -f1)

echo "Memory usage: ${MEMORY_PERCENT}%"
if [ "$MEMORY_PERCENT" -le "80" ]; then
    echo "✅ MEMORY USAGE OK: ${MEMORY_PERCENT}% <= 80%"
else
    echo "⚠️  MEMORY USAGE HIGH: ${MEMORY_PERCENT}% > 80%"
fi

# Check CPU usage
CPU_USAGE=$(curl -s "http://localhost:9090/api/v1/query?query=100-(avg(rate(node_cpu_seconds_total{mode=\"idle\"}[5m]))*100)" | jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
CPU_PERCENT=$(echo "$CPU_USAGE" | cut -d. -f1)

echo "CPU usage: ${CPU_PERCENT}%"
if [ "$CPU_PERCENT" -le "80" ]; then
    echo "✅ CPU USAGE OK: ${CPU_PERCENT}% <= 80%"
else
    echo "⚠️  CPU USAGE HIGH: ${CPU_PERCENT}% > 80%"
fi

# Test 5: WebSocket Connection Performance
echo ""
echo "Test 5: WebSocket Connection Performance"

# Test WebSocket connection time
WEBSOCKET_RESPONSE_TIME=$(curl -w "%{time_total}" -s -o /dev/null "http://localhost:8082" || echo "1")
WEBSOCKET_MS=$(echo "$WEBSOCKET_RESPONSE_TIME * 1000" | bc | cut -d. -f1)

echo "WebSocket response time: ${WEBSOCKET_MS}ms"
if [ "$WEBSOCKET_MS" -le "100" ]; then
    echo "✅ WEBSOCKET RESPONSE OK: ${WEBSOCKET_MS}ms <= 100ms"
else
    echo "⚠️  WEBSOCKET RESPONSE SLOW: ${WEBSOCKET_MS}ms > 100ms"
fi

# Summary
echo ""
echo "=========================================="
echo "PERFORMANCE TEST SUMMARY"
echo "=========================================="
echo "Throughput: $THROUGHPUT_INT events/minute (target: >$EXPECTED_THROUGHPUT)"
echo "Latency (P95): ${LATENCY_MS}ms (target: <$MAX_LATENCY ms)"
echo "Error Rate: ${ERROR_RATE_PERCENT}% (target: <1%)"
echo "Memory Usage: ${MEMORY_PERCENT}% (target: <80%)"
echo "CPU Usage: ${CPU_PERCENT}% (target: <80%)"
echo "WebSocket Response: ${WEBSOCKET_MS}ms (target: <100ms)"
echo ""

# Determine overall result
if [ "$THROUGHPUT_INT" -ge "$EXPECTED_THROUGHPUT" ] && (( $(echo "$LATENCY_MS < $MAX_LATENCY" | bc -l) )) && [ "$ERROR_RATE_PERCENT" -le "1" ]; then
    echo "🎉 OVERALL RESULT: ALL TESTS PASSED!"
    echo "The analytics pipeline meets all performance requirements."
    exit 0
else
    echo "❌ OVERALL RESULT: SOME TESTS FAILED!"
    echo "The analytics pipeline needs optimization."
    exit 1
fi
