package com.example.LAGO.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 차트패턴 분석 응답 DTO
 *
 */
@Data
@Schema(description = "차트패턴 분석 결과 정보")
public class ChartAnalysisResponse {
    /**
     * 패턴 이름
     */
    @Schema(description = "패턴 이름", example = "더블 탑 패턴")
    private String name;

    /**
     * 패턴 감지 근거
     */
    @Schema(description = "감지 근거", example = "고점이 두 번 형성되며 상승 추세가 멈추고 하락 반전될 가능성을 나타내는 패턴입니다.")
    private String reason;
}
