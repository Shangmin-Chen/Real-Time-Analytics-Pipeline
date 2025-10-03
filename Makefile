# Real-Time Analytics Pipeline Makefile
# Provides convenient commands for development and deployment

.PHONY: help build start stop restart clean test performance logs status

# Default target
help: ## Show this help message
	@echo "Real-Time Analytics Pipeline - Available Commands:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Build targets
build: ## Build all Docker images
	@echo "Building all Docker images..."
	docker-compose build

build-flink: ## Build Flink streaming jobs
	@echo "Building Flink streaming jobs..."
	cd flink-jobs && mvn clean package -DskipTests

build-simulator: ## Build event simulator
	@echo "Building event simulator..."
	cd event-simulator && mvn clean package -DskipTests

build-websocket: ## Build WebSocket server
	@echo "Building WebSocket server..."
	cd websocket-server && mvn clean package -DskipTests

build-dashboard: ## Build React dashboard
	@echo "Building React dashboard..."
	cd react-dashboard && npm install && npm run build

# Deployment targets
start: ## Start the entire pipeline
	@echo "Starting Real-Time Analytics Pipeline..."
	./scripts/start-pipeline.sh

start-infrastructure: ## Start core infrastructure only
	@echo "Starting infrastructure services..."
	docker-compose up -d zookeeper kafka-1 kafka-2 kafka-3 schema-registry influxdb

start-processing: ## Start processing components
	@echo "Starting processing components..."
	docker-compose up -d flink-jobmanager flink-taskmanager websocket-server

start-applications: ## Start applications
	@echo "Starting applications..."
	docker-compose up -d react-dashboard event-simulator

start-monitoring: ## Start monitoring stack
	@echo "Starting monitoring stack..."
	docker-compose up -d prometheus grafana

# Management targets
stop: ## Stop all services
	@echo "Stopping all services..."
	docker-compose down

restart: stop start ## Restart the entire pipeline

clean: ## Clean up containers, volumes, and images
	@echo "Cleaning up..."
	docker-compose down -v --rmi all
	docker system prune -f

clean-volumes: ## Clean up only volumes
	@echo "Cleaning up volumes..."
	docker-compose down -v

# Setup targets
setup-topics: ## Setup Kafka topics
	@echo "Setting up Kafka topics..."
	./scripts/setup-kafka-topics.sh

setup-schemas: ## Register Avro schemas
	@echo "Registering Avro schemas..."
	curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
		--data @schemas/events.avsc \
		http://localhost:8081/subjects/analytics-events/versions

# Testing targets
test: ## Run performance tests
	@echo "Running performance tests..."
	./scripts/performance-test.sh

test-latency: ## Test latency requirements
	@echo "Testing latency requirements..."
	curl -s "http://localhost:9090/api/v1/query?query=histogram_quantile(0.95,rate(analytics_pipeline_latency_seconds_bucket[5m]))" | jq -r '.data.result[0].value[1]' | awk '{print "Latency P95: " $$1*1000 "ms"}'

test-throughput: ## Test throughput requirements
	@echo "Testing throughput requirements..."
	curl -s "http://localhost:9090/api/v1/query?query=rate(analytics_pipeline_events_total[1m])*60" | jq -r '.data.result[0].value[1]' | awk '{print "Throughput: " int($$1) " events/minute"}'

# Monitoring targets
logs: ## Show logs for all services
	docker-compose logs -f

logs-kafka: ## Show Kafka logs
	docker-compose logs -f kafka-1 kafka-2 kafka-3

logs-flink: ## Show Flink logs
	docker-compose logs -f flink-jobmanager flink-taskmanager

logs-dashboard: ## Show dashboard logs
	docker-compose logs -f react-dashboard

logs-simulator: ## Show event simulator logs
	docker-compose logs -f event-simulator

status: ## Show status of all services
	@echo "Service Status:"
	@echo "==============="
	docker-compose ps

health: ## Check health of all services
	@echo "Health Check:"
	@echo "============="
	@echo -n "Kafka UI: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 && echo " ✅" || echo " ❌"
	@echo -n "Flink Dashboard: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:8081 && echo " ✅" || echo " ❌"
	@echo -n "InfluxDB: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:8086 && echo " ✅" || echo " ❌"
	@echo -n "WebSocket Server: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:8082 && echo " ✅" || echo " ❌"
	@echo -n "React Dashboard: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 && echo " ✅" || echo " ❌"
	@echo -n "Prometheus: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:9090 && echo " ✅" || echo " ❌"
	@echo -n "Grafana: "
	@curl -s -o /dev/null -w "%{http_code}" http://localhost:3001 && echo " ✅" || echo " ❌"

# Development targets
dev-dashboard: ## Start dashboard in development mode
	@echo "Starting dashboard in development mode..."
	cd react-dashboard && npm start

dev-websocket: ## Start WebSocket server in development mode
	@echo "Starting WebSocket server in development mode..."
	cd websocket-server && mvn spring-boot:run

dev-flink: ## Start Flink job in development mode
	@echo "Starting Flink job in development mode..."
	cd flink-jobs && mvn exec:java -Dexec.mainClass="com.analytics.pipeline.AnalyticsStreamingJob"

# Utility targets
scale-simulator: ## Scale event simulator
	@echo "Scaling event simulator..."
	docker-compose up -d --scale event-simulator=3

scale-flink: ## Scale Flink TaskManagers
	@echo "Scaling Flink TaskManagers..."
	docker-compose up -d --scale flink-taskmanager=3

backup-data: ## Backup InfluxDB data
	@echo "Backing up InfluxDB data..."
	docker-compose exec influxdb influx backup /backup/$(shell date +%Y%m%d_%H%M%S)

restore-data: ## Restore InfluxDB data (requires BACKUP_FILE variable)
	@echo "Restoring InfluxDB data from $(BACKUP_FILE)..."
	docker-compose exec influxdb influx restore /backup/$(BACKUP_FILE)

# Documentation targets
docs: ## Generate documentation
	@echo "Generating documentation..."
	@echo "Documentation available in docs/ directory"
	@echo "- Architecture: docs/architecture.md"
	@echo "- Deployment: docs/deployment-guide.md"
	@echo "- Performance: docs/performance-analysis.md"

# Quick start for new users
quickstart: ## Quick start for new users
	@echo "🚀 Real-Time Analytics Pipeline - Quick Start"
	@echo "============================================="
	@echo ""
	@echo "1. Prerequisites:"
	@echo "   - Docker & Docker Compose installed"
	@echo "   - 8GB+ RAM available"
	@echo "   - Ports 3000, 8080-8086, 9090-9094 free"
	@echo ""
	@echo "2. Starting the pipeline:"
	@echo "   make start"
	@echo ""
	@echo "3. Access the dashboard:"
	@echo "   http://localhost:3000"
	@echo ""
	@echo "4. Monitor performance:"
	@echo "   make test"
	@echo ""
	@echo "5. View logs:"
	@echo "   make logs"
	@echo ""
	@echo "6. Stop the pipeline:"
	@echo "   make stop"
	@echo ""
	@echo "For more commands: make help"

# Default target when no argument is provided
.DEFAULT_GOAL := help
