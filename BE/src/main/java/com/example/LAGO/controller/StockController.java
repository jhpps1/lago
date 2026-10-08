package com.example.LAGO.controller;

import com.example.LAGO.domain.TradeType;
import com.example.LAGO.dto.request.TradeRequest;
import com.example.LAGO.domain.User;
import com.example.LAGO.domain.Account;
import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.domain.StockHolding;
import com.example.LAGO.domain.MockTrade;
import com.example.LAGO.repository.UserRepository;
import com.example.LAGO.repository.AccountRepository;
import com.example.LAGO.repository.StockInfoRepository;
import com.example.LAGO.repository.StockHoldingRepository;
import com.example.LAGO.repository.MockTradeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 주식 매매 관련 API 컨트롤러 - 즉시 매매 처리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
@Tag(name = "주식 매매", description = "사용자 주식 매매 API")
@Validated
@Slf4j
public class StockController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StockInfoRepository stockInfoRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final MockTradeRepository mockTradeRepository;

    // ========== 상수 정의 ==========
    private static final Set<Integer> VALID_ACCOUNT_TYPES = Set.of(0, 1, 2);
    private static final Map<Integer, String> ACCOUNT_TYPE_NAMES = Map.of(
            0, "실시간 모의투자",
            1, "역사챌린지",
            2, "자동매매봇"
    );

    // ========== Custom Exception Classes ==========

    public static class StockTradingException extends RuntimeException {
        private final String errorCode;
        private final Map<String, Object> details;

        public StockTradingException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
            this.details = new HashMap<>();
        }

        public StockTradingException withDetail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public String getErrorCode() { return errorCode; }
        public Map<String, Object> getDetails() { return details; }
    }

    // ========== 메인 API 엔드포인트 ==========

    /**
     * 즉시 매매 처리 API (기존 엔드포인트 유지)
     * 매수/매도를 즉시 처리하고 결과를 반환
     */
    @PostMapping("/order")
    @Operation(
            summary = "즉시 매매 처리",
            description = "매수/매도를 즉시 처리합니다 (accountType: 0=실시간모의투자, 1=역사챌린지, 2=자동매매봇)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "매매 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @Transactional
    public ResponseEntity<?> submitOrder(
            @Parameter(description = "매매 요청 정보", required = true)
            @RequestBody @Valid TradeRequest request
    ) {
        try {
            // 1. 주문 검증
            ValidationResult validation = validateOrder(request);

            // 2. 즉시 매매 처리
            TradeResult tradeResult = executeTrade(request, validation);

            // 3. 성공 응답
            return ResponseEntity.ok(createSuccessResponse(tradeResult, request, validation));

        } catch (StockTradingException e) {
            log.warn("매매 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e));

        } catch (Exception e) {
            log.error("매매 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "INTERNAL_ERROR",
                            "message", "매매 처리 중 오류가 발생했습니다",
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * 매수 API (기존 호환성을 위해 유지)
     */
    @PostMapping("/buy")
    @Operation(summary = "주식 매수", description = "주식을 즉시 매수합니다")
    @Transactional
    public ResponseEntity<?> buyStock(@RequestBody @Valid TradeRequest request) {
        request.setTradeType(TradeType.BUY);
        return submitOrder(request);
    }

    /**
     * 매도 API (기존 호환성을 위해 유지)
     */
    @PostMapping("/sell")
    @Operation(summary = "주식 매도", description = "주식을 즉시 매도합니다")
    @Transactional
    public ResponseEntity<?> sellStock(@RequestBody @Valid TradeRequest request) {
        request.setTradeType(TradeType.SELL);
        return submitOrder(request);
    }

    // ========== 검증 로직 ==========

    private ValidationResult validateOrder(TradeRequest request) {
        // 1. 사용자 검증
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new StockTradingException("USER_NOT_FOUND",
                        "사용자를 찾을 수 없습니다")
                        .withDetail("userId", request.getUserId()));

        // 2. 계좌 타입 검증
        if (!VALID_ACCOUNT_TYPES.contains(request.getAccountType())) {
            throw new StockTradingException("INVALID_ACCOUNT_TYPE",
                    "잘못된 계좌 타입입니다")
                    .withDetail("accountType", request.getAccountType())
                    .withDetail("validTypes", VALID_ACCOUNT_TYPES);
        }

        // 3. 계좌 검증 - Long 타입으로 변경
        Account account = accountRepository.findByUserIdAndType(
                        Long.valueOf(request.getUserId()), request.getAccountType())
                .orElseThrow(() -> new StockTradingException("ACCOUNT_NOT_FOUND",
                        String.format("계좌를 찾을 수 없습니다 (%s)",
                                ACCOUNT_TYPE_NAMES.get(request.getAccountType())))
                        .withDetail("userId", request.getUserId())
                        .withDetail("accountType", request.getAccountType()));

        // 4. 종목 검증
        StockInfo stockInfo = stockInfoRepository.findByCode(request.getStockCode())
                .orElseThrow(() -> new StockTradingException("STOCK_NOT_FOUND",
                        "존재하지 않는 종목입니다")
                        .withDetail("stockCode", request.getStockCode()));

        // 5. 매수 시 잔고 검증
        if (TradeType.BUY.equals(request.getTradeType())) {
            int requiredAmount = request.getPrice() * request.getQuantity();
            if (account.getBalance() < requiredAmount) {
                throw new StockTradingException("INSUFFICIENT_BALANCE",
                        String.format("잔고가 부족합니다. 필요: %,d원, 보유: %,d원",
                                requiredAmount, account.getBalance()))
                        .withDetail("requiredAmount", requiredAmount)
                        .withDetail("currentBalance", account.getBalance());
            }
        }

        // 6. 매도 시 보유 수량 검증 - 실제 Repository 메서드 사용
        if (TradeType.SELL.equals(request.getTradeType())) {
            Optional<StockHolding> holding = stockHoldingRepository
                    .findByAccountIdAndStockCode(account.getAccountId(), request.getStockCode());

            if (holding.isEmpty() || holding.get().getQuantity() < request.getQuantity()) {
                int currentQuantity = holding.map(StockHolding::getQuantity).orElse(0);
                throw new StockTradingException("INSUFFICIENT_STOCK",
                        String.format("보유 수량이 부족합니다. 보유: %d주, 요청: %d주",
                                currentQuantity, request.getQuantity()))
                        .withDetail("currentQuantity", currentQuantity)
                        .withDetail("requestedQuantity", request.getQuantity());
            }
        }

        return new ValidationResult(user, account, stockInfo);
    }

    // ========== 매매 처리 로직 ==========

    private TradeResult executeTrade(TradeRequest request, ValidationResult validation) {
        Account account = validation.account;
        StockInfo stockInfo = validation.stockInfo;
        int tradeAmount = request.getPrice() * request.getQuantity();

        if (TradeType.BUY.equals(request.getTradeType())) {
            // 매수 처리
            return processBuy(account, stockInfo, request, tradeAmount);
        } else {
            // 매도 처리
            return processSell(account, stockInfo, request, tradeAmount);
        }
    }

    private TradeResult processBuy(Account account, StockInfo stockInfo,
                                   TradeRequest request, int tradeAmount) {
        // 1. 계좌 잔고 차감
        account.setBalance(account.getBalance() - tradeAmount);
        accountRepository.save(account);

        // 2. 보유 주식 업데이트 또는 생성 - 실제 Repository 메서드 사용
        Optional<StockHolding> existingHolding = stockHoldingRepository
                .findByAccountIdAndStockCode(account.getAccountId(), stockInfo.getCode());

        StockHolding holding;
        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.setQuantity(holding.getQuantity() + request.getQuantity());
            holding.setTotalPrice(holding.getTotalPrice() + tradeAmount);
        } else {
            holding = new StockHolding();
            // StockHolding 엔티티의 실제 필드 구조에 맞게 설정
            holding.setAccountId(account.getAccountId());
            holding.setStockInfoId(stockInfo.getStockInfoId());
            holding.setQuantity(request.getQuantity());
            holding.setTotalPrice(tradeAmount);
        }
        stockHoldingRepository.save(holding);

        // 3. 거래 내역 저장
        MockTrade trade = createTradeRecord(account, stockInfo, request, "BUY");
        mockTradeRepository.save(trade);

        // 4. 계좌 총 자산 업데이트
        updateAccountTotalAsset(account);

        return new TradeResult(trade.getTradeId(), account.getBalance(),
                holding.getQuantity(), tradeAmount);
    }

    private TradeResult processSell(Account account, StockInfo stockInfo,
                                    TradeRequest request, int tradeAmount) {
        // 1. 보유 주식 차감 - 실제 Repository 메서드 사용
        StockHolding holding = stockHoldingRepository
                .findByAccountIdAndStockCode(account.getAccountId(), stockInfo.getCode())
                .orElseThrow(() -> new StockTradingException("NO_HOLDING", "보유 주식이 없습니다"));

        int remainingQuantity = holding.getQuantity() - request.getQuantity();
        holding.setQuantity(remainingQuantity);

        // 평균 단가 계산 후 총 매입가 조정
        if (remainingQuantity > 0) {
            int avgPrice = holding.getTotalPrice() / (holding.getQuantity() + request.getQuantity());
            holding.setTotalPrice(avgPrice * remainingQuantity);
            stockHoldingRepository.save(holding);
        } else {
            stockHoldingRepository.delete(holding);
        }

        // 2. 계좌 잔고 증가
        account.setBalance(account.getBalance() + tradeAmount);
        accountRepository.save(account);

        // 3. 거래 내역 저장
        MockTrade trade = createTradeRecord(account, stockInfo, request, "SELL");
        mockTradeRepository.save(trade);

        // 4. 계좌 총 자산 및 수익률 업데이트
        updateAccountTotalAsset(account);

        return new TradeResult(trade.getTradeId(), account.getBalance(),
                remainingQuantity, tradeAmount);
    }

    private MockTrade createTradeRecord(Account account, StockInfo stockInfo,
                                        TradeRequest request, String buySell) {
        MockTrade trade = new MockTrade();
        // MockTrade 엔티티의 실제 필드 구조에 맞게 설정
        trade.setAccountId(account.getAccountId());
        trade.setStockId(stockInfo.getStockInfoId());
        trade.setTradeType("BUY".equals(buySell) ? TradeType.BUY : TradeType.SELL);
        trade.setQuantity(request.getQuantity());
        trade.setPrice(request.getPrice());
        trade.setTradeAt(LocalDateTime.now());
        trade.setIsQuiz(false);
        return trade;
    }

    private void updateAccountTotalAsset(Account account) {
        // 보유 주식의 현재 가치 계산 (실제로는 현재가를 조회해야 함)
        // 여기서는 간단히 매입가 기준으로 계산
        int totalStockValue = stockHoldingRepository
                .findByAccountId(account.getAccountId())
                .stream()
                .mapToInt(StockHolding::getTotalPrice)
                .sum();

        account.setTotalAsset(account.getBalance() + totalStockValue);
        accountRepository.save(account);
    }

    // ========== 응답 생성 ==========

    private Map<String, Object> createSuccessResponse(TradeResult result,
                                                      TradeRequest request,
                                                      ValidationResult validation) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tradeId", result.tradeId);
        response.put("userId", request.getUserId());
        response.put("stockCode", request.getStockCode());
        response.put("stockName", validation.stockInfo.getName());
        response.put("tradeType", request.getTradeType().name());
        response.put("quantity", request.getQuantity());
        response.put("price", request.getPrice());
        response.put("tradeAmount", result.tradeAmount);
        response.put("remainingBalance", result.remainingBalance);
        response.put("currentHolding", result.currentHolding);
        response.put("status", "COMPLETED");
        response.put("message", String.format("%s 완료",
                TradeType.BUY.equals(request.getTradeType()) ? "매수" : "매도"));
        response.put("completedAt", LocalDateTime.now());
        return response;
    }

    private Map<String, Object> createErrorResponse(StockTradingException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", e.getErrorCode());
        response.put("message", e.getMessage());
        response.putAll(e.getDetails());
        response.put("timestamp", LocalDateTime.now());
        return response;
    }

    // ========== Exception Handler ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "VALIDATION_ERROR",
                "message", "입력값 검증 실패",
                "details", errors,
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e) {
        log.error("예상치 못한 오류 발생", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "INTERNAL_ERROR",
                "message", "서버 내부 오류가 발생했습니다",
                "timestamp", LocalDateTime.now()
        ));
    }

    // ========== Inner Classes ==========

    private static class ValidationResult {
        final User user;
        final Account account;
        final StockInfo stockInfo;

        ValidationResult(User user, Account account, StockInfo stockInfo) {
            this.user = user;
            this.account = account;
            this.stockInfo = stockInfo;
        }
    }

    private static class TradeResult {
        final Long tradeId;
        final Integer remainingBalance;
        final Integer currentHolding;
        final Integer tradeAmount;

        TradeResult(Long tradeId, Integer remainingBalance,
                    Integer currentHolding, Integer tradeAmount) {
            this.tradeId = tradeId;
            this.remainingBalance = remainingBalance;
            this.currentHolding = currentHolding;
            this.tradeAmount = tradeAmount;
        }
    }
}