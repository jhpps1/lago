package com.example.LAGO.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkAutoIngestor {

    private final StringRedisTemplate redis;
    private final TimescaleOhlcIngestService ingestService;

    private static final String PENDING_ZSET = "ticks:ingest:pending";
    private static final String STATS_HASH   = "ticks:ingest:stats";
    private static final String LOCK_PREFIX  = "ticks:ingest:lock:";

    // 2초마다 due 아이템 처리 (고정 딜레이)
    @Scheduled(fixedDelay = 2_000L, initialDelay = 5_000L)
    public void drain() {
        var sysNow = java.time.ZonedDateTime.now();                // 시스템 기본 TZ
        var kstNow = java.time.ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        log.debug("drain tick: sysNow={} kstNow={}", sysNow, kstNow);

        LocalTime currentTime = kstNow.toLocalTime();
        if (currentTime.isBefore(LocalTime.of(9, 0)) || currentTime.isAfter(LocalTime.of(15, 30))) {
            return; // 장시간 외에는 실행 안함
        }

        long now = System.currentTimeMillis();


        // score(=예약시각) <= now 인 항목만 가져오기
        Set<String> ids = redis.opsForZSet()
                .rangeByScore(PENDING_ZSET, Double.NEGATIVE_INFINITY, now);
        if (ids == null || ids.isEmpty()) return;

        for (String id : ids) {
            String lockKey = LOCK_PREFIX + id;

            // SETNX + TTL(예: 5분) 로 락 획득
            Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofMinutes(5));
            if (Boolean.FALSE.equals(locked)) {
                // 다른 인스턴스가 처리 중
                continue;
            }

            try {
                int rows = ingestService.ingestChunkAs1sOHLC(id);

                // 성공 시에만 pending에서 제거
                redis.opsForZSet().remove(PENDING_ZSET, id);
                redis.opsForHash().increment(STATS_HASH, "okChunks", 1);
                redis.opsForHash().put(STATS_HASH, "lastOkChunk", id);

                log.info("✅ Auto-ingested chunk {} (rows={})", id, rows);

                // (선택) blob/meta 정리
                // redis.delete("ticks:chunk:" + id + ":blob");
                // redis.delete("ticks:chunk:" + id + ":meta");

            } catch (Exception ex) {
                // 실패: pending은 남겨둬서 다음 턴에 재시도되게 함
                // (옵션) 재시도 backoff: 다음 처리시각을 +15초로 밀기
                // redis.opsForZSet().add(PENDING_ZSET, id, now + 15_000);

                redis.opsForHash().increment(STATS_HASH, "failChunks", 1);
                redis.opsForHash().put(STATS_HASH, "lastErr", ex.toString());
                log.error("❌ Auto-ingest failed for chunk {}", id, ex);
            } finally {
                // 락 해제
                redis.delete(lockKey);
            }
        }
    }
}
