package com.analytics.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket server for real-time analytics dashboard updates
 * 
 * Features:
 * - Real-time metrics streaming from InfluxDB
 * - Client subscription management
 * - Optimized data aggregation queries
 * - Low-latency WebSocket communication
 * - Automatic reconnection handling
 */
public class WebSocketServer {
    
    private static final String INFLUXDB_URL = System.getenv().getOrDefault("INFLUXDB_URL", "http://influxdb:8086");
    private static final String INFLUXDB_TOKEN = System.getenv().getOrDefault("INFLUXDB_TOKEN", "analytics-token-123");
    private static final String INFLUXDB_ORG = System.getenv().getOrDefault("INFLUXDB_ORG", "analytics");
    private static final String INFLUXDB_BUCKET = System.getenv().getOrDefault("INFLUXDB_BUCKET", "metrics");
    private static final int WEBSOCKET_PORT = Integer.parseInt(System.getenv().getOrDefault("WEBSOCKET_PORT", "8082"));
    private static final int UPDATE_INTERVAL_SECONDS = 5;
    
    private final InfluxDBClient influxDB;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final Map<Session, ClientSubscription> activeSessions;
    
    public WebSocketServer() {
        this.influxDB = InfluxDBClientFactory.create(INFLUXDB_URL, INFLUXDB_TOKEN.toCharArray(), INFLUXDB_ORG, INFLUXDB_BUCKET);
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.activeSessions = new ConcurrentHashMap<>();
    }
    
