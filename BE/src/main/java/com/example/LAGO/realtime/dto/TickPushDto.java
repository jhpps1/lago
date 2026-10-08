package com.example.LAGO.realtime.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 실시간 데이터 웹소켓 전송용 DTO
@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class TickPushDto {
    private String code;          // 종목코드
    private String date;          // HHmmss (원본 그대로)
    private Integer openPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Integer closePrice;
    private Integer volume;
    private BigDecimal fluctuationRate;  // 등락률
    private Integer previousDay;         // 전일대비
}
