package com.example.LAGO.ai.sentiment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FinBERT 감정 분석 응답 DTO
 * 
 * FinBERT Flask 서버로부터 받는 감정 분석 결과를 담는 DTO
 * 
 * 지침서 명세:
 * - FinBERT 감정 점수: 호재 1.0 ~ 악재 -1.0 범위
 * - 캐릭터별 AI 전략에서 이 점수를 기반으로 매매 결정
 * - 화끈이: 긍정 신호에 강하게 반응
 * - 적극이: 성장주 관점에서 활용
 * - 균형이: 안정적 분산투자에 반영
 * - 조심이: 부정 신호에 민감하게 반응
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FinBERT 감정 분석 응답 DTO")
public class SentimentResponseDto {

    /**
     * 감정 분석 성공 여부
     */
    @Schema(description = "분석 성공 여부", example = "true")
    private Boolean success;

    /**
     * FinBERT 감정 점수
     * 
     * 점수 범위: -1.0 (극도 악재) ~ +1.0 (극도 호재)
     * - 1.0 ~ 0.3: 강한 호재 (매수 신호)
     * - 0.3 ~ 0.1: 약한 호재 (관심 종목)
     * - 0.1 ~ -0.1: 중립 (관망)
     * - -0.1 ~ -0.3: 약한 악재 (주의)
     * - -0.3 ~ -1.0: 강한 악재 (매도 신호)
     */
    @Schema(
        description = "FinBERT 감정 점수 (-1.0: 극도 악재 ~ +1.0: 극도 호재)", 
        example = "0.75",
        minimum = "-1.0",
        maximum = "1.0"
    )
    private Double sentimentScore;

    /**
     * 감정 분류 라벨
     * POSITIVE, NEUTRAL, NEGATIVE 중 하나
     */
    @Schema(
        description = "감정 분류 라벨", 
        example = "POSITIVE",
        allowableValues = {"POSITIVE", "NEUTRAL", "NEGATIVE"}
    )
    private String sentimentLabel;

    /**
     * 분석 신뢰도
     * 0.0 ~ 1.0 범위의 신뢰도 점수
     */
    @Schema(
        description = "분석 신뢰도 (0.0 ~ 1.0)", 
        example = "0.85",
        minimum = "0.0",
        maximum = "1.0"
    )
    private Double confidence;

    /**
     * 추출된 뉴스 제목
     */
    @Schema(description = "추출된 뉴스 제목", example = "삼성전자, 3분기 실적 시장 예상치 상회")
    private String newsTitle;

    /**
     * 분석된 뉴스 내용 요약 (선택사항)
     */
    @Schema(description = "뉴스 내용 요약", example = "삼성전자가 3분기 영업이익이 시장 예상을 크게 상회했다고 발표...")
    private String newsSummary;

    /**
     * 캐릭터별 매매 신호
     * 각 캐릭터가 이 뉴스에 대해 어떤 행동을 취할지 제안
     */
    @Schema(description = "화끈이 매매 신호", example = "STRONG_BUY")
    private String hwakkeunSignal;

    @Schema(description = "적극이 매매 신호", example = "BUY")
    private String jeokgeukSignal;

    @Schema(description = "균형이 매매 신호", example = "HOLD")
    private String gyunhyungSignal;

    @Schema(description = "조심이 매매 신호", example = "WEAK_BUY")
    private String josimSignal;

    /**
     * 분석 처리 시간
     */
    @Schema(description = "분석 완료 시간")
    private LocalDateTime analyzedAt;

    /**
     * 오류 메시지 (실패 시)
     */
    @Schema(description = "오류 메시지", example = "URL에서 뉴스 내용을 추출할 수 없습니다")
    private String errorMessage;

    /**
     * 처리 시간 (밀리초)
     */
    @Schema(description = "처리 소요 시간 (ms)", example = "1250")
    private Long processingTimeMs;
}
