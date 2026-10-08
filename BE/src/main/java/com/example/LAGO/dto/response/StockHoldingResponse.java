package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 보유주식 응답 DTO
 * 지침서 명세: 포트폴리오 조회 시 사용
 */
@Data
@Builder
@Schema(description = "보유주식 정보")
public class StockHoldingResponse {

    @Schema(description = "보유 ID", example = "1")
    private Long holdingId;

    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "종목명", example = "삼성전자")
    private String stockName;

    @Schema(description = "보유 수량", example = "100")
    private Integer quantity;

    @Schema(description = "평균 매수가", example = "75000")
    private Integer averagePrice;

    @Schema(description = "총 매수 금액", example = "7500000")
    private Integer totalCost;

    @Schema(description = "현재 주가", example = "80000")
    private Integer currentPrice;

    @Schema(description = "현재 평가 금액", example = "8000000")
    private Integer currentValue;

    @Schema(description = "평가 손익", example = "500000")
    private Integer profitLoss;

    @Schema(description = "수익률 (%)", example = "6.67")
    private Float profitLossRate;

    @Schema(description = "최초 매수일", example = "2025-01-01T10:00:00")
    private LocalDateTime firstPurchaseDate;

    @Schema(description = "마지막 거래일", example = "2025-01-15T14:30:00")
    private LocalDateTime lastTradeDate;

    @Schema(description = "시장구분", example = "KOSPI")
    private String market;

    @Schema(description = "업종", example = "반도체")
    private String sector;
}
