package com.example.LAGO.dto.response;

import com.example.LAGO.domain.Interest;
import lombok.Builder;
import lombok.Getter;

// 관심 종목 조회
@Getter
@Builder
public class InterestResponse {
    private Integer interestId;
    private Long userId;
    private String stockCode;
    private String stockName;
    public static InterestResponse from(Interest i) {
        var s = i.getStockInfo(); // StockInfo 도메인 접근
        return InterestResponse.builder()
                .interestId(i.getInterestId())
                .userId(i.getUserId())
                .stockCode(s != null ? s.getCode() : null)  // STOCK_INFO.code
                .stockName(s != null ? s.getName() : null)  // STOCK_INFO.name
                .build();
    }
}
