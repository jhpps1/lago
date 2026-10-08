package com.example.LAGO.domain;

/**
 * 거래 타입 열거형
 * BUY: 매수
 * SELL: 매도
 */
public enum TradeType {
    BUY("매수"),
    SELL("매도");
    
    private final String description;
    
    TradeType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}