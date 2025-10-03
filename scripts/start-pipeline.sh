#!/bin/bash

# Start script for the real-time analytics pipeline
# Orchestrates the entire pipeline startup process

set -e

echo "=========================================="
echo "Real-Time Analytics Pipeline Startup"
echo "=========================================="
echo "Target: <500ms latency, >10K events/minute"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_status $RED "ERROR: Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_status $GREEN "✅ Docker is running"
}

# Function to check if Docker Compose is available
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        print_status $RED "ERROR: docker-compose is not installed. Please install docker-compose and try again."
        exit 1
    fi
    print_status $GREEN "✅ Docker Compose is available"
}

# Function to check system resources
check_resources() {
    print_status $BLUE "Checking system resources..."
    
    # Check available memory (minimum 8GB recommended)
    TOTAL_MEM=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    if [ "$TOTAL_MEM" -lt 8000 ]; then
        print_status $YELLOW "⚠️  WARNING: Only ${TOTAL_MEM}MB RAM available. 8GB+ recommended for optimal performance."
    else
        print_status $GREEN "✅ Available RAM: ${TOTAL_MEM}MB"
    fi
    
    # Check available disk space (minimum 10GB recommended)
    AVAILABLE_DISK=$(df -BG . | awk 'NR==2{print $4}' | sed 's/G//')
    if [ "$AVAILABLE_DISK" -lt 10 ]; then
        print_status $YELLOW "⚠️  WARNING: Only ${AVAILABLE_DISK}GB disk space available. 10GB+ recommended."
    else
        print_status $GREEN "✅ Available disk space: ${AVAILABLE_DISK}GB"
    fi
}

# Function to start core infrastructure
start_infrastructure() {
    print_status $BLUE "Starting core infrastructure..."
    
    # Start Zookeeper and Kafka cluster
    print_status $YELLOW "Starting Zookeeper and Kafka cluster..."
    docker-compose up -d zookeeper kafka-1 kafka-2 kafka-3
    
    # Wait for Kafka to be ready
    print_status $YELLOW "Waiting for Kafka cluster to be ready..."
    for i in {1..60}; do
        if docker-compose exec kafka-1 kafka-topics.sh --bootstrap-server kafka-1:29092 --list > /dev/null 2>&1; then
            print_status $GREEN "✅ Kafka cluster is ready!"
            break
        fi
        echo -n "."
        sleep 5
    done
    
    # Start Schema Registry
    print_status $YELLOW "Starting Schema Registry..."
    docker-compose up -d schema-registry
    
    # Start InfluxDB
    print_status $YELLOW "Starting InfluxDB..."
    docker-compose up -d influxdb
    
    # Wait for InfluxDB to be ready
    print_status $YELLOW "Waiting for InfluxDB to be ready..."
    for i in {1..60}; do
        if curl -s http://localhost:8086/health > /dev/null 2>&1; then
            print_status $GREEN "✅ InfluxDB is ready!"
            break
        fi
        echo -n "."
        sleep 5
    done
}

# Function to setup Kafka topics
setup_kafka_topics() {
    print_status $BLUE "Setting up Kafka topics..."
    
    # Make setup script executable
    chmod +x scripts/setup-kafka-topics.sh
    
    # Run topic setup
    if ./scripts/setup-kafka-topics.sh; then
        print_status $GREEN "✅ Kafka topics created successfully"
    else
        print_status $RED "❌ Failed to create Kafka topics"
        exit 1
    fi
}

# Function to start processing components
start_processing() {
    print_status $BLUE "Starting processing components..."
    
    # Start Flink cluster
    print_status $YELLOW "Starting Flink cluster..."
    docker-compose up -d flink-jobmanager flink-taskmanager
    
    # Wait for Flink to be ready
    print_status $YELLOW "Waiting for Flink to be ready..."
    for i in {1..60}; do
        if curl -s http://localhost:8081/overview > /dev/null 2>&1; then
            print_status $GREEN "✅ Flink cluster is ready!"
            break
        fi
        echo -n "."
        sleep 5
    done
    
    # Start WebSocket server
    print_status $YELLOW "Starting WebSocket server..."
    docker-compose up -d websocket-server
    
    # Wait for WebSocket server
    print_status $YELLOW "Waiting for WebSocket server..."
    for i in {1..30}; do
        if curl -s http://localhost:8082/health > /dev/null 2>&1; then
            print_status $GREEN "✅ WebSocket server is ready!"
            break
        fi
        echo -n "."
        sleep 5
    done
}

