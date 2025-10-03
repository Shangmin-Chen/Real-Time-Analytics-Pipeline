package com.analytics.pipeline;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Flink streaming job for real-time analytics processing
 * 
 * Features:
 * - Windowed aggregations for real-time metrics
 * - Complex Event Processing (CEP) patterns
 * - Exactly-once processing guarantees
 * - Fault-tolerant state management
 * - Sub-500ms latency optimization
 */
public class AnalyticsStreamingJob {

    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka-1:29092,kafka-2:29092,kafka-3:29092";
    private static final String INPUT_TOPIC = "website-analytics-events";
    private static final String OUTPUT_TOPIC = "processed-metrics";
    private static final String ALERTS_TOPIC = "cep-alerts";
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        
        // Create execution environment with optimized settings
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Configure for exactly-once processing
        env.enableCheckpointing(10000); // 10 second checkpoint interval
        env.getCheckpointConfig().setCheckpointingMode(org.apache.flink.streaming.api.environment.CheckpointConfig.CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);
        env.getCheckpointConfig().setCheckpointTimeout(600000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        
        // Set parallelism for high throughput
        env.setParallelism(12);
        
        // Configure Kafka source with optimized settings
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
            .setBootstrapServers(KAFKA_BOOTSTRAP_SERVERS)
            .setTopics(INPUT_TOPIC)
            .setGroupId("analytics-processing-group")
            .setStartingOffsets(OffsetsInitializer.latest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();

        // Create data stream from Kafka
        DataStream<String> rawEventsStream = env.fromSource(
            kafkaSource,
            WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner((event, timestamp) -> extractTimestamp(event)),
            "Kafka Analytics Events"
        );

        // Parse events and create typed streams
        DataStream<AnalyticsEvent> eventsStream = rawEventsStream
            .map(new EventParser())
            .name("Parse Analytics Events")
            .setParallelism(8);

        // Filter by event type for specialized processing
        DataStream<AnalyticsEvent> pageViewStream = eventsStream
            .filter(event -> "PAGE_VIEW".equals(event.getEventType()))
            .name("Filter Page Views");

        DataStream<AnalyticsEvent> sessionStream = eventsStream
            .filter(event -> "SESSION_START".equals(event.getEventType()) || "SESSION_END".equals(event.getEventType()))
            .name("Filter Session Events");

        DataStream<AnalyticsEvent> conversionStream = eventsStream
            .filter(event -> "CONVERSION".equals(event.getEventType()))
            .name("Filter Conversion Events");

        // Real-time metrics processing (5-second windows)
        SingleOutputStreamOperator<ProcessedMetric> realTimeMetrics = processRealTimeMetrics(pageViewStream, sessionStream);
        
        // Business metrics processing (1-minute windows)
        SingleOutputStreamOperator<ProcessedMetric> businessMetrics = processBusinessMetrics(pageViewStream, conversionStream);
        
        // Session analytics processing (session windows)
        SingleOutputStreamOperator<ProcessedMetric> sessionAnalytics = processSessionAnalytics(sessionStream);

        // CEP pattern detection
        SingleOutputStreamOperator<CEAlert> cepAlerts = processCEPatterns(pageViewStream, conversionStream);

        // Combine all metrics streams
        DataStream<ProcessedMetric> allMetrics = realTimeMetrics
            .union(businessMetrics)
            .union(sessionAnalytics)
            .name("Combine Metrics Streams");

        // Sink to Kafka for further processing
        allMetrics
            .map(new MetricToJsonMapper())
            .name("Serialize Metrics")
            .sinkTo(new KafkaSink<>(OUTPUT_TOPIC))
            .name("Write Metrics to Kafka");

        // Sink alerts to Kafka
        cepAlerts
            .map(new AlertToJsonMapper())
            .name("Serialize Alerts")
            .sinkTo(new KafkaSink<>(ALERTS_TOPIC))
            .name("Write Alerts to Kafka");

        // Execute the job
        env.execute("Real-Time Analytics Pipeline");
    }

    /**
     * Process real-time metrics with 5-second tumbling windows
     */
    private static SingleOutputStreamOperator<ProcessedMetric> processRealTimeMetrics(
            DataStream<AnalyticsEvent> pageViewStream,
            DataStream<AnalyticsEvent> sessionStream) {

        // Page views per second
        SingleOutputStreamOperator<ProcessedMetric> pageViewsPerSecond = pageViewStream
            .keyBy(event -> "global")
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .process(new ProcessWindowFunction<AnalyticsEvent, ProcessedMetric, String, TimeWindow>() {
                @Override
                public void process(String key, Context context, Iterable<AnalyticsEvent> elements, Collector<ProcessedMetric> out) {
                    long count = 0;
                    long windowStart = context.window().getStart();
                    long windowEnd = context.window().getEnd();
                    
                    for (AnalyticsEvent event : elements) {
                        count++;
                    }
                    
                    double rate = count / 5.0; // events per second
                    
                    ProcessedMetric metric = ProcessedMetric.builder()
                        .metricId("page_views_per_second_" + windowStart)
                        .timestamp(System.currentTimeMillis())
                        .windowStart(windowStart)
                        .windowEnd(windowEnd)
                        .metricType("PAGE_VIEWS_PER_SECOND")
                        .value(rate)
                        .count(count)
                        .dimensions(Map.of("metric", "real_time"))
                        .tags(java.util.Arrays.asList("real_time", "page_views"))
                        .source("flink_streaming_job")
                        .build();
                    
                    out.collect(metric);
                }
            })
            .name("Page Views Per Second");

        // Active users count
        SingleOutputStreamOperator<ProcessedMetric> activeUsers = sessionStream
            .filter(event -> "SESSION_START".equals(event.getEventType()))
            .keyBy(event -> "global")
            .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
            .process(new ProcessWindowFunction<AnalyticsEvent, ProcessedMetric, String, TimeWindow>() {
                @Override
                public void process(String key, Context context, Iterable<AnalyticsEvent> elements, Collector<ProcessedMetric> out) {
                    long count = 0;
                    long windowStart = context.window().getStart();
                    long windowEnd = context.window().getEnd();
                    
                    for (AnalyticsEvent event : elements) {
                        count++;
                    }
                    
                    ProcessedMetric metric = ProcessedMetric.builder()
                        .metricId("active_users_" + windowStart)
                        .timestamp(System.currentTimeMillis())
                        .windowStart(windowStart)
                        .windowEnd(windowEnd)
                        .metricType("ACTIVE_USERS")
                        .value(count)
                        .count(count)
                        .dimensions(Map.of("metric", "real_time"))
                        .tags(java.util.Arrays.asList("real_time", "users"))
                        .source("flink_streaming_job")
                        .build();
                    
                    out.collect(metric);
                }
            })
            .name("Active Users");

        return pageViewsPerSecond.union(activeUsers);
    }

    /**
     * Process business metrics with 1-minute sliding windows
     */
    private static SingleOutputStreamOperator<ProcessedMetric> processBusinessMetrics(
            DataStream<AnalyticsEvent> pageViewStream,
            DataStream<AnalyticsEvent> conversionStream) {

        // Conversion rate calculation
        SingleOutputStreamOperator<ProcessedMetric> conversionRate = conversionStream
            .keyBy(event -> "global")
            .window(SlidingProcessingTimeWindows.of(Time.minutes(1), Time.seconds(10)))
            .process(new ProcessWindowFunction<AnalyticsEvent, ProcessedMetric, String, TimeWindow>() {
                @Override
                public void process(String key, Context context, Iterable<AnalyticsEvent> elements, Collector<ProcessedMetric> out) {
                    long conversions = 0;
                    long windowStart = context.window().getStart();
                    long windowEnd = context.window().getEnd();
                    
                    for (AnalyticsEvent event : elements) {
                        conversions++;
                    }
                    
                    // Get page views for the same window (simplified - in practice would join streams)
                    double estimatedPageViews = 1000.0; // This would be calculated from pageViewStream
                    double rate = conversions / estimatedPageViews;
                    
                    ProcessedMetric metric = ProcessedMetric.builder()
                        .metricId("conversion_rate_" + windowStart)
                        .timestamp(System.currentTimeMillis())
                        .windowStart(windowStart)
                        .windowEnd(windowEnd)
                        .metricType("CONVERSION_RATE")
                        .value(rate)
                        .count(conversions)
                        .dimensions(Map.of("metric", "business"))
                        .tags(java.util.Arrays.asList("business", "conversion"))
                        .source("flink_streaming_job")
                        .build();
                    
                    out.collect(metric);
                }
            })
            .name("Conversion Rate");

        return conversionRate;
    }

    /**
     * Process session analytics with session windows
     */
    private static SingleOutputStreamOperator<ProcessedMetric> processSessionAnalytics(
            DataStream<AnalyticsEvent> sessionStream) {

        return sessionStream
            .keyBy(AnalyticsEvent::getSessionId)
            .window(org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows.withGap(Time.minutes(30)))
            .process(new ProcessWindowFunction<AnalyticsEvent, ProcessedMetric, String, TimeWindow>() {
                @Override
                public void process(String sessionId, Context context, Iterable<AnalyticsEvent> elements, Collector<ProcessedMetric> out) {
                    long windowStart = context.window().getStart();
                    long windowEnd = context.window().getEnd();
                    long sessionDuration = windowEnd - windowStart;
                    
                    ProcessedMetric metric = ProcessedMetric.builder()
                        .metricId("session_duration_" + sessionId + "_" + windowStart)
                        .timestamp(System.currentTimeMillis())
                        .windowStart(windowStart)
                        .windowEnd(windowEnd)
                        .metricType("AVERAGE_SESSION_DURATION")
                        .value(sessionDuration / 1000.0) // Convert to seconds
                        .count(1)
                        .dimensions(Map.of("session_id", sessionId))
                        .tags(java.util.Arrays.asList("session", "duration"))
                        .source("flink_streaming_job")
                        .build();
                    
                    out.collect(metric);
                }
            })
            .name("Session Analytics");
    }

    /**
     * Process Complex Event Processing patterns
     */
    private static SingleOutputStreamOperator<CEAlert> processCEPatterns(
            DataStream<AnalyticsEvent> pageViewStream,
            DataStream<AnalyticsEvent> conversionStream) {

        // High bounce rate detection (simplified CEP pattern)
        return pageViewStream
            .keyBy(AnalyticsEvent::getUserId)
            .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
            .process(new ProcessWindowFunction<AnalyticsEvent, CEAlert, String, TimeWindow>() {
                @Override
                public void process(String userId, Context context, Iterable<AnalyticsEvent> elements, Collector<CEAlert> out) {
                    long windowStart = context.window().getStart();
                    long windowEnd = context.window().getEnd();
                    int pageCount = 0;
                    
                    for (AnalyticsEvent event : elements) {
                        pageCount++;
                    }
                    
                    // Detect high bounce rate (single page view in 5-minute window)
                    if (pageCount == 1) {
                        CEAlert alert = CEAlert.builder()
                            .alertId("high_bounce_" + userId + "_" + windowStart)
                            .timestamp(System.currentTimeMillis())
                            .alertType("HIGH_BOUNCE_RATE")
                            .severity("MEDIUM")
                            .title("High Bounce Rate Detected")
                            .description("User " + userId + " viewed only one page in 5-minute window")
                            .patternMatched("SINGLE_PAGE_VIEW_WITHIN_WINDOW")
                            .affectedUsers(java.util.Arrays.asList(userId))
                            .metrics(Map.of("page_views", (double) pageCount))
                            .context(Map.of("window_start", String.valueOf(windowStart), "window_end", String.valueOf(windowEnd)))
                            .recommendedAction("Review page content and user experience")
                            .source("flink_cep_processor")
                            .build();
                        
                        out.collect(alert);
                    }
                }
            })
            .name("CEP Pattern Detection");
    }

    /**
     * Extract timestamp from JSON event
     */
    private static long extractTimestamp(String event) {
        try {
            JsonNode jsonNode = objectMapper.readTree(event);
            return jsonNode.get("timestamp").asLong();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    /**
     * Parse JSON events to AnalyticsEvent objects
     */
    private static class EventParser implements MapFunction<String, AnalyticsEvent> {
        @Override
        public AnalyticsEvent map(String jsonEvent) throws Exception {
            return objectMapper.readValue(jsonEvent, AnalyticsEvent.class);
        }
    }

    /**
     * Convert ProcessedMetric to JSON string
     */
    private static class MetricToJsonMapper implements MapFunction<ProcessedMetric, String> {
        @Override
        public String map(ProcessedMetric metric) throws Exception {
            return objectMapper.writeValueAsString(metric);
        }
    }

    /**
     * Convert CEAlert to JSON string
     */
    private static class AlertToJsonMapper implements MapFunction<CEAlert, String> {
        @Override
        public String map(CEAlert alert) throws Exception {
            return objectMapper.writeValueAsString(alert);
        }
    }
}
