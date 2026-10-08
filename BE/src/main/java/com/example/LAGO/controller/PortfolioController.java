package com.example.LAGO.controller;

import com.example.LAGO.dto.response.AccountCurrentStatusResponse;
import com.example.LAGO.dto.response.StockHoldingResponse;
import com.example.LAGO.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 포트폴리오 조회 API 컨트롤러
 * 지침서 명세: GET /api/users/me/portfolio, GET /api/accounts/{accountId}/holdings
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "포트폴리오", description = "사용자 보유주식 및 포트폴리오 조회 API")
@Validated
public class PortfolioController {

    private final PortfolioService portfolioService;

    /**
     * 사용자 포트폴리오 조회 (모든 계좌)
     * 지침서 명세: GET /api/users/me/portfolio
     * 
     * @param userId 사용자 ID
     * @return 포트폴리오 목록
     */
    @GetMapping("/users/me/portfolio")
    @Operation(
        summary = "사용자 포트폴리오 조회", 
        description = "사용자의 모든 계좌에서 보유하고 있는 주식 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<StockHoldingResponse>> getUserPortfolio(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") // TODO: JWT 인증으로 변경 예정
            @NotNull(message = "사용자 ID는 필수입니다") 
            @Positive(message = "사용자 ID는 양수여야 합니다") 
            Long userId
    ) {
        List<StockHoldingResponse> portfolio = portfolioService.getUserPortfolio(userId);
        return ResponseEntity.ok(portfolio);
    }

    /**
     * 특정 계좌의 보유주식 조회
     * 지침서 명세: GET /api/accounts/{accountId}/holdings
     * 
     * @param accountId 계좌 ID
     * @param userId 사용자 ID (인증용)
     * @return 보유주식 목록
     */
    @GetMapping("/accounts/{accountId}/holdings")
    @Operation(
        summary = "계좌별 보유주식 조회", 
        description = "특정 계좌의 보유주식 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (본인 계좌가 아님)"),
        @ApiResponse(responseCode = "404", description = "계좌를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<StockHoldingResponse>> getAccountHoldings(
            @Parameter(description = "계좌 ID", required = true, example = "1001")
            @PathVariable 
            @NotNull(message = "계좌 ID는 필수입니다") 
            @Positive(message = "계좌 ID는 양수여야 합니다") 
            Long accountId,
            
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") // TODO: JWT 인증으로 변경 예정
            @NotNull(message = "사용자 ID는 필수입니다") 
            @Positive(message = "사용자 ID는 양수여야 합니다") 
            Long userId
    ) {
        List<StockHoldingResponse> holdings = portfolioService.getAccountHoldings(accountId, userId);
        return ResponseEntity.ok(holdings);
    }

    /**
     * 특정 종목의 보유 정보 조회
     * 
     * @param accountId 계좌 ID
     * @param stockCode 종목 코드
     * @param userId 사용자 ID (인증용)
     * @return 보유주식 정보
     */
    @GetMapping("/accounts/{accountId}/holdings/{stockCode}")
    @Operation(
        summary = "종목별 보유 정보 조회", 
        description = "특정 계좌에서 특정 종목의 보유 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "보유하지 않은 종목"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<StockHoldingResponse> getStockHolding(
            @Parameter(description = "계좌 ID", required = true, example = "1001")
            @PathVariable 
            @NotNull(message = "계좌 ID는 필수입니다") 
            @Positive(message = "계좌 ID는 양수여야 합니다") 
            Long accountId,
            
            @Parameter(description = "종목 코드", required = true, example = "005930")
            @PathVariable 
            @NotNull(message = "종목 코드는 필수입니다") 
            String stockCode,
            
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @RequestHeader("User-Id") // TODO: JWT 인증으로 변경 예정
            @NotNull(message = "사용자 ID는 필수입니다") 
            @Positive(message = "사용자 ID는 양수여야 합니다") 
            Long userId
    ) {
        StockHoldingResponse holding = portfolioService.getStockHolding(accountId, stockCode, userId);
        return ResponseEntity.ok(holding);
    }

    /**
     * 사용자 계좌 현재 상황 조회 (프론트 실시간 계산용)
     * MockTrade 기반으로 보유현금 + 보유종목 정보 제공
     * userId로 지정된 타입의 계좌를 찾아서 조회 (기본값: 타입 0)
     * 
     * @param userId 사용자 ID
     * @param type 계좌 타입 (기본값: 0)
     * @return 계좌 현재 상황
     */
    @GetMapping("/users/{userId}/current-status")
    @Operation(
        summary = "사용자 계좌 현재 상황 조회", 
        description = "프론트에서 실시간 계산을 위한 계좌 현황 정보를 조회합니다. userId로 지정된 타입의 계좌를 찾아서 조회합니다. (보유현금 + 보유종목 + 매수총액 + 사용자정보)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자 또는 계좌를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<AccountCurrentStatusResponse> getUserCurrentStatus(
            @Parameter(description = "사용자 ID", required = true, example = "1")
            @PathVariable 
            @NotNull(message = "사용자 ID는 필수입니다") 
            @Positive(message = "사용자 ID는 양수여야 합니다") 
            Long userId,
            
            @Parameter(description = "계좌 타입", required = false, example = "0")
            @RequestParam(value = "type", defaultValue = "0") 
            Integer type
    ) {
        AccountCurrentStatusResponse status = portfolioService.getUserCurrentStatus(userId, type);
        return ResponseEntity.ok(status);
    }
}
