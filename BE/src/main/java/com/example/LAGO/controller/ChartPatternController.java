package com.example.LAGO.controller;

import com.example.LAGO.dto.response.ChartPatternResponse;
import com.example.LAGO.exception.ErrorResponse;
import com.example.LAGO.service.ChartPatternService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 차트 패턴 컨트롤러
 * 
 * 지침서 명세: API 명세서 기준, URL/메서드/입출력 모두 일치 (파라미터/반환 구조 임의 변경 금지)
 * 지침서 명세: Swagger/Javadoc 모든 API/메서드/엔티티에 상세 주석 작성
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-09
 */
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "차트학습/퀴즈", description = "투자 학습 및 퀴즈 관련 API")
public class ChartPatternController {

    private final ChartPatternService chartPatternService;

    /**
     * 차트 패턴 목록 조회 API
     * URL: /api/study/chart
     * Method: GET
     * 
     * @return 차트 패턴 응답 리스트
     */
    @Operation(
        summary = "차트 패턴 목록 조회",
        description = "차트 학습 화면에서 전체 차트 패턴 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ChartPatternResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/chart")
    public ResponseEntity<List<ChartPatternResponse>> getChartPatterns() {
        
        try {
            log.info("차트 패턴 목록 조회 API 호출");
            
            List<ChartPatternResponse> responses = chartPatternService.getAllChartPatterns();
            
            log.info("차트 패턴 목록 조회 API 응답 완료. 반환된 패턴 수: {}", responses.size());
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            log.error("차트 패턴 목록 조회 API 처리 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
    }
}