    public void start() throws Exception {
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(WEBSOCKET_PORT);
        
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(AnalyticsWebSocket.class);
            }
        };
        
        server.setHandler(wsHandler);
        server.start();
        
        System.out.println("WebSocket server started on port " + WEBSOCKET_PORT);
        
        // Start metrics broadcasting
        startMetricsBroadcasting();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                influxDB.close();
                scheduler.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down WebSocket server: " + e.getMessage());
            }
        }));
        
        server.join();
    }
    
    private void startMetricsBroadcasting() {
        scheduler.scheduleAtFixedRate(this::broadcastMetrics, 0, UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    private void broadcastMetrics() {
        if (activeSessions.isEmpty()) {
            return;
        }
        
        try {
            Map<String, Object> metrics = fetchLatestMetrics();
            String metricsJson = objectMapper.writeValueAsString(metrics);
            
            // Broadcast to all connected clients
            activeSessions.entrySet().removeIf(entry -> {
                Session session = entry.getKey();
                try {
                    if (session.isOpen()) {
                        session.getRemote().sendString(metricsJson);
                        return false;
                    } else {
                        return true; // Remove closed sessions
                    }
                } catch (IOException e) {
                    System.err.println("Error sending metrics to client: " + e.getMessage());
                    return true; // Remove sessions with errors
                }
            });
            
        } catch (Exception e) {
            System.err.println("Error broadcasting metrics: " + e.getMessage());
        }
    }
    
    private Map<String, Object> fetchLatestMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        QueryApi queryApi = influxDB.getQueryApi();
        
        try {
            // Fetch real-time metrics (last 1 minute)
            Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);
            
            // Page views per second
            String pageViewsQuery = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: %s)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"page_views_per_second\")\n" +
                "  |> last()",
                INFLUXDB_BUCKET, oneMinuteAgo
            );
            
            List<FluxTable> pageViewsTables = queryApi.query(pageViewsQuery);
            if (!pageViewsTables.isEmpty() && !pageViewsTables.get(0).getRecords().isEmpty()) {
                double pageViews = (Double) pageViewsTables.get(0).getRecords().get(0).getValue();
                metrics.put("pageViewsPerSecond", pageViews);
            }
            
            // Active users
            String activeUsersQuery = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: %s)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"active_users\")\n" +
                "  |> last()",
                INFLUXDB_BUCKET, oneMinuteAgo
            );
            
            List<FluxTable> activeUsersTables = queryApi.query(activeUsersQuery);
            if (!activeUsersTables.isEmpty() && !activeUsersTables.get(0).getRecords().isEmpty()) {
                double activeUsers = (Double) activeUsersTables.get(0).getRecords().get(0).getValue();
                metrics.put("activeUsers", (long) activeUsers);
            }
            
            // Conversion rate
            String conversionRateQuery = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: %s)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"conversion_rate\")\n" +
                "  |> last()",
                INFLUXDB_BUCKET, oneMinuteAgo
            );
            
            List<FluxTable> conversionRateTables = queryApi.query(conversionRateQuery);
            if (!conversionRateTables.isEmpty() && !conversionRateTables.get(0).getRecords().isEmpty()) {
                double conversionRate = (Double) conversionRateTables.get(0).getRecords().get(0).getValue();
                metrics.put("conversionRate", conversionRate);
            }
            
            // Session duration
            String sessionDurationQuery = String.format(
                "from(bucket: \"%s\")\n" +
                "  |> range(start: %s)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"average_session_duration\")\n" +
                "  |> last()",
                INFLUXDB_BUCKET, oneMinuteAgo
            );
            
            List<FluxTable> sessionDurationTables = queryApi.query(sessionDurationQuery);
            if (!sessionDurationTables.isEmpty() && !sessionDurationTables.get(0).getRecords().isEmpty()) {
                double sessionDuration = (Double) sessionDurationTables.get(0).getRecords().get(0).getValue();
                metrics.put("averageSessionDuration", sessionDuration);
            }
            
            // Add timestamp
            metrics.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            System.err.println("Error fetching metrics from InfluxDB: " + e.getMessage());
            metrics.put("error", "Failed to fetch metrics");
        }
        
        return metrics;
    }
    
    public void addSession(Session session, ClientSubscription subscription) {
        activeSessions.put(session, subscription);
        System.out.println("Client connected. Total sessions: " + activeSessions.size());
    }
    
    public void removeSession(Session session) {
        activeSessions.remove(session);
        System.out.println("Client disconnected. Total sessions: " + activeSessions.size());
    }
    
    @WebSocket
    public static class AnalyticsWebSocket {
        private static WebSocketServer server;
        
        public static void setServer(WebSocketServer server) {
            AnalyticsWebSocket.server = server;
        }
        
        @OnWebSocketConnect
        public void onConnect(Session session) {
            ClientSubscription subscription = new ClientSubscription();
            server.addSession(session, subscription);
        }
        
        @OnWebSocketClose
        public void onClose(Session session, int statusCode, String reason) {
            server.removeSession(session);
        }
        
        @OnWebSocketMessage
        public void onMessage(Session session, String message) {
            try {
                // Handle client messages (subscription changes, etc.)
                Map<String, Object> request = server.objectMapper.readValue(message, Map.class);
                String action = (String) request.get("action");
                
                if ("subscribe".equals(action)) {
                    String[] metrics = ((List<String>) request.get("metrics")).toArray(new String[0]);
                    ClientSubscription subscription = new ClientSubscription();
                    subscription.setSubscribedMetrics(Arrays.asList(metrics));
                    server.activeSessions.put(session, subscription);
                }
                
            } catch (Exception e) {
                System.err.println("Error processing client message: " + e.getMessage());
            }
        }
    }
    
    private static class ClientSubscription {
        private List<String> subscribedMetrics;
        private long lastUpdate;
        
        public ClientSubscription() {
            this.subscribedMetrics = Arrays.asList("pageViewsPerSecond", "activeUsers", "conversionRate", "averageSessionDuration");
            this.lastUpdate = System.currentTimeMillis();
        }
        
        public List<String> getSubscribedMetrics() {
            return subscribedMetrics;
        }
        
        public void setSubscribedMetrics(List<String> subscribedMetrics) {
            this.subscribedMetrics = subscribedMetrics;
        }
        
        public long getLastUpdate() {
            return lastUpdate;
        }
        
        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }
    
    public static void main(String[] args) {
        try {
            WebSocketServer server = new WebSocketServer();
            AnalyticsWebSocket.setServer(server);
            server.start();
        } catch (Exception e) {
            System.err.println("Failed to start WebSocket server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
