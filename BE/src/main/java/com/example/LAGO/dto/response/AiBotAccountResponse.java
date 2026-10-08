package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 매매봇 계좌 응답 DTO
 * 지침서 명세: AI 봇은 user 테이블의 is_ai 컬럼으로 구분, AI 거래/전략은 별도 테이블
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 매매봇 계좌 정보")
public class AiBotAccountResponse {

    @Schema(description = "AI 봇 식별자", example = "1")
    private Integer aiId;

    @Schema(description = "AI 봇 닉네임", example = "워렌버핏봇")
    private String nickname;

    @Schema(description = "계좌 ID", example = "12345")
    private Long accountId;

    @Schema(description = "보유 현금", example = "1000000")
    private Integer balance;

    @Schema(description = "총 자산", example = "1500000")
    private Integer totalAsset;

    @Schema(description = "수익", example = "500000")
    private Integer profit;

    @Schema(description = "수익률", example = "50.5")
    private Double profitRate;

    @Schema(description = "계좌 타입 (0:모의투자, 1:역사챌린지)", example = "0")
    private Integer type;

    @Schema(description = "거래 횟수", example = "25")
    private Long tradeCount;

    @Schema(description = "평균 거래 금액", example = "200000.0")
    private Double avgTradeValue;

    @Schema(description = "마지막 거래일", example = "2025-08-04T14:30:00")
    private LocalDateTime lastTradeAt;

    @Schema(description = "AI 전략", example = "보수적 투자")
    private String strategy;

    @Schema(description = "응답 시간", example = "2025-08-04T16:45:00")
    private LocalDateTime responseTime;
}
