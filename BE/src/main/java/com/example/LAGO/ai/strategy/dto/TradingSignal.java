package com.example.LAGO.ai.strategy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 매매 신호 DTO
 * 
 * 캐릭터별 AI 전략에서 생성되는 매매 신호 정보
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "매매 신호")
public class TradingSignal {

    /**
     * 매매 행동 (BUY, SELL, HOLD 등)
     */
    @Schema(description = "매매 행동", example = "BUY", allowableValues = {"STRONG_BUY", "BUY", "WEAK_BUY", "HOLD", "WEAK_SELL", "SELL", "STRONG_SELL", "WATCH"})
    private String action;

    /**
     * 신호 강도 (0.0 ~ 1.0)
     */
    @Schema(description = "신호 강도", example = "0.85", minimum = "0.0", maximum = "1.0")
    private Double strength;

    /**
     * 신뢰도 (0.0 ~ 1.0)
     */
    @Schema(description = "신뢰도", example = "0.75", minimum = "0.0", maximum = "1.0")
    private Double confidence;

    /**
     * 추천 수량 (주)
     */
    @Schema(description = "추천 수량", example = "10")
    private Integer recommendedQuantity;

    /**
     * 목표 가격
     */
    @Schema(description = "목표 가격", example = "75000")
    private Integer targetPrice;

    /**
     * 손절가
     */
    @Schema(description = "손절가", example = "65000")
    private Integer stopLossPrice;

    /**
     * 매매 근거
     */
    @Schema(description = "매매 근거", example = "FinBERT 긍정 감정(0.75) + 기술적 상승 신호로 매수 추천")
    private String reasoning;
}
