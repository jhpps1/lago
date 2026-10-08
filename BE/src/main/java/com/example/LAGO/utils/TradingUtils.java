package com.example.LAGO.utils;

import com.example.LAGO.constants.TradingConstants;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 거래 관련 유틸리티 클래스
 * 지침서 명세: 공통 계산 로직 및 유틸리티 메서드 중앙 집중화
 */
@Slf4j
public final class TradingUtils {

    private TradingUtils() {
        // 유틸리티 클래스는 인스턴스화 금지
    }

    // =========================== 거래 계산 관련 메서드 ===========================

    /**
     * 수수료 계산
     * 
     * @param totalAmount 총 거래금액
     * @return 수수료 (거래금액의 0.2%)
     */
    public static Integer calculateCommission(Integer totalAmount) {
        if (totalAmount == null || totalAmount <= 0) {
            log.warn("잘못된 거래금액으로 수수료 계산 시도: {}", totalAmount);
            return 0;
        }
        
        int commission = (int) (totalAmount * TradingConstants.COMMISSION_RATE);
        log.debug("수수료 계산: 거래금액={}, 수수료={}", totalAmount, commission);
        return commission;
    }

    /**
     * 세금 계산 (매도 시에만 적용)
     * 
     * @param totalAmount 총 거래금액
     * @param tradeType 거래 타입 (BUY/SELL)
     * @return 세금 (매도 시 거래금액의 0.1%, 매수 시 0)
     */
    public static Integer calculateTax(Integer totalAmount, String tradeType) {
        if (totalAmount == null || totalAmount <= 0 || tradeType == null) {
            log.warn("잘못된 파라미터로 세금 계산 시도: amount={}, type={}", totalAmount, tradeType);
            return 0;
        }
        
        if (TradingConstants.TRADE_TYPE_SELL.equals(tradeType)) {
            int tax = (int) (totalAmount * TradingConstants.SELL_TAX_RATE);
            log.debug("매도 세금 계산: 거래금액={}, 세금={}", totalAmount, tax);
            return tax;
        }
        
        log.debug("매수 거래 - 세금 없음: type={}", tradeType);
        return 0;
    }

    /**
     * 총 거래비용 계산 (거래금액만)
     * 
     * @param quantity 거래 수량
     * @param price 거래 단가
     * @param tradeType 거래 타입
     * @return 총 거래비용
     */
    public static Integer calculateTotalCost(Integer quantity, Integer price, String tradeType) {
        if (quantity == null || price == null || quantity <= 0 || price <= 0) {
            log.warn("잘못된 거래 정보로 총비용 계산 시도: quantity={}, price={}", quantity, price);
            return 0;
        }
        
        int totalAmount = quantity * price;
        
        log.debug("총 거래비용 계산: 수량={}, 단가={}, 거래금액={}", 
                quantity, price, totalAmount);
        
        return totalAmount;
    }

    /**
     * 총 거래수익 계산 (매도 시: 거래금액만)
     * 
     * @param quantity 거래 수량
     * @param price 거래 단가  
     * @param tradeType 거래 타입
     * @return 총 거래수익 (실제 받는 금액)
     */
    public static Integer calculateTotalRevenue(Integer quantity, Integer price, String tradeType) {
        if (quantity == null || price == null || quantity <= 0 || price <= 0) {
            log.warn("잘못된 거래 정보로 총수익 계산 시도: quantity={}, price={}", quantity, price);
            return 0;
        }
        
        int totalAmount = quantity * price;
        
        log.debug("총 거래수익 계산: 수량={}, 단가={}, 거래금액={}", 
                quantity, price, totalAmount);
        
        return totalAmount;
    }

    /**
     * 수익률 계산
     * 
     * @param currentValue 현재 평가금액
     * @param totalCost 총 매수금액
     * @return 수익률 (%)
     */
    public static Float calculateProfitRate(Integer currentValue, Integer totalCost) {
        if (currentValue == null || totalCost == null || totalCost <= 0) {
            log.warn("잘못된 파라미터로 수익률 계산 시도: current={}, cost={}", currentValue, totalCost);
            return 0.0f;
        }
        
        float profitRate = ((float) (currentValue - totalCost) / totalCost) * 100;
        log.debug("수익률 계산: 현재가치={}, 매수금액={}, 수익률={}%", currentValue, totalCost, profitRate);
        
        return profitRate;
    }

    // =========================== 거래 검증 관련 메서드 ===========================

