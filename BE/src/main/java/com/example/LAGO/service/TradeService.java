package com.example.LAGO.service;

import com.example.LAGO.domain.*;
import com.example.LAGO.dto.request.MockTradeRequest;
import com.example.LAGO.dto.request.TradeRequest;
import com.example.LAGO.dto.response.MockTradeResponse;
import com.example.LAGO.dto.response.TradeResponse;
import com.example.LAGO.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 거래 처리 서비스
 * 
 * 실제 거래 및 모의 거래 처리를 담당하는 서비스
 * 
 * @author 라고할때 팀
 * @version 1.0
 * @since 2025-01-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {
    
    private final MockTradeRepository mockTradeRepository;
    private final StockInfoRepository stockInfoRepository;
    private final UserRepository userRepository;
    
    // 수수료율 (0.25%)
    private static final double COMMISSION_RATE = 0.0025;
    
    /**
     * 수수료 계산
     * @param price 주가
     * @param quantity 수량
     * @return 수수료
     */
    private Integer calculateCommission(Integer price, Integer quantity) {
        long tradeAmount = (long) price * quantity;
        return (int) Math.round(tradeAmount * COMMISSION_RATE);
    }
    
    /**
     * 매수 시 총 지불 금액 계산
     * 총 지불 금액 = (주가 × 수량) + 수수료
     */
    private Integer calculateBuyTotalAmount(Integer price, Integer quantity) {
        int baseAmount = price * quantity;
        int commission = calculateCommission(price, quantity);
        return baseAmount + commission;
    }
    
    /**
     * 매도 시 총 수령 금액 계산 
     * 총 수령 금액 = (주가 × 수량) - 수수료
     */
    private Integer calculateSellTotalAmount(Integer price, Integer quantity) {
        int baseAmount = price * quantity;
        int commission = calculateCommission(price, quantity);
        return baseAmount - commission;
    }
    
    /**
     * 모의 거래 처리 (간소화 버전)
     */
    public MockTradeResponse processMockTrade(MockTradeRequest request) {
        try {
            log.info("모의 거래 처리 시작: stockCode={}, tradeType={}", 
                    request.getStockCode(), request.getTradeType());
            
            // 기본 검증
            if (request.getStockCode() == null || request.getTradeType() == null) {
                return MockTradeResponse.failure("ERROR", "필수 파라미터가 누락되었습니다.");
            }
            
            // 종목 정보 조회
            StockInfo stockInfo = stockInfoRepository.findByCode(request.getStockCode())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid stock code: " + request.getStockCode()));
            
            // 거래 기록 생성 (간소화)
            MockTrade mockTrade = MockTrade.builder()
                    .stockId(stockInfo.getStockInfoId())
                    .tradeType(request.getTradeType())
                    .quantity(request.getQuantity())
                    .price(1000) // 임시 가격
                    .tradeAt(LocalDateTime.now())
                    .build();
            
            // 거래 기록 저장
            mockTradeRepository.save(mockTrade);
            
            log.info("모의 거래 처리 완료: tradeId={}", mockTrade.getTradeId());
            
            return MockTradeResponse.success(
                    mockTrade.getTradeId(), // Long 타입 그대로 사용
                    stockInfo.getCode(), // stockCode
                    stockInfo.getName(), // stockName
                    mockTrade.getQuantity(),
                    mockTrade.getPrice(),
                    mockTrade.getTotalAmount(),
                    0, // commission
                    0, // remainingBalance (임시)
                    mockTrade.getTradeType().name()
            );
            
        } catch (Exception e) {
            log.error("모의 거래 처리 오류: {}", e.getMessage(), e);
            return MockTradeResponse.failure(
                    "ERROR",
                    "거래 처리 실패: " + e.getMessage()
            );
        }
    }
    
    /**
     * 거래 검증 (간소화)
     */
    public boolean validateTrade(MockTradeRequest request) {
        try {
            return request.getStockCode() != null && 
                   request.getTradeType() != null && 
                   request.getQuantity() != null && 
                   request.getQuantity() > 0;
        } catch (Exception e) {
            log.error("거래 검증 오류: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 사용자 거래 실행 (StockController에서 호출)
     * 지침서 명세: API 명세서 기준, 파라미터/반환 구조 임의 변경 금지
     */
    public TradeResponse executeUserTrade(Long userId, TradeRequest request) {
        try {
            log.info("사용자 거래 실행: userId={}, stockCode={}, tradeType={}", 
                    userId, request.getStockCode(), request.getTradeType());
            
            // 기본 검증
            if (userId == null || request.getStockCode() == null || request.getTradeType() == null) {
                return TradeResponse.failure("INVALID_REQUEST", "필수 파라미터가 누락되었습니다.");
            }
            
            // 거래 요청 유효성 검증
            if (!isValidTradeRequest(request)) {
                return TradeResponse.failure(userId, request.getStockCode(), request.getTradeType(),
                        "INVALID_TRADE_REQUEST", "유효하지 않은 거래 요청입니다.");
            }
            
            // 가격 및 수수료 계산
            Integer price = request.getPrice() != null ? request.getPrice() : 75000;
            Integer quantity = request.getQuantity();
            Integer commission = calculateCommission(price, quantity);
            Integer totalAmount;
            
            // 매수/매도에 따른 총 금액 계산
            if (TradeType.BUY.equals(request.getTradeType())) {
                totalAmount = calculateBuyTotalAmount(price, quantity);
            } else {
                totalAmount = calculateSellTotalAmount(price, quantity);
            }
            
            // 종목 정보 조회
            StockInfo stockInfo = stockInfoRepository.findByCode(request.getStockCode())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid stock code: " + request.getStockCode()));
            
            // MockTrade 엔티티 생성
            MockTrade mockTrade = MockTrade.builder()
                    .accountId(request.getAccountType().longValue()) // 계좌 타입 필수
                    .stockId(stockInfo.getStockInfoId()) // stock_info_id 설정
                    .tradeType(request.getTradeType())
                    .quantity(quantity)
                    .price(price)
                    .commission(commission)
                    .tradeAt(LocalDateTime.now())
                    .build();
            
            // 거래 기록 저장
            MockTrade savedTrade = mockTradeRepository.save(mockTrade);
            
            log.info("사용자 거래 완료: tradeId={}", savedTrade.getTradeId());
            
            // 성공 응답 반환
            return TradeResponse.success(
                    savedTrade.getTradeId(),
                    userId,
                    stockInfo.getCode(), // stockCode
                    stockInfo.getName(), // stockName
                    savedTrade.getTradeType(),
                    savedTrade.getQuantity(),
                    savedTrade.getPrice(),
                    savedTrade.getTotalAmount(),
                    savedTrade.getCommission(), // 계산된 수수료
                    0, // tax - 제거됨
                    1000000, // remainingBalance - TODO: 실제 잔고 조회 필요
                    "거래가 성공적으로 처리되었습니다."
            );
            
        } catch (Exception e) {
            log.error("사용자 거래 실행 오류: {}", e.getMessage(), e);
            return TradeResponse.failure(userId, request.getStockCode(), request.getTradeType(),
                    "TRADE_EXECUTION_ERROR", "거래 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    /**
     * TradeRequest 런타임 유효성 검증
     * - stockCode: 공백 불가
     * - tradeType: BUY 또는 SELL
     * - quantity: > 0
     * - price: > 0
     */
    private boolean isValidTradeRequest(TradeRequest request) {
        try {
            if (request == null) return false;
            if (request.getStockCode() == null || request.getStockCode().isBlank()) return false;
            if (request.getTradeType() == null) return false;
            // TradeType enum이므로 이미 BUY 또는 SELL만 가능
            if (request.getQuantity() == null || request.getQuantity() <= 0) return false;
            if (request.getPrice() == null || request.getPrice() <= 0) return false;
            return true;
        } catch (Exception e) {
            log.error("거래 요청 유효성 검증 오류: {}", e.getMessage(), e);
            return false;
        }
    }
}
