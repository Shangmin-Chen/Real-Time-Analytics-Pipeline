package com.analytics.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Event simulator for generating realistic website analytics events
 * 
 * Features:
 * - Configurable event rates (default 200 events/second)
 * - Realistic user behavior patterns
 * - Multiple event types (page views, sessions, conversions)
 * - Geographic and device diversity
 * - Session-based event correlation
 */
public class EventSimulator {
    
    private static final String KAFKA_BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka-1:29092,kafka-2:29092,kafka-3:29092");
    private static final String TOPIC_NAME = "website-analytics-events";
    private static final int EVENTS_PER_SECOND = Integer.parseInt(System.getenv().getOrDefault("EVENT_RATE_PER_SECOND", "200"));
    private static final int SIMULATION_DURATION_MINUTES = Integer.parseInt(System.getenv().getOrDefault("SIMULATION_DURATION_MINUTES", "60"));
    
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;
    private final Random random;
    private final ScheduledExecutorService scheduler;
    
    // Simulation state
    private final Map<String, UserSession> activeSessions = new HashMap<>();
    private final List<String> userIds = new ArrayList<>();
    private final List<String> pageUrls = new ArrayList<>();
    private final List<String> countries = new ArrayList<>();
    private final List<String> cities = new ArrayList<>();
    private final List<String> devices = new ArrayList<>();
    private final List<String> browsers = new ArrayList<>();
    private final List<String> operatingSystems = new ArrayList<>();
    
    public EventSimulator() {
        // Configure Kafka producer for high throughput and low latency
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // Ensure durability
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB batch size
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Wait up to 5ms for batching
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB buffer
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Exactly-once semantics
        
        this.producer = new KafkaProducer<>(props);
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
        this.scheduler = Executors.newScheduledThreadPool(4);
        
        initializeSimulationData();
    }
    
