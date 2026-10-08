package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * AI 봇 목록 응답 DTO
 * 지침서 명세: AI 봇은 USER 테이블의 is_ai=true로 구분
 */
@Getter
@Builder
@Schema(description = "AI 봇 목록 응답")
public class AiBotListResponse {

    @Schema(description = "AI 봇 사용자 ID", example = "2")
    private Long userId;

    @Schema(description = "AI 봇 식별 ID", example = "1")
    private Integer aiId;

    @Schema(description = "AI 봇 이름", example = "화끈이")
    private String nickname;

    @Schema(description = "투자 성향", example = "공격투자형")
    private String personality;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/ai-bot-1.png")
    private String profileImg;

    @Schema(description = "총 자산", example = "12500000")
    private Integer totalAsset;

    @Schema(description = "수익", example = "2500000")
    private Integer profit;

    @Schema(description = "수익률 (%)", example = "25.0")
    private Double profitRate;

    @Schema(description = "현재 보유 현금", example = "3000000")
    private Integer balance;
}
