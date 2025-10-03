package com.analytics.pipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Core analytics event model representing website user interactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("pageViewData")
    private PageViewData pageViewData;
    
    @JsonProperty("sessionData")
    private SessionData sessionData;
    
    @JsonProperty("conversionData")
    private ConversionData conversionData;
    
    @JsonProperty("customData")
    private Map<String, String> customData;
    
    @JsonProperty("metadata")
    private EventMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageViewData {
        @JsonProperty("url")
        private String url;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("referrer")
        private String referrer;
        
        @JsonProperty("pageLoadTime")
        private Integer pageLoadTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionData {
        @JsonProperty("userAgent")
        private String userAgent;
        
        @JsonProperty("ipAddress")
        private String ipAddress;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("deviceType")
        private String deviceType;
        
        @JsonProperty("browser")
        private String browser;
        
        @JsonProperty("os")
        private String os;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversionData {
        @JsonProperty("conversionType")
        private String conversionType;
        
        @JsonProperty("value")
        private Double value;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("productId")
        private String productId;
        
        @JsonProperty("category")
        private String category;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventMetadata {
        @JsonProperty("source")
        private String source;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("environment")
        private String environment;
    }
}
