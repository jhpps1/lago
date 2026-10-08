package com.example.LAGO.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * FinBERT URL 기반 뉴스 분석 결과 DTO
 * 본문, 이미지, 감정분석을 모두 포함
 */
@Data
public class NewsAnalysisResult {
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("images")
    private List<String> images;
    
    // FinBERT 감정분석 결과
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
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
}