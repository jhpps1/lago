package com.example.LAGO.realtime;

import java.time.ZoneId;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.realtime.dto.TickData;
import com.example.LAGO.repository.StockInfoRepository;
import com.example.LAGO.repository.StockMinuteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.LAGO.domain.StockMinute;
import com.example.LAGO.dto.StockMinuteDto;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

// 1분봉 집계 : 임시 저장 + 집계 역할

@Slf4j
@Service
@ConditionalOnProperty(name = "redis.stream.enabled", havingValue = "true", matchIfMissing = false)
public class MinuteCandleService {
    private final Map<String, List<TickData>> minuteBucket = new ConcurrentHashMap<>();
    // String key = "종목코드_yyyyMMdd_HHmm"

    @Autowired
    private StockInfoRepository stockInfoRepository;
    
    @Autowired
    private StockMinuteRepository stockMinuteRepository;
    
    @Autowired
    private MinuteCandleWebsocketController websocketController;
    
    // 마지막으로 처리한 분을 추적 (중복 처리 방지)
    private volatile LocalDateTime lastProcessedMinute = null;
    
    // 지연 처리를 위한 스케줄러
    private ScheduledExecutorService delayedProcessingScheduler;
    
    @PostConstruct
    public void init() {
        delayedProcessingScheduler = Executors.newScheduledThreadPool(2);
        log.info("🚀 MinuteCandleService 초기화 완료 - 지연 처리 스케줄러 생성");
    }
    
