package com.example.LAGO.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 관심 종목 토글 요청 DTO
 * 사용자 ID와 종목 코드를 받아서 관심 종목 추가/삭제 처리
 */
@Schema(description = "관심 종목 토글 요청")
public class InterestToggleRequest {
    
    @Schema(description = "사용자 ID", example = "5", required = true)
    public Integer userId;
    
    @Schema(description = "종목 코드", example = "005930", required = true)
    public String stockCode;
    
    // 기본 생성자
    public InterestToggleRequest() {}
    
    // 전체 생성자
    public InterestToggleRequest(Integer userId, String stockCode) {
        this.userId = userId;
        this.stockCode = stockCode;
    }
    
    // Getter/Setter
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public String getStockCode() {
        return stockCode;
    }
    
    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }
    
    @Override
    public String toString() {
        return "InterestToggleRequest{" +
                "userId=" + userId +
                ", stockCode='" + stockCode + '\'' +
                '}';
    }
}