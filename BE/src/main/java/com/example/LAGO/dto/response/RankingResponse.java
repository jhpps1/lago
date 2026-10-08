package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 랭킹 조회 응답 DTO
 * 총자산 기준 사용자 랭킹 정보
 */
@Data
@Builder
@Schema(description = "사용자 랭킹 정보")
public class RankingResponse {

    @Schema(description = "랭킹 순위", example = "1")
    private Integer rank;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자명", example = "testuser1")
    private String username;

    @Schema(description = "총 자산", example = "12500000")
    private Integer totalAsset;

    @Schema(description = "수익률 (%)", example = "25.0")
    private Double profitRate;

    @Schema(description = "총 수익", example = "2500000")
    private Integer totalProfit;

    @Schema(description = "투자 성향", example = "aggressive")
    private String personality;

    @Schema(description = "AI 봇 계정 여부", example = "false")
    private Boolean isAi;
}