    @PreDestroy
    public void cleanup() {
        if (delayedProcessingScheduler != null) {
            delayedProcessingScheduler.shutdown();
            try {
                if (!delayedProcessingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    delayedProcessingScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                delayedProcessingScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("🧹 MinuteCandleService 정리 완료");
    }

    public void addTick(TickData tick) {
        String key = tick.getMinuteKey();
        minuteBucket.computeIfAbsent(key, k -> new ArrayList<>()).add(tick);
        
        // 새로운 분이 시작되면 이전 분의 집계를 지연 처리로 스케줄링
        scheduleDelayedProcessing(tick.getTruncatedMinute());
    }
    
    /**
     * 새로운 분 도착 시 이전 분의 1분봉 집계를 지연 처리로 스케줄링
     */
    private void scheduleDelayedProcessing(LocalDateTime currentMinute) {
        if (currentMinute == null) return;
        
        // 1분 전 시간 계산
        LocalDateTime previousMinute = currentMinute.minusMinutes(1);
        
        // 이전 분을 아직 처리하지 않았다면 10초 후 처리하도록 스케줄링
        if (lastProcessedMinute == null || previousMinute.isAfter(lastProcessedMinute)) {
            String scheduleKey = previousMinute.toString();
            
            // 중복 스케줄링 방지를 위해 동기화된 블록에서 확인 및 즉시 상태 업데이트
            synchronized (this) {
                if (lastProcessedMinute == null || previousMinute.isAfter(lastProcessedMinute)) {
                    // 즉시 상태 업데이트로 중복 스케줄링 방지
                    lastProcessedMinute = previousMinute;
                    
                    delayedProcessingScheduler.schedule(() -> {
                        try {
                            log.info("⏰ 지연 처리 시작 - 분: {}", previousMinute);
                            processCompletedMinutesUpTo(previousMinute);
                        } catch (Exception e) {
                            log.error("❌ 지연 처리 중 오류 발생 - 분: {}, 오류: {}", previousMinute, e.getMessage(), e);
                        }
                    }, 10, TimeUnit.SECONDS);
                    
                    log.debug("📅 분봉 집계 스케줄링 완료 - 분: {} (10초 후 처리)", previousMinute);
                }
            }
        }
    }
    
    /**
     * 지정된 시간까지의 완성된 분봉들을 처리
     */
    private void processCompletedMinutesUpTo(LocalDateTime upToMinute) {
        // 현재 버킷에서 완성된 분봉들을 찾아서 처리
        minuteBucket.entrySet().removeIf(entry -> {
            String minuteKey = entry.getKey();
            List<TickData> tickList = entry.getValue();
            
            if (tickList.isEmpty()) return true;
            
            // 키에서 종목코드와 시간 분리
            String[] keyParts = minuteKey.split("_");
            if (keyParts.length < 3) return true;
            
            String code = keyParts[0];
            LocalDateTime minuteTime = tickList.get(0).getTruncatedMinute();
            
            // 완성된 분인지 확인 (현재 분보다 이전)
            if (minuteTime != null && !minuteTime.isAfter(upToMinute)) {
                processCompletedMinute(code, minuteKey, tickList);
                return true; // 처리 완료된 항목 제거
            }
            return false; // 아직 완성되지 않은 항목 유지
        });
    }

    // Domain(Entity)용
    public StockMinute buildCandle(String code, String minuteKey, List<TickData> tickList) {
        if (tickList == null || tickList.isEmpty()) return null;

        // 정렬(시간순) - 보통 실시간 입력이지만, 혹시 out-of-order 대비
        tickList.sort(Comparator.comparing(TickData::getParsedDateTime));

        // 첫 번째와 마지막 틱 데이터 추출
        TickData firstTick = tickList.get(0);
        TickData lastTick = tickList.get(tickList.size() - 1);

        // open: 첫 번째, close: 마지막
        int openPrice = firstTick.getOpenPrice() != null ? firstTick.getOpenPrice() : firstTick.getClosePrice();
        int closePrice = lastTick.getClosePrice();

        // high, low: 실제 거래가격(closePrice) 기준으로 계산
        int highPrice = tickList.stream()
                .mapToInt(TickData::getClosePrice)
                .max()
                .orElse(closePrice);
        int lowPrice = tickList.stream()
                .mapToInt(TickData::getClosePrice)
                .min()
                .orElse(closePrice);

        // volume 합계
        int volume = tickList.stream()
                .mapToInt(t -> t.getVolume() != null ? t.getVolume() : 0)
                .sum();

        // 집계 시간(분)
        LocalDateTime candleDateTime = firstTick.getTruncatedMinute();

        // (1) code로 stock_info_id 조회
        StockInfo stockInfo = stockInfoRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("해당 종목코드가 없습니다: " + code));

        // StockMinute 엔티티 생성 (stockInfoId 필드에 저장)
        return StockMinute.builder()
                .stockInfo(stockInfo)   // 필드명 변경: stockInfo → stockInfoId
                .date(candleDateTime)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(volume)
                .build();
    }

    // DTO 용
    public StockMinuteDto buildCandleDto(String code, String minuteKey, List<TickData> tickList) {
        StockMinute stockMinute = buildCandle(code, minuteKey, tickList);
        if (stockMinute == null) return null;
        return StockMinuteDto.fromEntity(stockMinute);
    }

    /**
     * 완성된 1분봉 처리 (DB 저장 + WebSocket 전송)
     */
    private void processCompletedMinute(String code, String minuteKey, List<TickData> tickList) {
        try {
            // 1분봉 Entity 생성
            StockMinute stockMinute = buildCandle(code, minuteKey, tickList);
            if (stockMinute == null) {
                log.warn("⚠️ 1분봉 생성 실패 - Code: {}, Key: {}", code, minuteKey);
                return;
            }
            
            // 1. DB에 저장
            StockMinute savedEntity = stockMinuteRepository.save(stockMinute);
            log.info("💾 1분봉 DB 저장 완료 - Code: {}, Time: {}", code, savedEntity.getDate());
            
            // 2. DTO 변환 후 WebSocket으로 전송
            StockMinuteDto dto = StockMinuteDto.fromEntity(savedEntity);
            websocketController.sendMinuteCandle(dto);
            log.info("📡 1분봉 WebSocket 전송 완료 - Code: {}, StockInfoId: {}", code, dto.getStockInfoId());
            
        } catch (Exception e) {
            log.error("❌ 1분봉 처리 실패 - Code: {}, Key: {}, Error: {}", code, minuteKey, e.getMessage(), e);
        }
    }
    
    /**
     * 스케줄러: 매 분마다 완성된 1분봉 처리 (혹시 놓친 것들 처리)
     * 지연 처리 방식 사용으로 비활성화
     */
    // @Scheduled(cron = "30 * * * * *") // 지연 처리와 중복 방지를 위해 비활성화
    public void scheduleProcessCompletedMinutes() {
        LocalDateTime currentTime = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDateTime oneMinuteAgo = currentTime.minusMinutes(1);
        
        log.debug("🕒 스케줄 실행 - 완성된 분봉 처리 ({}까지)", oneMinuteAgo);
        processCompletedMinutesUpTo(oneMinuteAgo);
    }
    
    /**
     * @deprecated 기존 메서드 (호환성 유지)
     */
    @Deprecated
    public void handleMinuteComplete(String code, String minuteKey) {
        List<TickData> tickList = minuteBucket.remove(minuteKey);
        if (tickList == null || tickList.isEmpty()) return;
        processCompletedMinute(code, minuteKey, tickList);
    }

    @RestController
    @RequiredArgsConstructor
    @RequestMapping("/api/redis/chunks")
    public static class ChunkDebugController {
        private final TickData.TickChunkReaderService reader;

        @GetMapping("/{id}")
        public ResponseEntity<List<Map<String,Object>>> preview(
                @PathVariable String id,
                @RequestParam(defaultValue = "5") int limit) {

            var all = reader.readChunk(id);
            var out = all.stream()
                    .limit(Math.max(0, limit))
                    .map(d -> Map.<String, Object>ofEntries(
                            Map.entry("stockId", d.stockId()),
                            Map.entry("tsKst",  d.ts().atZone(ZoneId.of("Asia/Seoul")).toString()),
                            Map.entry("price",  d.price()),
                            Map.entry("volume", d.volume())
                    ))
                    .toList();
            return ResponseEntity.ok(out);

        }
    }
}