    /**
     * 매매 요청 유효성 검증
     * 
     * @param stockCode 종목 코드
     * @param quantity 거래 수량
     * @param price 거래 단가
     * @param tradeType 거래 타입
     * @return 유효성 여부
     */
    public static boolean validateTradeRequest(String stockCode, Integer quantity, Integer price, String tradeType) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            log.error("종목 코드가 비어있음: {}", stockCode);
            return false;
        }
        
        if (quantity == null || quantity <= 0 || quantity > TradingConstants.MAX_TRADE_QUANTITY) {
            log.error("잘못된 거래 수량: {}", quantity);
            return false;
        }
        
        if (price == null || price <= 0) {
            log.error("잘못된 거래 단가: {}", price);
            return false;
        }
        
        if (!TradingConstants.TRADE_TYPE_BUY.equals(tradeType) && 
            !TradingConstants.TRADE_TYPE_SELL.equals(tradeType)) {
            log.error("잘못된 거래 타입: {}", tradeType);
            return false;
        }
        
        int totalAmount = quantity * price;
        if (totalAmount < TradingConstants.MIN_TRADE_AMOUNT) {
            log.error("최소 거래금액 미달: {} < {}", totalAmount, TradingConstants.MIN_TRADE_AMOUNT);
            return false;
        }
        
        log.debug("거래 요청 검증 성공: stockCode={}, quantity={}, price={}, type={}", 
                stockCode, quantity, price, tradeType);
        return true;
    }

    /**
     * 잔액 충분성 검증
     * 
     * @param balance 계좌 잔액
     * @param requiredAmount 필요 금액
     * @return 잔액 충분 여부
     */
    public static boolean isBalanceSufficient(Integer balance, Integer requiredAmount) {
        if (balance == null || requiredAmount == null) {
            log.warn("잔액 검증 파라미터 누락: balance={}, required={}", balance, requiredAmount);
            return false;
        }
        
        boolean sufficient = balance >= requiredAmount;
        log.debug("잔액 충분성 검증: 보유={}, 필요={}, 결과={}", balance, requiredAmount, sufficient);
        
        return sufficient;
    }

    // =========================== 로그 관련 유틸리티 ===========================

    /**
     * 거래 시작 로그 출력
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @param tradeType 거래 타입
     * @param quantity 거래 수량
     * @param price 거래 단가
     */
    public static void logTradeStart(Long userId, String stockCode, String tradeType, 
                                   Integer quantity, Integer price) {
        log.info("거래 시작: userId={}, stockCode={}, tradeType={}, quantity={}, price={}", 
                userId, stockCode, tradeType, quantity, price);
    }

    /**
     * 거래 완료 로그 출력
     * 
     * @param tradeId 거래 ID
     * @param userId 사용자 ID
     * @param remainingBalance 거래 후 잔액
     */
    public static void logTradeComplete(Long tradeId, Integer userId, Integer remainingBalance) {
        log.info("거래 완료: tradeId={}, userId={}, remainingBalance={}", 
                tradeId, userId, remainingBalance);
    }

    /**
     * 에러 로그 출력
     * 
     * @param operation 작업명
     * @param userId 사용자 ID
     * @param error 에러 메시지
     * @param exception 예외 객체
     */
    public static void logError(String operation, Integer userId, String error, Exception exception) {
        log.error("{} 실행 중 오류 발생: userId={}, error={}", operation, userId, error, exception);
    }

    // =========================== 포맷팅 관련 유틸리티 ===========================

    /**
     * 금액 포맷팅 (천 단위 구분)
     * 
     * @param amount 금액
     * @return 포맷팅된 문자열
     */
    public static String formatAmount(Integer amount) {
        if (amount == null) {
            return "0원";
        }
        return String.format("%,d원", amount);
    }

    /**
     * 수익률 포맷팅
     * 
     * @param profitRate 수익률
     * @return 포맷팅된 문자열
     */
    public static String formatProfitRate(Float profitRate) {
        if (profitRate == null) {
            return "0.00%";
        }
        return String.format("%.2f%%", profitRate);
    }

    /**
     * 거래 시간 포맷팅
     * 
     * @param dateTime 거래 시간
     * @return 포맷팅된 문자열
     */
    public static String formatTradeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // =========================== 거래 메시지 생성 ===========================

    /**
     * 거래 완료 메시지 생성
     * 
     * @param tradeType 거래 타입
     * @param stockName 종목명
     * @param quantity 거래 수량
     * @return 완료 메시지
     */
    public static String createTradeCompleteMessage(String tradeType, String stockName, Integer quantity) {
        if (TradingConstants.TRADE_TYPE_BUY.equals(tradeType)) {
            return String.format("%s %,d주 매수 주문이 체결되었습니다", stockName, quantity);
        } else if (TradingConstants.TRADE_TYPE_SELL.equals(tradeType)) {
            return String.format("%s %,d주 매도 주문이 체결되었습니다", stockName, quantity);
        }
        return "거래가 완료되었습니다";
    }

    /**
     * 에러 메시지 생성 (상세 정보 포함)
     * 
     * @param baseMessage 기본 메시지
     * @param details 상세 정보
     * @return 완성된 에러 메시지
     */
    public static String createErrorMessage(String baseMessage, String details) {
        if (details == null || details.trim().isEmpty()) {
            return baseMessage;
        }
        return String.format("%s: %s", baseMessage, details);
    }
}
