package com.example.LAGO.dto.response;

import com.example.LAGO.domain.ChartPattern;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 차트 패턴 응답 DTO
 * 
 * 지침서 명세: API 명세서 기준, 파라미터/반환 구조 임의 변경 금지
 * 연동된 EC2 DB CHART_PATTERN 테이블 결과 반환
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-09
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "차트 패턴 응답 정보")
public class ChartPatternResponse {

    /**
     * 패턴 ID (CHART_PATTERN 테이블 pattern_id)
     */
    @Schema(description = "패턴 ID", example = "1")
    private Integer pattern_id;

    /**
     * 패턴 이름
     */
    @Schema(description = "패턴 이름", example = "헤드&숄더 패턴")
    private String name;

    /**
     * 패턴 설명
     */
    @Schema(description = "패턴 설명", example = "헤드&숄더 패턴은 3개의 고점을 기준으로 하는 차트 형태로...")
    private String description;

    /**
     * 차트 이미지 파일명
     */
    @Schema(description = "차트 이미지 파일명", example = "head_and_shoulder.jpg")
    private String chart_img;

    /**
     * ChartPattern 엔티티를 ChartPatternResponse로 변환하는 정적 팩토리 메서드
     * 지침서 명세: 예외처리/Validation 코드 필수
     */
    public static ChartPatternResponse from(ChartPattern chartPattern) {
        if (chartPattern == null) {
            throw new IllegalArgumentException("ChartPattern cannot be null");
        }
        
        return ChartPatternResponse.builder()
                .pattern_id(chartPattern.getPatternId())
                .name(chartPattern.getName())
                .description(chartPattern.getDescription())
                .chart_img(chartPattern.getChartImg())
                .build();
    }
}