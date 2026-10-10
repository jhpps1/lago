package com.example.LAGO.realtime;

import com.example.LAGO.controller.StockController;
import com.example.LAGO.domain.TradeType;
import com.example.LAGO.dto.request.TradeRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * orders-stream의 AI 주문을 읽어 기존 매수·매도 처리로 넘긴다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiOrderStreamConsumer {

    private static final String GROUP = "ai-order-group";

    private final RedisTemplate<String, String> redisTemplate;
    private final StockController stockController;

    @Value("${spring.redis.stream.key:orders-stream}")
    private String streamKey;

    @Value("${redis.stream.consumer-name:ai-order-consumer}")
    private String consumerName;

    @Value("${spring.redis.stream.read-timeout:1000}")
    private long readTimeoutMs;

    @Value("${spring.redis.stream.batch-size:10}")
    private int batchSize;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean groupReady = new AtomicBoolean(false);
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        running.set(true);
        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "ai-order-stream");
            thread.setDaemon(true);
            return thread;
        });
        executor.submit(this::loop);
        log.info("AI 주문 스트림 소비 시작 stream={} group={}", streamKey, GROUP);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void loop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ensureGroup();
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        Consumer.from(GROUP, consumerName),
                        StreamReadOptions.empty().count(batchSize).block(Duration.ofMillis(readTimeoutMs)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
                if (records == null || records.isEmpty()) {
                    continue;
                }
                for (MapRecord<String, Object, Object> record : records) {
                    handle(asStrings(record.getValue()));
                    redisTemplate.opsForStream().acknowledge(streamKey, GROUP, record.getId());
                }
            } catch (Exception e) {
                if (!running.get()) {
                    return;
                }
                log.warn("AI 주문 스트림 읽기 실패: {}", e.getMessage());
                sleepQuietly();
            }
        }
    }

    private void ensureGroup() {
        if (groupReady.get()) {
            return;
        }
        try {
            // 스트림 키가 없으면 createGroup만으로는 실패한다. MKSTREAM으로 키와 그룹을 같이 만든다.
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    bytes("CREATE"),
                    bytes(streamKey),
                    bytes(GROUP),
                    bytes("0"),
                    bytes("MKSTREAM")));
            groupReady.set(true);
            log.info("주문 스트림 그룹 생성 stream={} group={}", streamKey, GROUP);
        } catch (Exception e) {
            if (isBusyGroup(e)) {
                groupReady.set(true);
                return;
            }
            throw e;
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean isBusyGroup(Throwable error) {
        while (error != null) {
            String message = error.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    private Map<String, String> asStrings(Map<Object, Object> raw) {
        Map<String, String> body = new LinkedHashMap<>();
        if (raw == null) {
            return body;
        }
        raw.forEach((key, value) -> body.put(String.valueOf(key), value == null ? null : String.valueOf(value)));
        return body;
    }

    private void handle(Map<String, String> body) {
        TradeRequest request = TradeRequest.builder()
                .userId(Long.valueOf(body.get("userId")))
                .stockCode(body.get("stockCode"))
                .tradeType(TradeType.valueOf(body.get("tradeType")))
                .quantity(Integer.valueOf(body.get("quantity")))
                .price(Integer.valueOf(body.get("price")))
                .accountType(Integer.valueOf(body.get("accountType")))
                .build();
        ResponseEntity<?> response = stockController.submitOrder(request);
        log.info("AI 주문 체결 시도 userId={} {} {}주 @{} -> {}",
                request.getUserId(), request.getTradeType(), request.getQuantity(),
                request.getPrice(), response.getStatusCode());
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
