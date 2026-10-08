package com.example.LAGO.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class FinBertResponse {
    
    @JsonProperty("label")
    private String label; // POSITIVE, NEGATIVE, NEUTRAL
    
    @JsonProperty("score")
    private Double score; // -100 ~ +100
    
    @JsonProperty("raw_score")
    private Double rawScore;
    
    @JsonProperty("keyword_adjustment")
    private Double keywordAdjustment;
    
    @JsonProperty("confidence")
    private Double confidence;
    
    @JsonProperty("confidence_level")
    private String confidenceLevel; // very_high, high, medium, low
    
    @JsonProperty("probabilities")
    private Map<String, Double> probabilities;
    
    @JsonProperty("trading_signal")
    private String tradingSignal; // BUY, SELL, HOLD, WEAK_BUY, WEAK_SELL
    
    @JsonProperty("signal_strength")
    private Double signalStrength;
}