package com.example.LAGO.realtime;

import com.example.LAGO.dto.request.TradeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 매매 주문을 Redis Stream에 넣는다. 체결은 AiOrderStreamConsumer가 한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrderStreamPublisher {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.redis.stream.key:orders-stream}")
    private String streamKey;

    public RecordId publish(TradeRequest request) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("userId", String.valueOf(request.getUserId()));
        body.put("stockCode", request.getStockCode());
        body.put("tradeType", request.getTradeType().name());
        body.put("quantity", String.valueOf(request.getQuantity()));
        body.put("price", String.valueOf(request.getPrice()));
        body.put("accountType", String.valueOf(request.getAccountType()));

        RecordId id = redisTemplate.opsForStream().add(
                StreamRecords.mapBacked(body).withStreamKey(streamKey));
        log.info("AI 주문 스트림 적재 stream={} id={} userId={} {} {}주 @{}",
                streamKey, id, request.getUserId(), request.getTradeType(),
                request.getQuantity(), request.getPrice());
        return id;
    }
}
