package com.example.LAGO.dto.request;

import com.example.LAGO.constants.ChartMode;
import com.example.LAGO.constants.Interval;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ChartAnalysisRequest {

    @NotNull(message = "종목코드는 필수입니다.")
    @Schema(description = "종목코드", example = "005730")
    private String stockCode;

    @NotNull(message = "차트모드는 필수입니다.")
    @Schema(description = "차트모드(모의투자/역사챌린지)", example = "mock")
    private ChartMode chartMode;

    @NotNull(message = "조회 간격은 필수입니다.")
    private Interval interval;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime fromDateTime;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDateTime toDateTime;
}
