package com.example.LAGO.controller;

import com.example.LAGO.dto.request.ChartAnalysisRequest;
import com.example.LAGO.dto.response.ChartAnalysisResponse;
import com.example.LAGO.service.ChartAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charts")
@Tag(name = "차트 API", description = "")
@Validated
public class ChartController {

    private final ChartAnalysisService chartAnalysisService;
    private static final Logger log = LoggerFactory.getLogger(ChartController.class);

    @PostMapping("/pattern-analysis")
    @Operation(
            summary = "차트패턴 분석",
            description = "주어진 조건의 차트 데이터에서 감지된 패턴과 근거를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "차트패턴 분석 결과 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<ChartAnalysisResponse>> getPatternAnalysis(
            @Valid @RequestBody ChartAnalysisRequest requestDto
    ) {

        List<ChartAnalysisResponse> detectedPatterns = chartAnalysisService.analyzePatterns(
                requestDto.getStockCode(),
                requestDto.getChartMode(),
                requestDto.getInterval(),
                requestDto.getFromDateTime(),
                requestDto.getToDateTime()
        );

        log.info("차트 분석 응답 완료: {}개 패턴 감지", detectedPatterns.size());
        return ResponseEntity.ok(detectedPatterns);
    }
}
