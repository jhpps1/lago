package com.example.LAGO.controller;

import com.example.LAGO.dto.response.RankingResponse;
import com.example.LAGO.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 랭킹 조회 API 컨트롤러
 * 총자산 기준 사용자 랭킹 제공
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "랭킹", description = "사용자 랭킹 조회 API")
@Validated
public class RankingController {

    private final RankingService rankingService;

    /**
     * 총자산 기준 랭킹 조회
     * 
     * @param limit 조회할 랭킹 수 (기본값: 100, 최대: 1000)
     * @return 랭킹 목록
     */
    @GetMapping("/ranking")
    @Operation(
        summary = "총자산 기준 랭킹 조회", 
        description = "모의투자 계좌(타입 0)의 총자산을 기준으로 사용자 랭킹을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (limit 범위 초과)"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<RankingResponse>> getTotalAssetRanking(
            @Parameter(description = "조회할 랭킹 수 (1-1000)", example = "100")
            @RequestParam(value = "limit", defaultValue = "100") 
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 1000, message = "limit은 1000 이하여야 합니다")
            Integer limit
    ) {
        List<RankingResponse> rankings = rankingService.getTotalAssetRanking(limit);
        return ResponseEntity.ok(rankings);
    }

    /**
     * 특정 사용자 랭킹 조회
     * 
     * @param userId 사용자 ID
     * @return 해당 사용자의 랭킹 정보
     */
    @GetMapping("/users/{userId}/ranking")
    @Operation(
        summary = "특정 사용자 랭킹 조회", 
        description = "특정 사용자의 총자산 기준 랭킹 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<RankingResponse> getUserRanking(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable Long userId
    ) {
        RankingResponse ranking = rankingService.getUserRanking(userId);
        return ResponseEntity.ok(ranking);
    }
}