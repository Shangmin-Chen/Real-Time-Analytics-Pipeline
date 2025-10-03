#!/bin/bash

# Kafka topic setup script for analytics pipeline
# Creates topics with optimal configuration for high throughput and low latency

set -e

KAFKA_BOOTSTRAP_SERVERS="localhost:9092,localhost:9093,localhost:9094"
REPLICATION_FACTOR=3
PARTITIONS=12

echo "Setting up Kafka topics for analytics pipeline..."

# Function to create topic if it doesn't exist
create_topic() {
    local topic_name=$1
    local partitions=${2:-$PARTITIONS}
    local replication=${3:-$REPLICATION_FACTOR}
    
    echo "Creating topic: $topic_name"
    
    kafka-topics.sh \
        --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
        --create \
        --if-not-exists \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication \
        --config cleanup.policy=delete \
        --config retention.ms=604800000 \
        --config segment.ms=3600000 \
        --config compression.type=snappy \
        --config min.insync.replicas=2 \
        --config max.message.bytes=1048576 \
        --config message.timestamp.type=CreateTime \
        --config message.timestamp.difference.max.ms=9223372036854775807
}

# Function to create compacted topic
create_compacted_topic() {
    local topic_name=$1
    local partitions=${2:-$PARTITIONS}
    local replication=${3:-$REPLICATION_FACTOR}
    
    echo "Creating compacted topic: $topic_name"
    
    kafka-topics.sh \
        --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS \
        --create \
        --if-not-exists \
        --topic $topic_name \
        --partitions $partitions \
        --replication-factor $replication \
        --config cleanup.policy=compact \
        --config retention.ms=86400000 \
        --config segment.ms=3600000 \
        --config compression.type=snappy \
        --config min.insync.replicas=2 \
        --config max.message.bytes=1048576
}

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
for i in {1..30}; do
    if kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --list > /dev/null 2>&1; then
        echo "Kafka is ready!"
        break
    fi
    echo "Waiting for Kafka... ($i/30)"
    sleep 10
done

# Create main analytics events topic
create_topic "website-analytics-events" 12 3

# Create processed metrics topic (compacted for latest values)
create_compacted_topic "processed-metrics" 6 3

# Create CEP alerts topic
create_topic "cep-alerts" 3 3

# Create schema registry topic
create_topic "_schemas" 1 3

# List all topics
echo ""
echo "Created topics:"
kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --list

echo ""
echo "Topic details:"
kafka-topics.sh --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --describe

echo ""
echo "Kafka topics setup completed successfully!"