    public void startSimulation() {
        System.out.println("Starting event simulation...");
        System.out.println("Events per second: " + EVENTS_PER_SECOND);
        System.out.println("Simulation duration: " + SIMULATION_DURATION_MINUTES + " minutes");
        System.out.println("Target topic: " + TOPIC_NAME);
        
        // Start session management
        scheduler.scheduleAtFixedRate(this::manageSessions, 0, 30, TimeUnit.SECONDS);
        
        // Start event generation
        scheduler.scheduleAtFixedRate(this::generateEvents, 0, 1, TimeUnit.SECONDS);
        
        // Schedule simulation end
        scheduler.schedule(this::stopSimulation, SIMULATION_DURATION_MINUTES, TimeUnit.MINUTES);
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopSimulation));
    }
    
    private void initializeSimulationData() {
        // Initialize user IDs (mix of authenticated and anonymous)
        for (int i = 0; i < 1000; i++) {
            userIds.add("user_" + String.format("%04d", i));
        }
        
        // Initialize page URLs (realistic website structure)
        pageUrls.addAll(Arrays.asList(
            "/", "/home", "/products", "/products/laptops", "/products/phones", "/products/tablets",
            "/products/laptops/gaming", "/products/laptops/business", "/products/laptops/ultrabooks",
            "/products/phones/flagship", "/products/phones/budget", "/products/phones/mid-range",
            "/about", "/contact", "/support", "/blog", "/blog/tech-trends", "/blog/product-reviews",
            "/cart", "/checkout", "/checkout/shipping", "/checkout/payment", "/checkout/confirmation",
            "/account", "/account/profile", "/account/orders", "/account/settings",
            "/login", "/register", "/forgot-password", "/search", "/search?q=laptop"
        ));
        
        // Initialize geographic data
        countries.addAll(Arrays.asList("US", "CA", "GB", "DE", "FR", "IT", "ES", "AU", "JP", "BR", "IN", "CN"));
        cities.addAll(Arrays.asList(
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego",
            "Dallas", "San Jose", "Austin", "Jacksonville", "Fort Worth", "Columbus", "Charlotte", "San Francisco",
            "Toronto", "Vancouver", "Montreal", "Calgary", "London", "Manchester", "Birmingham", "Leeds",
            "Berlin", "Munich", "Hamburg", "Frankfurt", "Paris", "Lyon", "Marseille", "Toulouse"
        ));
        
        // Initialize device and browser data
        devices.addAll(Arrays.asList("DESKTOP", "MOBILE", "TABLET"));
        browsers.addAll(Arrays.asList("Chrome", "Firefox", "Safari", "Edge", "Opera"));
        operatingSystems.addAll(Arrays.asList("Windows", "macOS", "Linux", "iOS", "Android"));
    }
    
    private void generateEvents() {
        int eventsToGenerate = EVENTS_PER_SECOND;
        
        for (int i = 0; i < eventsToGenerate; i++) {
            String eventType = determineEventType();
            String event = generateEvent(eventType);
            
            if (event != null) {
                // Use user ID as key for partitioning
                String key = getEventKey(event);
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, key, event);
                
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        System.err.println("Failed to send event: " + exception.getMessage());
                    }
                });
            }
        }
    }
    
    private String determineEventType() {
        double rand = random.nextDouble();
        
        // Event type distribution
        if (rand < 0.70) return "PAGE_VIEW";        // 70% page views
        if (rand < 0.85) return "SESSION_START";    // 15% session starts
        if (rand < 0.95) return "SESSION_END";      // 10% session ends
        return "CONVERSION";                         // 5% conversions
    }
    
    private String generateEvent(String eventType) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            
            // Common fields
            event.put("eventId", UUID.randomUUID().toString());
            event.put("timestamp", System.currentTimeMillis());
            event.put("eventType", eventType);
            
            // Generate user and session data
            String userId = random.nextBoolean() ? userIds.get(random.nextInt(userIds.size())) : null;
            String sessionId = getOrCreateSession(userId);
            
            event.put("userId", userId);
            event.put("sessionId", sessionId);
            
            // Event-specific data
            switch (eventType) {
                case "PAGE_VIEW":
                    generatePageViewData(event);
                    break;
                case "SESSION_START":
                    generateSessionData(event);
                    break;
                case "SESSION_END":
                    generateSessionEndData(event);
                    break;
                case "CONVERSION":
                    generateConversionData(event);
                    break;
            }
            
            // Metadata
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("source", "event-simulator");
            metadata.put("version", "1.0");
            metadata.put("environment", "simulation");
            event.set("metadata", metadata);
            
            return objectMapper.writeValueAsString(event);
            
        } catch (Exception e) {
            System.err.println("Error generating event: " + e.getMessage());
            return null;
        }
    }
    
    private void generatePageViewData(ObjectNode event) {
        ObjectNode pageViewData = objectMapper.createObjectNode();
        
        String url = pageUrls.get(random.nextInt(pageUrls.size()));
        pageViewData.put("url", url);
        pageViewData.put("title", generatePageTitle(url));
        
        // Add referrer for some page views
        if (random.nextDouble() < 0.3) {
            pageViewData.put("referrer", "https://google.com/search?q=" + url.replace("/", "+"));
        }
        
        // Simulate page load time (100ms to 5 seconds)
        pageViewData.put("pageLoadTime", 100 + random.nextInt(4900));
        
        event.set("pageViewData", pageViewData);
    }
    
    private void generateSessionData(ObjectNode event) {
        ObjectNode sessionData = objectMapper.createObjectNode();
        
        sessionData.put("userAgent", generateUserAgent());
        sessionData.put("ipAddress", generateIPAddress());
        sessionData.put("country", countries.get(random.nextInt(countries.size())));
        sessionData.put("city", cities.get(random.nextInt(cities.size())));
        sessionData.put("deviceType", devices.get(random.nextInt(devices.size())));
        sessionData.put("browser", browsers.get(random.nextInt(browsers.size())));
        sessionData.put("os", operatingSystems.get(random.nextInt(operatingSystems.size())));
        
        event.set("sessionData", sessionData);
    }
    
    private void generateSessionEndData(ObjectNode event) {
        // Session end events don't need additional data beyond common fields
        // The session duration will be calculated by Flink based on session windows
    }
    
    private void generateConversionData(ObjectNode event) {
        ObjectNode conversionData = objectMapper.createObjectNode();
        
        String[] conversionTypes = {"PURCHASE", "SIGNUP", "DOWNLOAD", "CONTACT"};
        String conversionType = conversionTypes[random.nextInt(conversionTypes.length)];
        conversionData.put("conversionType", conversionType);
        
        if ("PURCHASE".equals(conversionType)) {
            conversionData.put("value", 50.0 + random.nextDouble() * 2000.0); // $50-$2050
            conversionData.put("currency", "USD");
            
            String[] categories = {"Electronics", "Clothing", "Books", "Home", "Sports"};
            conversionData.put("category", categories[random.nextInt(categories.length)]);
            conversionData.put("productId", "prod_" + String.format("%06d", random.nextInt(100000)));
        }
        
        event.set("conversionData", conversionData);
    }
    
    private String generatePageTitle(String url) {
        switch (url) {
            case "/": return "Home - TechStore";
            case "/products": return "Products - TechStore";
            case "/products/laptops": return "Laptops - TechStore";
            case "/products/phones": return "Phones - TechStore";
            case "/cart": return "Shopping Cart - TechStore";
            case "/checkout": return "Checkout - TechStore";
            case "/login": return "Login - TechStore";
            case "/register": return "Register - TechStore";
            default: return "Page - TechStore";
        }
    }
    
    private String generateUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Android 14; Mobile; rv:109.0) Gecko/119.0 Firefox/119.0",
            "Mozilla/5.0 (iPad; CPU OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1"
        };
        return userAgents[random.nextInt(userAgents.length)];
    }
    
    private String generateIPAddress() {
        return String.format("%d.%d.%d.%d", 
            random.nextInt(256), random.nextInt(256), 
            random.nextInt(256), random.nextInt(256));
    }
    
    private String getOrCreateSession(String userId) {
        // Simulate session management
        if (userId != null && activeSessions.containsKey(userId)) {
            UserSession session = activeSessions.get(userId);
            session.lastActivity = System.currentTimeMillis();
            return session.sessionId;
        }
        
        // Create new session
        String sessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
        if (userId != null) {
            activeSessions.put(userId, new UserSession(sessionId, System.currentTimeMillis()));
        }
        return sessionId;
    }
    
    private void manageSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 30 * 60 * 1000; // 30 minutes
        
        activeSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            return (currentTime - session.lastActivity) > sessionTimeout;
        });
    }
    
    private String getEventKey(String event) {
        try {
            ObjectNode eventNode = (ObjectNode) objectMapper.readTree(event);
            String userId = eventNode.has("userId") && !eventNode.get("userId").isNull() 
                ? eventNode.get("userId").asText() 
                : eventNode.get("sessionId").asText();
            return userId;
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
    
    private void stopSimulation() {
        System.out.println("Stopping event simulation...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        producer.close();
        System.out.println("Simulation stopped.");
    }
    
    private static class UserSession {
        final String sessionId;
        long lastActivity;
        
        UserSession(String sessionId, long lastActivity) {
            this.sessionId = sessionId;
            this.lastActivity = lastActivity;
        }
    }
    
    public static void main(String[] args) {
        EventSimulator simulator = new EventSimulator();
        simulator.startSimulation();
    }
}
