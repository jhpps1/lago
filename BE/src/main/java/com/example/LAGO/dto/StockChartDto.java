package com.example.LAGO.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StockChartDto {
    private Integer stockInfoId;
    private LocalDateTime bucket; // KST로 변환된 시간
    private String code; // 종목코드
    private String interval; // "1m", "3m", "5m", "10m", "15m", "30m", "60m"
    private Integer openPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Integer closePrice;
    private Long volume;
}