package com.example.LAGO.realtime;


import com.example.LAGO.realtime.dto.TickData;
import com.example.LAGO.realtime.dto.TickPushDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

// 실시간 틱 데이터 바로 전송
@Service
@RequiredArgsConstructor
public class RealTimeDataBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendRealTimeData(TickData tickData) {
        // WebSocket으로 프론트엔드에 즉시 전송
        // TickData -> TickPushDto 변환
        TickPushDto pushDto = convertToTickPushDto(tickData);

        /**
         * 특정 종목 구독
         * /topic/stocks/{종목코드}
         */
        messagingTemplate.convertAndSend(
                "/topic/stocks/" + tickData.getCode(),
                pushDto
        );

        /**
         * 전체 종목 구독
         */
        messagingTemplate.convertAndSend("/topic/stocks/all", tickData);
    }

    /**
     * TickData를 TickPushDto로 변환
     */
    private TickPushDto convertToTickPushDto(TickData tickData) {
        return TickPushDto.builder()
                .code(tickData.getCode())
                .date(tickData.getDate())
                .openPrice(tickData.getOpenPrice())
                .highPrice(tickData.getHighPrice())
                .lowPrice(tickData.getLowPrice())
                .closePrice(tickData.getClosePrice())
                .volume(tickData.getVolume())
                .fluctuationRate(tickData.getFluctuationRate())  // 등락율 포함
                .previousDay(tickData.getPreviousDay())          // 전일대비 추가
                .build();
    }

}
