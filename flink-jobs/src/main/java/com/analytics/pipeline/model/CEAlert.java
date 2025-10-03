package com.analytics.pipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Complex Event Processing alert model for pattern-based notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CEAlert {
    
    @JsonProperty("alertId")
    private String alertId;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("alertType")
    private String alertType;
    
    @JsonProperty("severity")
    private String severity;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("patternMatched")
    private String patternMatched;
    
    @JsonProperty("affectedUsers")
    private List<String> affectedUsers;
    
    @JsonProperty("metrics")
    private Map<String, Double> metrics;
    
    @JsonProperty("context")
    private Map<String, String> context;
    
    @JsonProperty("recommendedAction")
    private String recommendedAction;
    
    @JsonProperty("source")
    private String source;
}
