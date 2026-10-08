package com.example.LAGO.ai.strategy.dto;

import com.example.LAGO.ai.sentiment.dto.SentimentResponseDto;
import com.example.LAGO.dto.response.TechnicalAnalysisResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 캐릭터별 AI 트레이딩 추천 결과 DTO
 * 
 * 지침서 명세:
 * - 캐릭터별 매매 추천 결과를 프론트엔드에 전달
 * - FinBERT 감정 분석과 기술적 분석을 통합한 추천
 * - API 친화적 구조로 설계
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "캐릭터별 AI 트레이딩 추천 결과")
public class CharacterTradingRecommendation {

    /**
     * 캐릭터명 (화끈이, 적극이, 균형이, 조심이)
     */
    @Schema(description = "캐릭터명", example = "화끈이")
    private String characterName;

    /**
     * 사용자 ID
     */
    @Schema(description = "사용자 ID", example = "12345")
    private Long userId;

    /**
     * 주식 코드
     */
    @Schema(description = "주식 코드", example = "005930")
    private String stockCode;

    /**
     * 매매 신호 정보
     */
    @Schema(description = "매매 신호")
    private TradingSignal tradingSignal;

    /**
     * FinBERT 감정 분석 결과
     */
    @Schema(description = "감정 분석 결과")
    private SentimentResponseDto sentimentAnalysis;

    /**
     * 기술적 분석 결과
     */
    @Schema(description = "기술적 분석 결과")
    private TechnicalAnalysisResult technicalAnalysis;

    /**
     * AI 전략 ID
     */
    @Schema(description = "AI 전략 ID", example = "67890")
    private Long strategyId;

    /**
     * 성공 여부
     */
    @Schema(description = "추천 생성 성공 여부", example = "true")
    private Boolean success;

    /**
     * 오류 메시지 (실패 시)
     */
    @Schema(description = "오류 메시지", example = "주식 정보를 찾을 수 없습니다")
    private String errorMessage;

    /**
     * 추천 생성 시간
     */
    @Schema(description = "추천 생성 시간")
    private LocalDateTime createdAt;
}
