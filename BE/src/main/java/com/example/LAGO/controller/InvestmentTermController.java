package com.example.LAGO.controller;

import com.example.LAGO.dto.InvestmentTermDto;
import com.example.LAGO.exception.ErrorResponse;
import com.example.LAGO.service.InvestmentTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Optional;

/**
 * 투자 용어 컨트롤러
 * /api/study/term 경로로 투자 용어 관련 API 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study")
@Tag(name = "차트학습/퀴즈", description = "투자 학습 및 퀴즈 관련 API")
@Slf4j
public class InvestmentTermController {

    private final InvestmentTermService investmentTermService;

    @GetMapping("/term")
    @Operation(
        summary = "투자 용어 전체 조회", 
        description = "모든 투자 용어를 조회합니다. userId를 제공하면 각 용어의 이해도 정보도 포함됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", 
            content = @Content(schema = @Schema(implementation = InvestmentTermDto.class))),
        @ApiResponse(responseCode = "400", description = "제약조건 위반",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<InvestmentTermDto>> getAllInvestmentTerms(
            @Parameter(description = "사용자 ID (선택사항)", example = "1", required = false)
            @RequestParam(required = false) Integer userId) {
        
        log.info("투자 용어 전체 조회 요청 - userId: {}", userId);
        
        List<InvestmentTermDto> terms = investmentTermService.getAllInvestmentTerms(userId);
        
        log.info("투자 용어 {}개 조회 완료", terms.size());
        
        return ResponseEntity.ok(terms);
    }


    @GetMapping("/term/search")
    @Operation(
        summary = "투자 용어 검색", 
        description = "키워드로 투자 용어를 검색합니다. userId를 제공하면 각 용어의 이해도 정보도 포함됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "검색 성공",
            content = @Content(schema = @Schema(implementation = InvestmentTermDto.class))),
        @ApiResponse(responseCode = "400", description = "제약조건 위반",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<InvestmentTermDto>> searchInvestmentTerms(
            @Parameter(description = "검색 키워드", example = "PER") 
            @RequestParam String keyword,
            @Parameter(description = "사용자 ID (선택사항)", example = "1", required = false)
            @RequestParam(required = false) Integer userId) {
        
        log.info("투자 용어 검색 요청 - keyword: {}, userId: {}", keyword, userId);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("검색 키워드가 비어있음");
            return ResponseEntity.badRequest().build();
        }
        
        List<InvestmentTermDto> terms = investmentTermService.searchInvestmentTerms(keyword.trim(), userId);
        
        log.info("투자 용어 검색 완료 - keyword: {}, 결과: {}개", keyword, terms.size());
        
        return ResponseEntity.ok(terms);
    }
}