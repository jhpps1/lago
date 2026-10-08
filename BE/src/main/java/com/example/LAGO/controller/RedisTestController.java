package com.example.LAGO.controller;

import com.example.LAGO.dto.StockInfoDto;
import com.example.LAGO.realtime.dto.TickData;
import com.example.LAGO.realtime.RealtimeDataService;
import com.example.LAGO.service.StockInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



/**
 * Redis 압축 배치 저장 테스트를 위한 컨트롤러
 * 외부 API 없이 Mock 데이터로 압축 효과를 검증
 */
@Slf4j
@RestController
@RequestMapping("/api/redis-test")
@RequiredArgsConstructor
@Tag(name = "Redis 압축 테스트", description = "Redis 압축 배치 저장 테스트 API")
public class RedisTestController {

    private final RealtimeDataService realtimeDataService;
    private final StockInfoService stockInfoService;
    private final Random random = new Random();

    /**
     * 대량 Mock 틱 데이터 생성 및 압축 테스트
     */
    @PostMapping("/generate-ticks")
    @Operation(summary = "Mock 틱 데이터 생성", description = "압축 테스트를 위한 대량 Mock 틱 데이터 생성")
    public ResponseEntity<Map<String, Object>> generateMockTicks(@RequestBody GenerateTicksRequest request) {
        try {
            log.info("🚀 Starting mock tick generation: {} ticks for {} stocks", 
                request.getCount(), request.getStockCodes().size());

            long startTime = System.currentTimeMillis();
            int totalGenerated = 0;

            // 각 종목별로 Mock 데이터 생성
            for (String stockCode : request.getStockCodes()) {
                for (int i = 0; i < request.getCount(); i++) {
                    TickData mockTick = generateMockTickData(stockCode);
                    realtimeDataService.saveTickData(mockTick);
                    totalGenerated++;
                    
                    // 시간 간격 시뮬레이션
                    if (request.getIntervalMs() > 0) {
                        Thread.sleep(request.getIntervalMs());
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("totalGenerated", totalGenerated);
            result.put("stockCodes", request.getStockCodes());
            result.put("durationMs", duration);
            result.put("throughputPerSecond", totalGenerated * 1000.0 / duration);

            log.info("✅ Mock tick generation completed: {} ticks in {}ms", totalGenerated, duration);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Failed to generate mock ticks: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 압축 배치 통계 조회
     */
    @GetMapping("/batch-stats")
    @Operation(summary = "압축 배치 통계", description = "현재 압축 배치 저장 통계 정보 조회")
    public ResponseEntity<Map<String, Object>> getBatchStatistics() {
        try {
            Map<String, Object> stats = realtimeDataService.getBatchStatistics();
            stats.put("timestamp", LocalDateTime.now());
            
            log.info("📊 Batch statistics requested: {}", stats);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Failed to get batch statistics: {}", e.getMessage(), e);
            e.printStackTrace(); // 스택 트레이스 출력
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getStackTrace()[0].toString());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 간단한 상태 확인 (디버깅용)
     */
    @GetMapping("/debug-stats")
    @Operation(summary = "디버그 통계", description = "간단한 상태 확인")
    public ResponseEntity<Map<String, Object>> getDebugStats() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            debug.put("timestamp", LocalDateTime.now());
            debug.put("realtimeDataServiceExists", realtimeDataService != null);
            debug.put("stockInfoServiceExists", stockInfoService != null);
            
            // 안전한 Redis 연결 테스트
            if (realtimeDataService != null) {
                try {
                    boolean redisConnected = realtimeDataService.isRedisConnected();
                    debug.put("redisConnected", redisConnected);
                } catch (Exception redisError) {
                    debug.put("redisError", redisError.getMessage());
                }
            }
            
            debug.put("success", true);
            return ResponseEntity.ok(debug);
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            debug.put("success", false);
            log.error("Debug stats error", e);
            return ResponseEntity.ok(debug);
        }
    }
    
    /**
     * 매우 단순한 헬스 체크
     */
    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "기본 동작 확인")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    /**
     * 특정 종목의 배치 메타데이터 조회
     */
    @GetMapping("/batch-meta/{stockCode}")
    @Operation(summary = "종목별 배치 메타데이터", description = "특정 종목의 압축 배치 메타데이터 조회")
    public ResponseEntity<Map<String, Object>> getBatchMetadata(@PathVariable String stockCode) {
        try {
            Map<Object, Object> metadata = realtimeDataService.getBatchMetadata(stockCode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("stockCode", stockCode);
            result.put("metadata", metadata);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("📋 Batch metadata requested for {}: {} entries", stockCode, metadata.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get batch metadata for {}: {}", stockCode, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * DB에서 모든 종목코드 조회 (테스트용)
     */
    @GetMapping("/stock-codes")
    @Operation(summary = "전체 종목코드 조회", description = "테스트에 사용할 수 있는 모든 종목코드 조회")
    public ResponseEntity<Map<String, Object>> getAllStockCodes() {
        try {
            List<StockInfoDto> allStocks = stockInfoService.getAllStockInfo();
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", allStocks.size());
            result.put("stockCodes", allStocks.stream().map(StockInfoDto::getCode).toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to get stock codes: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 메모리 정리 수동 실행
     */
    @PostMapping("/cleanup-memory")
    @Operation(summary = "메모리 정리", description = "압축 청크 메모리 정리 수동 실행")
    public ResponseEntity<Map<String, Object>> cleanupMemory() {
        try {
            realtimeDataService.cleanupMemory();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Memory cleanup completed");
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌ Failed to cleanup memory: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Mock 틱 데이터 생성
     */
    private TickData generateMockTickData(String stockCode) {
        // 현실적인 주가 범위 (50,000 ~ 100,000원)
        int basePrice = 50000 + random.nextInt(50000);
        int variation = 1000; // ±1000원 변동
        
        int openPrice = basePrice + random.nextInt(variation * 2) - variation;
        int closePrice = openPrice + random.nextInt(variation * 2) - variation;
        int highPrice = Math.max(openPrice, closePrice) + random.nextInt(500);
        int lowPrice = Math.min(openPrice, closePrice) - random.nextInt(500);
        
        // 현실적인 거래량 (100 ~ 10,000주)
        int volume = 100 + random.nextInt(9900);
        
        // 현재 시간 기준 HHMMSS
        String timeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        
        return TickData.builder()
                .code(stockCode)
                .date(timeString)
                .openPrice(openPrice)
                .closePrice(closePrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .volume(volume)
                .build();
    }

    /**
     * Mock 틱 생성 요청 DTO
     */
    public static class GenerateTicksRequest {
        private List<String> stockCodes;
        private int count = 1000; // 기본값: 1000개
        private long intervalMs = 0; // 기본값: 즉시 생성

        public List<String> getStockCodes() { return stockCodes; }
        public void setStockCodes(List<String> stockCodes) { this.stockCodes = stockCodes; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    }
}