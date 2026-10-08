package com.example.LAGO.ai.sentiment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FinBERT 감정 분석 요청 DTO
 * 
 * FinBERT Flask 서버에 뉴스 URL을 전송하여 감정 분석을 요청하는 DTO
 * 
 * 지침서 명세:
 * - FinBERT는 뉴스 호재악재 판단 라이브러리로 호재1 ~ 악재-1 점수 제공
 * - 캐릭터별 AI 전략에서 FinBERT 감정 점수를 매매 판단 기준으로 활용
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FinBERT 감정 분석 요청 DTO")
public class SentimentRequestDto {

    /**
     * 분석할 뉴스 기사의 URL
     * 
     * 유효성 검증:
     * - 필수 입력 값
     * - HTTP/HTTPS URL 형식만 허용
     */
    @NotBlank(message = "뉴스 URL은 필수 입력값입니다")
    @Pattern(
        regexp = "^https?://.*", 
        message = "올바른 URL 형식이 아닙니다 (http:// 또는 https://로 시작해야 함)"
    )
    @Schema(
        description = "분석할 뉴스 기사의 URL", 
        example = "https://finance.naver.com/news/news_read.naver?article_id=0004852070&office_id=008&mode=RANK",
        required = true
    )
    private String url;

    /**
     * 요청한 사용자 ID (선택사항)
     * 로깅 및 추적 목적으로 사용
     */
    @Schema(
        description = "요청 사용자 ID (로깅용)", 
        example = "12345"
    )
    private Long userId;

    /**
     * 관련 주식 코드 (선택사항)
     * 특정 종목 관련 뉴스인 경우 주식 코드 포함
     */
    @Schema(
        description = "관련 주식 코드", 
        example = "005930"
    )
    private String stockCode;
}
