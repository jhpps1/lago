package com.example.LAGO.kis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController  // KIS WebSocket 실시간 데이터 테스트를 위해 활성화
@RequestMapping("/api/kis/realtime")
public class KisWebSocketController {
    
    private final KisWebSocketService kisWebSocketService;
    
    public KisWebSocketController(KisWebSocketService kisWebSocketService) {
        this.kisWebSocketService = kisWebSocketService;
    }

    // 웹소켓 연결
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startConnections() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            kisWebSocketService.startAll();
            
            response.put("success", true);
            response.put("message", "All WebSocket connections started successfully");
            response.put("connections", kisWebSocketService.getConnectionStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to start connections: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 웹소켓 연결 끊음
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopConnections() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            kisWebSocketService.stopAll();
            
            response.put("success", true);
            response.put("message", "All WebSocket connections stopped successfully");
            response.put("connections", kisWebSocketService.getConnectionStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to stop connections: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * STOCK_INFO 테이블의 모든 주식 종목을 WebSocket 구독하는 API
     * DB에서 자동으로 모든 종목을 조회하여 실시간 데이터 구독
     * 구독 실패한 종목과 성공한 종목을 구분하여 응답
     */
    @PostMapping("/stocks")
    public ResponseEntity<Map<String, Object>> addStocks() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // DB에서 모든 주식 종목 자동 조회 및 구독
            List<String> failedStocks = kisWebSocketService.addAllStocksFromDatabase();
            Map<String, Set<String>> distribution = kisWebSocketService.getUserSubscriptions();
            int totalStocks = kisWebSocketService.getTotalActiveSubscriptions();
            
            response.put("success", true);
            response.put("message", totalStocks + " stocks loaded from database");
            response.put("total", totalStocks);
            response.put("distribution", distribution);
            response.put("failed", failedStocks);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to load stocks from database: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @DeleteMapping("/stocks/{stockCode}")
    public ResponseEntity<Map<String, Object>> removeStock(@PathVariable String stockCode) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean removed = kisWebSocketService.removeStock(stockCode);
            
            if (removed) {
                response.put("success", true);
                response.put("message", "Stock " + stockCode + " removed successfully");
                response.put("subscriptions", kisWebSocketService.getUserSubscriptions());
            } else {
                response.put("success", false);
                response.put("message", "Stock " + stockCode + " not found in subscriptions");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to remove stock: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 사용자가 구독 중인 주식 종목 목록을 조회
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Boolean> connections = kisWebSocketService.getConnectionStatus();
            Map<String, Set<String>> subscriptions = kisWebSocketService.getUserSubscriptions();
            int total = kisWebSocketService.getTotalActiveSubscriptions();
            
            response.put("success", true);
            response.put("connections", connections);
            response.put("subscriptions", subscriptions);
            response.put("total", total);
            response.put("capacity", total + "/40");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/users/{userId}/stocks")
    public ResponseEntity<Map<String, Object>> getUserStocks(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Set<String> userStocks = kisWebSocketService.getUserStocks(userId);
            
            response.put("success", true);
            response.put("userId", userId);
            response.put("stocks", userStocks);
            response.put("count", userStocks.size());
            response.put("capacity", userStocks.size() + "/20");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get user stocks: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
}