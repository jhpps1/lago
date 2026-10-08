package com.example.LAGO.kis;

import com.example.LAGO.realtime.KisRealTimeDataProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.example.LAGO.kis.KisAuthClient;
@Slf4j
public class KisWebSocketClient {
    private final KisAuthClient kis;
    private final KisRealTimeDataProcessor dataProcessor;

    // ▼▼ NEW: 세션/승인키/상태를 보관해서 "연결 1개" 재사용 ▼▼
    private volatile WebSocketSession session;          // ★ 재사용할 세션
    private volatile String approvalKey;                // ★ 재사용할 승인키
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private KisAuthClient.Env currentEnv;
    // ▲▲ NEW ▲▲


    public KisWebSocketClient(KisAuthClient kis,
                             KisRealTimeDataProcessor dataProcessor) {
        this.kis = kis;
        this.dataProcessor = dataProcessor;
    }

    // =========================
    // NEW ①: 연결만 담당 (1회)
    // =========================
    public synchronized void connect(String envBase) throws Exception {
        if (session != null && session.isOpen()) {
            return; // 이미 연결됨 → 재사용
        }
        if (connecting.getAndSet(true)) {
            return; // 동시 connect 방지
        }

        this.currentEnv = "prod".equalsIgnoreCase(envBase)
                ? KisAuthClient.Env.PROD : KisAuthClient.Env.PAPER;

        // 승인키 1회 발급 → 재사용
        this.approvalKey = kis.getApprovalKey(currentEnv);

        String wsUrl = (currentEnv == KisAuthClient.Env.PROD)
                ? "ws://ops.koreainvestment.com:21000"
                : "ws://ops.koreainvestment.com:31000";

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();

        TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession s) throws Exception {
                session = s;
                log.info("[KIS WS] Connected: {}", s.getUri());
            }

            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                try {
                    String payload = message.getPayload();
                    log.debug("[KIS WS] RX: {}", payload);
                    dataProcessor.processStockData(payload);
                } catch (Exception e) {
                    log.error("Failed to handle KIS WebSocket message", e);
                }
            }
        };

        // 블로킹으로 연결 완료 대기 (예: .get()) — 환경에 따라 논블로킹도 가능
        client.execute(handler, headers, URI.create(wsUrl)).get();

        connecting.set(false);
    }

    // =========================
    // NEW ②: 상태 체크
    // =========================
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    // ==========================================
    // NEW ③: 같은 세션에서 여러 종목을 "배치" 구독
    // ==========================================
    public synchronized void subscribeBatch(String trId, List<String> trKeys) throws Exception {
        if (!isOpen()) throw new IllegalStateException("WS not connected");

        // 공급사 레이트 리밋/제한 회피용으로 약간의 간격과 배치 전송
        int batchSize = 20; // 필요하면 10~20로 조정
        for (int i = 0; i < trKeys.size(); i += batchSize) {
            List<String> batch = trKeys.subList(i, Math.min(i + batchSize, trKeys.size()));
            log.info("[KIS WS] Subscribing batch ({}): {}", batch.size(), batch);

            for (String key : batch) {
                String payload = """
                {"header":{"approval_key":"%s","custtype":"P","tr_type":"1","content-type":"utf-8"},
                 "body":{"input":{"tr_id":"%s","tr_key":"%s"}}}
                """.formatted(approvalKey, trId, key);

                log.info("[KIS WS] SUBSCRIBE tr_id={}, tr_key={}", trId, key);
                session.sendMessage(new TextMessage(payload));

                try { Thread.sleep(30); } catch (InterruptedException ignored) {} // 소폭 딜레이
            }
        }
    }

    // ======================================================
    // CHANGED: 기존 메서드는 "연결+단일 구독"으로 위임(새 연결 만들지 않음)
    // ======================================================
    public void connectAndSubscribe(String trId, String trKey, String envBase) throws Exception {
        connect(envBase);                                  // 기존: 매번 새 연결 ❌ → 이제 재사용 ✅
        subscribeBatch(trId, java.util.List.of(trKey));    // 단일 키도 배치 API로 전송
    }

    // (옵션) 종료
    public synchronized void close() {
        try {
            if (session != null && session.isOpen()) session.close();
        } catch (Exception ignore) {}
        session = null;
    }

//    public void connectAndSubscribe(String trId, String trKey, String envBase) throws Exception {
//        // KisAuthClient.java:54의 Env enum 사용
//        KisAuthClient.Env env = "prod".equals(envBase) ?
//            KisAuthClient.Env.PROD : KisAuthClient.Env.PAPER;
//        String approval = kis.getApprovalKey(env);
//
//        // KIS 웹소켓 URL - 환경별 분기
//        String wsUrl = env == KisAuthClient.Env.PROD ?
//            "ws://ops.koreainvestment.com:21000" :
//            "ws://ops.koreainvestment.com:31000";
//
//        StandardWebSocketClient client = new StandardWebSocketClient();
//        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
//        // 필요시 헤더 세팅
//
//        TextWebSocketHandler handler = new TextWebSocketHandler() {
//            @Override
//            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//                String subscribe = """
//                {"header":{"approval_key":"%s","custtype":"P","tr_type":"1","content-type":"utf-8"},
//                 "body":{"input":{"tr_id":"%s","tr_key":"%s"}}}
//                """.formatted(approval, trId, trKey);
//
//                // ✅ 추가한 로그
//                log.info("[KIS WS] Subscribing tr_id={}, tr_key={}", trId, trKey);
//                log.info("[KIS WS] SUBSCRIBE frame: {}", subscribe);
//
//                session.sendMessage(new TextMessage(subscribe));
//            }
//            @Override
//            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
//                try {
//                    String payload = message.getPayload();
//                    System.out.println("Received KIS WebSocket message: " + payload);
//
//                    // 기존 KisRealTimeDataProcessor를 사용하여 원시 메시지 처리
//                    // KIS 원시 메시지 형식: "0|H0STCNT0|004|005930^123929^73100^..."
//                    dataProcessor.processStockData(payload);
//
//                } catch (Exception e) {
//                    System.err.println("Failed to handle KIS WebSocket message: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//        };
//
//        client.execute(handler, headers, URI.create(wsUrl));
//    }
}