# Function to start monitoring
start_monitoring() {
    print_status $BLUE "Starting monitoring stack..."
    
    # Start Prometheus
    print_status $YELLOW "Starting Prometheus..."
    docker-compose up -d prometheus
    
    # Start Grafana
    print_status $YELLOW "Starting Grafana..."
    docker-compose up -d grafana
    
    # Wait for monitoring services
    print_status $YELLOW "Waiting for monitoring services..."
    for i in {1..30}; do
        if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1 && curl -s http://localhost:3001/api/health > /dev/null 2>&1; then
            print_status $GREEN "✅ Monitoring stack is ready!"
            break
        fi
        echo -n "."
        sleep 5
    done
}

# Function to start applications
start_applications() {
    print_status $BLUE "Starting applications..."
    
    # Start React dashboard
    print_status $YELLOW "Starting React dashboard..."
    docker-compose up -d react-dashboard
    
    # Start event simulator
    print_status $YELLOW "Starting event simulator..."
    docker-compose up -d event-simulator
    
    # Wait for applications
    print_status $YELLOW "Waiting for applications..."
    for i in {1..60}; do
        if curl -s http://localhost:3000 > /dev/null 2>&1; then
            print_status $GREEN "✅ Applications are ready!"
            break
        fi
        echo -n "."
        sleep 5
    done
}

# Function to display service URLs
show_service_urls() {
    print_status $GREEN "=========================================="
    print_status $GREEN "Pipeline Services Started Successfully!"
    print_status $GREEN "=========================================="
    echo ""
    print_status $BLUE "🌐 Service URLs:"
    echo "  • React Dashboard:    http://localhost:3000"
    echo "  • Kafka UI:          http://localhost:8080"
    echo "  • Flink Dashboard:   http://localhost:8081"
    echo "  • InfluxDB UI:       http://localhost:8086"
    echo "  • WebSocket Server:  ws://localhost:8082"
    echo "  • Prometheus:        http://localhost:9090"
    echo "  • Grafana:           http://localhost:3001"
    echo ""
    print_status $BLUE "📊 Monitoring Dashboards:"
    echo "  • Analytics Overview: http://localhost:3001/d/analytics-pipeline"
    echo "  • System Metrics:     http://localhost:3001/d/system-overview"
    echo ""
    print_status $BLUE "🔧 Management Commands:"
    echo "  • View logs:          docker-compose logs -f [service-name]"
    echo "  • Stop pipeline:      docker-compose down"
    echo "  • Restart service:    docker-compose restart [service-name]"
    echo "  • Run performance test: ./scripts/performance-test.sh"
    echo ""
    print_status $YELLOW "📈 Performance Targets:"
    echo "  • Latency: <500ms end-to-end"
    echo "  • Throughput: >10K events/minute"
    echo "  • Error Rate: <1%"
    echo ""
}

# Main execution
main() {
    print_status $BLUE "Starting Real-Time Analytics Pipeline..."
    echo ""
    
    # Pre-flight checks
    check_docker
    check_docker_compose
    check_resources
    echo ""
    
    # Start services in order
    start_infrastructure
    setup_kafka_topics
    start_processing
    start_monitoring
    start_applications
    
    echo ""
    show_service_urls
    
    print_status $GREEN "🚀 Pipeline startup completed successfully!"
    print_status $YELLOW "Monitor the dashboard at http://localhost:3000 to see real-time metrics."
}

# Handle script interruption
trap 'print_status $RED "Pipeline startup interrupted!"; exit 1' INT TERM

# Run main function
main
