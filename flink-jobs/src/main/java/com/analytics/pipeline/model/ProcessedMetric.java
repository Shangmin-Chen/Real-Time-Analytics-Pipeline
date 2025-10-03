package com.analytics.pipeline.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Processed metric model for storing aggregated analytics data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMetric {
    
    @JsonProperty("metricId")
    private String metricId;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("windowStart")
    private Long windowStart;
    
    @JsonProperty("windowEnd")
    private Long windowEnd;
    
    @JsonProperty("metricType")
    private String metricType;
    
    @JsonProperty("value")
    private Double value;
    
    @JsonProperty("count")
    private Long count;
    
    @JsonProperty("dimensions")
    private Map<String, String> dimensions;
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("source")
    private String source;
}
