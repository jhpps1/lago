//package com.example.LAGO.realtime;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.redis.connection.stream.*;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.time.LocalDateTime;
//
//@Slf4j
//@Component
//public class RedisStreamConsumer {
//
//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;
//
//    @Autowired
//    private MinuteCandleService minuteCandleService;
//
//    @Value("${redis.stream.key}")
//    private String streamKey;
//
//    @Value("${redis.stream.consumer-group}")
//    private String consumerGroup;
//
//    @Value("${redis.stream.consumer-name}")
//    private String consumerName;
//
//    @Value("${redis.stream.read-timeout:1000}")
//    private long readTimeout;
//
//    @Value("${redis.stream.batch-size:10}")
//    private int batchSize;
//
//    private ExecutorService executorService;
//    private Future<?> consumerTask;
//    private final AtomicBoolean running = new AtomicBoolean(false);
//
//    // 메트릭 정보
//    private volatile long totalMessagesProcessed = 0;
//    private volatile long totalErrors = 0;
//    private volatile LocalDateTime lastMessageTime = null;
//
//    @PostConstruct
//    public void initialize() {
//        log.info("🚀 RedisStreamConsumer 초기화 중...");
//        log.info("Stream Key: {}, Consumer Group: {}, Consumer Name: {}",
//                streamKey, consumerGroup, consumerName);
//
//        // Consumer Group 생성 (이미 존재하면 무시)
//        createConsumerGroupIfNotExists();
//
//        // 단일 스레드 실행기 생성
//        executorService = Executors.newSingleThreadExecutor(r -> {
//            Thread t = new Thread(r, "redis-stream-consumer");
//            t.setDaemon(true);
//            return t;
//        });
//    }
//
//    /**
//     * Consumer Group 생성 (존재하지 않는 경우에만)
//     */
//    @SuppressWarnings("unchecked")
//    private void createConsumerGroupIfNotExists() {
//        try {
//            // Stream이 없으면 먼저 생성
//            if (!redisTemplate.hasKey(streamKey)) {
//                log.info("📝 Stream '{}' 생성 중...", streamKey);
//                // 더미 데이터로 Stream 생성
//                redisTemplate.opsForStream().add(streamKey, Map.of("init", "dummy"));
//                // 더미 데이터 제거 (Stream은 유지됨)
//                redisTemplate.opsForStream().trim(streamKey, 0);
//            }
//
//            // Consumer Group 생성 (MKSTREAM 옵션 사용)
//            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
//            log.info("✅ Consumer Group '{}' 생성 완료", consumerGroup);
//        } catch (Exception e) {
//            // 이미 존재하는 경우는 정상
//            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
//                log.info("✅ Consumer Group '{}' 이미 존재", consumerGroup);
//            } else {
//                log.error("❌ Consumer Group 생성 실패: {}", e.getMessage());
//                // 재시도를 위해 예외를 다시 던지지 않음
//            }
//        }
//    }
//
//    /**
//     * Consumer 시작
//     */
//    public void startConsumer() {
//        if (running.compareAndSet(false, true)) {
//            log.info("▶️ Redis Stream Consumer 시작");
//            consumerTask = executorService.submit(this::consumeStream);
//        } else {
//            log.warn("⚠️ Consumer가 이미 실행 중입니다");
//        }
//    }
//
//    /**
//     * Consumer 중지
//     */
//    public void stopConsumer() {
//        if (running.compareAndSet(true, false)) {
//            log.info("⏹️ Redis Stream Consumer 중지");
//            if (consumerTask != null) {
//                consumerTask.cancel(true);
//            }
//        }
//    }
//
//    /**
//     * 실제 Stream 소비 로직
//     */
//    private void consumeStream() {
//        log.info("🔄 Stream 소비 시작 - Key: {}", streamKey);
//
//        while (running.get()) {
//            try {
//                // Consumer Group에서 메시지 읽기
//                StreamReadOptions readOptions = StreamReadOptions.empty()
//                        .count(batchSize)
//                        .block(Duration.ofMillis(readTimeout));
//
//                Consumer consumer = Consumer.from(consumerGroup, consumerName);
//                StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
//
//                // Stream에서 메시지 읽기
//                @SuppressWarnings("unchecked")
//                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(consumer, readOptions, streamOffset);
//
//                if (records != null && !records.isEmpty()) {
//                    log.debug("📥 {} 개의 메시지 수신", records.size());
//
//                    // 각 메시지 처리
//                    for (MapRecord<String, Object, Object> record : records) {
//                        try {
//                            processStreamRecord(record);
//
//                            // 메시지 처리 완료 확인응답
//                            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
//
//                            totalMessagesProcessed++;
//                            lastMessageTime = LocalDateTime.now();
//
//                        } catch (Exception e) {
//                            totalErrors++;
//                            log.error("❌ Record 처리 중 오류 - ID: {}, Error: {}", record.getId(), e.getMessage(), e);
//
//                            // ACK는 실패해도 해야 함 (무한 재처리 방지)
//                            try {
//                                redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
//                            } catch (Exception ackError) {
//                                log.error("❌ ACK 실패 - Record ID: {}, Error: {}", record.getId(), ackError.getMessage());
//                            }
//                        }
//                    }
//                }
//
//            } catch (Exception e) {
//                if (running.get()) {
//                    log.error("❌ Stream 소비 중 오류 발생: {}", e.getMessage(), e);
//
//                    // 오류 발생 시 잠시 대기
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                        break;
//                    }
//                }
//            }
//        }
//
//        log.info("🔚 Stream 소비 종료");
//    }
//
//    /**
//     * 개별 Stream Record 처리
//     */
//    private void processStreamRecord(MapRecord<String, Object, Object> record) {
//        try {
//            log.debug("📨 Record ID: {}, Fields: {}", record.getId(), record.getValue());
//
//            // Stream 데이터를 TickData로 변환
//            TickData tickData = parseStreamDataToTickData(record);
//
//            if (tickData != null && tickData.isValid()) {
//                // MinuteCandleService에 틱 데이터 전달
//                minuteCandleService.addTick(tickData);
//                log.debug("✅ TickData 추가 완료 - Code: {}, Time: {}", tickData.getCode(), tickData.getDate());
//            } else {
//                log.warn("⚠️ 유효하지 않은 TickData - Record ID: {}", record.getId());
//            }
//
//        } catch (Exception e) {
//            log.error("❌ Record 처리 실패 - ID: {}, Error: {}", record.getId(), e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Redis Stream 데이터를 TickData로 변환
//     * Python에서 보내는 필드 형식:
//     * - code: 종목코드
//     * - date: HHMMSS 형태 시간
//     * - close_price, open_price, high_price, low_price: 문자열로 된 가격
//     * - volume: 문자열로 된 거래량
//     * - data_type: "real_tick"
//     */
//    private TickData parseStreamDataToTickData(MapRecord<String, Object, Object> record) {
//        try {
//            var fields = record.getValue();
//
//            // 필수 필드 검증
//            if (!fields.containsKey("code") || !fields.containsKey("date")) {
//                log.warn("⚠️ 필수 필드 누락 - code 또는 date가 없음");
//                return null;
//            }
//
//            // data_type이 real_tick인지 확인
//            String dataType = getStringValue(fields, "data_type");
//            if (!"real_tick".equals(dataType)) {
//                log.debug("🔍 real_tick이 아닌 데이터 타입: {}", dataType);
//                return null;
//            }
//
//            // TickData 빌더로 생성
//            TickData.TickDataBuilder builder = TickData.builder();
//
//            // 종목코드 (필수)
//            String code = getStringValue(fields, "code");
//            if (code == null || code.trim().isEmpty()) {
//                log.warn("⚠️ 유효하지 않은 종목코드: {}", code);
//                return null;
//            }
//            builder.code(code.trim());
//
//            // 시간 (필수, HHMMSS 형태)
//            String date = getStringValue(fields, "date");
//            if (date == null || !date.matches("\\d{6}")) {
//                log.warn("⚠️ 유효하지 않은 시간 형식: {}", date);
//                return null;
//            }
//            builder.date(date);
//
//            // 가격 정보 파싱
//            builder.closePrice(parseIntegerValue(fields, "close_price"));
//            builder.openPrice(parseIntegerValue(fields, "open_price"));
//            builder.highPrice(parseIntegerValue(fields, "high_price"));
//            builder.lowPrice(parseIntegerValue(fields, "low_price"));
//
//            // 거래량 파싱
//            builder.volume(parseIntegerValue(fields, "volume"));
//
//            TickData tickData = builder.build();
//
//            log.trace("🔄 TickData 변환 완료 - {}", tickData.toString());
//            return tickData;
//
//        } catch (Exception e) {
//            log.error("❌ TickData 변환 실패: {}", e.getMessage(), e);
//            return null;
//        }
//    }
//
//    /**
//     * Map에서 문자열 값 안전하게 추출
//     */
//    private String getStringValue(Map<Object, Object> fields, String key) {
//        Object value = fields.get(key);
//        return value != null ? value.toString() : null;
//    }
//
//    /**
//     * Map에서 정수 값 안전하게 추출 (문자열로 저장된 숫자 파싱)
//     */
//    private Integer parseIntegerValue(Map<Object, Object> fields, String key) {
//        String stringValue = getStringValue(fields, key);
//        if (stringValue == null || stringValue.trim().isEmpty()) {
//            return null;
//        }
//
//        try {
//            return Integer.parseInt(stringValue.trim());
//        } catch (NumberFormatException e) {
//            log.warn("⚠️ 숫자 파싱 실패 - Key: {}, Value: {}", key, stringValue);
//            return null;
//        }
//    }
//
//    /**
//     * Consumer 상태 정보 반환
//     */
//    public String getConsumerStatus() {
//        return String.format("Consumer Status: %s | Processed: %d | Errors: %d | Last Message: %s",
//                running.get() ? "RUNNING" : "STOPPED",
//                totalMessagesProcessed,
//                totalErrors,
//                lastMessageTime != null ? lastMessageTime.toString() : "Never");
//    }
//
//    /**
//     * Consumer 메트릭 로깅 (주기적 상태 출력)
//     */
//    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000) // 1분마다
//    public void logConsumerMetrics() {
//        if (running.get()) {
//            log.info("📊 {}", getConsumerStatus());
//        }
//    }
//
//    @PreDestroy
//    public void cleanup() {
//        log.info("🧹 RedisStreamConsumer 정리 중...");
//        log.info("📊 최종 통계 - {}", getConsumerStatus());
//
//        stopConsumer();
//
//        if (executorService != null) {
//            executorService.shutdown();
//            try {
//                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
//                    executorService.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                executorService.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//        log.info("✅ RedisStreamConsumer 정리 완료");
//    }
//}