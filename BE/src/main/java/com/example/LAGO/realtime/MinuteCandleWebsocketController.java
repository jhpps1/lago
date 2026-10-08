package com.example.LAGO.realtime;

import com.example.LAGO.dto.StockMinuteDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

// 집계된 1분봉 데이터 웹소켓으로 전달

@Controller
@ConditionalOnProperty(name = "redis.stream.enabled", havingValue = "true", matchIfMissing = false)
public class MinuteCandleWebsocketController {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MinuteCandleWebsocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 1분봉 완성 시 프론트로 실시간 push (MinuteCandleService에서 호출)
    public void sendMinuteCandle(StockMinuteDto dto) {
        // /topic/minute-candle 구독 중인 프론트에게 전송
        messagingTemplate.convertAndSend("/topic/realtime-1m/" + dto.getStockInfoId(), dto);
    }
}
