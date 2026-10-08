package com.example.LAGO.controller;

import com.example.LAGO.realtime.dto.TickData;
// import com.example.LAGO.service.RealtimeTradingService;  // 서비스 제거됨
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 실시간 매매 테스트 컨트롤러 (사용 안함 - 자동매매봇으로 대체)
 * AutoTradingBotService로 교체되어 더 이상 사용하지 않음
 */
//@RestController  // 완전 비활성화 - AutoTradingBotService로 교체됨
@RequestMapping("/api/test/realtime-trading")
@RequiredArgsConstructor
@Slf4j
public class RealtimeTradingTestController {
    
    /**
     * 가짜 실시간 가격 데이터를 주입해서 매매 테스트
     * 
     * @param stockCode 종목 코드
     * @param price 시뮬레이션할 실시간 가격
     */
    @PostMapping("/simulate/{stockCode}/{price}")
    @Operation(
        summary = "실시간 가격 시뮬레이션", 
        description = "가짜 실시간 가격 데이터를 주입해서 대기 중인 주문들을 체결 테스트"
    )
    public ResponseEntity<?> simulateRealtimePrice(
            @Parameter(description = "종목 코드", required = true)
            @PathVariable String stockCode,
            @Parameter(description = "시뮬레이션할 가격", required = true)
            @PathVariable Integer price
    ) {
        try {
            log.info("🧪 실시간 가격 시뮬레이션 시작: stockCode={}, price={}", stockCode, price);
            
            // 가짜 TickData 생성
            TickData fakeTickData = TickData.builder()
                    .code(stockCode)
                    .closePrice(price)
                    .openPrice(price)
                    .highPrice(price)
                    .lowPrice(price)
                    .volume(1000)
                    .previousDay(0)
                    .fluctuationRate(BigDecimal.ZERO)
                    .date("153000")
                    .build();
            
            // 실시간 매매 처리 실행 (비활성화 - AutoTradingBotService로 교체됨)
            // realtimeTradingService.processRealtimeOrders(fakeTickData);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "실시간 가격 시뮬레이션 완료",
                "stockCode", stockCode,
                "simulatedPrice", price,
                "simulatedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("실시간 가격 시뮬레이션 실패: stockCode={}, price={}", stockCode, price, e);
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "SIMULATION_FAILED",
                "message", "실시간 가격 시뮬레이션에 실패했습니다: " + e.getMessage(),
                "stockCode", stockCode,
                "price", price
            ));
        }
    }
    
    /**
     * 대기 중인 주문 개수 조회
     * 
     * @param stockCode 종목 코드
     */
    @GetMapping("/pending-orders/{stockCode}")
    @Operation(
        summary = "대기 주문 조회", 
        description = "특정 종목의 대기 중인 주문 개수를 조회"
    )
    public ResponseEntity<?> getPendingOrdersCount(
            @Parameter(description = "종목 코드", required = true)
            @PathVariable String stockCode
    ) {
        try {
            // int pendingCount = realtimeTradingService.getPendingOrdersCount(stockCode);
            int pendingCount = 0; // 비활성화됨
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "stockCode", stockCode,
                "pendingOrdersCount", pendingCount,
                "checkedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "PENDING_ORDERS_CHECK_FAILED",
                "message", "대기 주문 조회에 실패했습니다: " + e.getMessage(),
                "stockCode", stockCode
            ));
        }
    }
    
    /**
     * 모든 대기 주문 초기화 (테스트용)
     */
    @DeleteMapping("/pending-orders")
    @Operation(
        summary = "대기 주문 초기화", 
        description = "모든 대기 중인 주문을 삭제 (테스트용)"
    )
    public ResponseEntity<?> clearAllPendingOrders() {
        try {
            // int clearedCount = realtimeTradingService.clearAllPendingOrders();
            int clearedCount = 0; // 비활성화됨
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "모든 대기 주문이 삭제되었습니다",
                "clearedCount", clearedCount,
                "clearedAt", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "CLEAR_FAILED",
                "message", "대기 주문 삭제에 실패했습니다: " + e.getMessage()
            ));
        }
    }
}