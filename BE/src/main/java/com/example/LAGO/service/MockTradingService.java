package com.example.LAGO.service;

import com.example.LAGO.constants.TradingConstants;
import com.example.LAGO.domain.*;
import com.example.LAGO.dto.request.MockTradeRequest;
import com.example.LAGO.dto.response.MockTradeResponse;
import com.example.LAGO.repository.*;
import com.example.LAGO.utils.TradingUtils;
import com.example.LAGO.realtime.RealtimeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.Optional;

/**
 * 모의투자 거래 서비스
 * 
 * 연동된 EC2 DB 기반:
 * - MOCK_TRADE 테이블: 거래 내역 저장
 * - ACCOUNTS 테이블: 계좌 잔액/자산 관리  
 * - STOCK_HOLDING 테이블: 보유 주식 관리
 * - STOCK_INFO 테이블: 종목 정보 조회
 * - USERS 테이블: 사용자 정보 검증
 * 
 * Java 21 Virtual Thread 활용:
 * - 비동기 거래 처리로 성능 최적화
 * - 동시성 처리로 사용자 경험 향상
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MockTradingService {

    // ======================== Repository 의존성 ========================
    
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StockInfoRepository stockInfoRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final MockTradeRepository mockTradeRepository;
    private final RealtimeDataService realtimeDataService;
    private final TicksRepository ticksRepository;

    // ======================== Virtual Thread Executor ========================
    
    /**
     * Java 21 Virtual Thread를 활용한 비동기 처리
     * - 경량 스레드로 높은 동시성 지원
     * - 블로킹 I/O 작업에 최적화
     */
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ======================== 매수 거래 처리 ========================

    /**
     * 모의투자 매수 주문 처리
     * 
     * 처리 과정:
     * 1. 사용자 및 계좌 유효성 검증
     * 2. 종목 정보 조회 및 검증
     * 3. 거래 요청 유효성 검증 (수량, 가격 등)
     * 4. 계좌 잔액 충분성 검증
     * 5. Virtual Thread로 비동기 거래 처리
     * 6. 계좌 잔액 차감 및 보유 주식 추가/업데이트
     * 7. 거래 내역 저장
     * 
     * @param userId 사용자 ID
     * @param request 매수 요청 정보
     * @return 거래 결과 DTO
     * @throws IllegalArgumentException 잘못된 요청 파라미터
     * @throws RuntimeException 거래 처리 실패
     */
    @Transactional
    public MockTradeResponse processBuyOrder(Long userId, MockTradeRequest request) {
        return processBuyOrder(userId, request, null); // 기본 계좌 사용
    }

    @Transactional
    public MockTradeResponse processBuyOrder(Long userId, MockTradeRequest request, Integer accountType) {
        log.info("매수 주문 처리 시작: userId={}, stockCode={}, quantity={}, price={}, accountType={}", 
                userId, request.getStockCode(), request.getQuantity(), request.getPrice(), accountType);

        try {
            // 1. 사용자 존재 여부 검증
            User user = getUserOrThrow(userId);
            
            // 2. 계좌 정보 조회 (accountType 지정 또는 기본 계좌)
            Account account = getAccountOrThrow(userId, accountType);
            
            // 3. 주식 정보 조회 및 검증
            StockInfo stockInfo = getStockInfoOrThrow(request.getStockCode());
            
            // 4. 거래 요청 유효성 검증
            validateTradeRequest(request, TradingConstants.TRADE_TYPE_BUY);
            
            // 5. 실제 거래 가격 결정 (현재가 또는 지정가)
            Integer executedPrice = determineExecutedPrice(request.getPrice(), stockInfo);
            
            // 6. 총 거래 비용 계산 (거래금액 + 수수료)
            Integer totalCost = TradingUtils.calculateTotalCost(
                request.getQuantity(), 
                executedPrice, 
                TradingConstants.TRADE_TYPE_BUY
            );
            
            // 7. 계좌 잔액 충분성 검증
            validateSufficientBalance(account, totalCost);
            
            // 8. 동기 거래 처리 (트랜잭션 보장을 위해)
            try {
                // 계좌 잔액 차감
                updateAccountForBuy(account, totalCost);
                
                // 거래 내역 저장
                MockTrade mockTrade = createAndSaveMockTrade(
                    account, request.getStockCode(), TradeType.BUY,
                    request.getQuantity(), executedPrice, totalCost
                );
                
                // 보유 주식 추가/업데이트
                updateStockHoldingForBuy(account, request, executedPrice, totalCost);
                
                log.info("매수 주문 처리 완료: userId={}, stockCode={}, quantity={}, totalCost={}", 
                        userId, request.getStockCode(), request.getQuantity(), totalCost);
                
                // 성공 응답 생성
                return MockTradeResponse.success(
                    mockTrade.getTradeId(),
                    request.getStockCode(),
                    stockInfo.getName(),
                    request.getQuantity(),
                    executedPrice,
                    totalCost,
                    0, // 수수료 없음
                    account.getBalance(),
                    TradingConstants.TRADE_TYPE_BUY
                );
                
            } catch (Exception e) {
                log.error("매수 주문 처리 중 오류 발생: userId={}", userId, e);
                throw new RuntimeException("매수 주문 처리 실패", e);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("매수 주문 요청 오류: userId={}, error={}", userId, e.getMessage());
            return MockTradeResponse.failure(request.getStockCode(), e.getMessage());
        } catch (Exception e) {
            log.error("매수 주문 처리 실패: userId={}, stockCode={}", userId, request.getStockCode(), e);
            return MockTradeResponse.failure(request.getStockCode(), "거래 처리 중 오류가 발생했습니다");
        }
    }

    // ======================== 매도 거래 처리 ========================

    /**
     * 모의투자 매도 주문 처리
     * 
     * 처리 과정:
     * 1. 사용자 및 계좌 유효성 검증
     * 2. 종목 정보 조회 및 검증
     * 3. 보유 주식 수량 충분성 검증
     * 4. 거래 요청 유효성 검증
     * 5. Virtual Thread로 비동기 거래 처리
     * 6. 계좌 잔액 증가 및 보유 주식 감소/삭제
     * 7. 거래 내역 저장
     * 
     * @param userId 사용자 ID
     * @param request 매도 요청 정보
     * @return 거래 결과 DTO
     * @throws IllegalArgumentException 잘못된 요청 파라미터
     * @throws RuntimeException 거래 처리 실패
     */
    @Transactional
    public MockTradeResponse processSellOrder(Long userId, MockTradeRequest request) {
        return processSellOrder(userId, request, null); // 기본 계좌 사용
    }

    @Transactional
    public MockTradeResponse processSellOrder(Long userId, MockTradeRequest request, Integer accountType) {
        log.info("매도 주문 처리 시작: userId={}, stockCode={}, quantity={}, price={}, accountType={}", 
                userId, request.getStockCode(), request.getQuantity(), request.getPrice(), accountType);

        try {
            // 1. 사용자 존재 여부 검증
            User user = getUserOrThrow(userId);
            
            // 2. 계좌 정보 조회 (accountType 지정 또는 기본 계좌)
            Account account = getAccountOrThrow(userId, accountType);
            
            // 3. 주식 정보 조회
            StockInfo stockInfo = getStockInfoOrThrow(request.getStockCode());
            
            // 4. 보유 주식 검증
            StockHolding holding = getStockHoldingOrThrow(account.getAccountId(), request.getStockCode());
            validateSufficientHolding(holding, request.getQuantity());
            
            // 5. 거래 요청 유효성 검증
            validateTradeRequest(request, TradingConstants.TRADE_TYPE_SELL);
            
            // 6. 실제 거래 가격 결정
            Integer executedPrice = determineExecutedPrice(request.getPrice(), stockInfo);
            
            // 7. 총 매도 수익 계산 (거래금액 - 수수료 - 세금)
            Integer totalRevenue = TradingUtils.calculateTotalRevenue(
                request.getQuantity(), 
                executedPrice, 
                TradingConstants.TRADE_TYPE_SELL
            );
            
            // 8. 동기 거래 처리 (트랜잭션 보장을 위해)
            try {
                // 계좌 잔액 증가
                updateAccountForSell(account, totalRevenue);
                
                // 거래 내역 저장
                MockTrade mockTrade = createAndSaveMockTrade(
                    account, request.getStockCode(), TradeType.SELL,
                    request.getQuantity(), executedPrice, totalRevenue
                );
                
                // 보유 주식 감소/삭제
                updateStockHoldingForSell(holding, request.getQuantity());
                
                log.info("매도 주문 처리 완료: userId={}, stockCode={}, quantity={}, totalRevenue={}", 
                        userId, request.getStockCode(), request.getQuantity(), totalRevenue);
                
                // 성공 응답 생성
                return MockTradeResponse.success(
                    mockTrade.getTradeId(),
                    request.getStockCode(),
                    stockInfo.getName(),
                    request.getQuantity(),
                    executedPrice,
                    totalRevenue,
                    0, // 수수료 없음
                    account.getBalance(),
                    TradingConstants.TRADE_TYPE_SELL
                );
                    
            } catch (Exception e) {
                log.error("매도 주문 처리 중 오류 발생: userId={}", userId, e);
                throw new RuntimeException("매도 주문 처리 실패", e);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("매도 주문 요청 오류: userId={}, error={}", userId, e.getMessage());
            return MockTradeResponse.failure(request.getStockCode(), e.getMessage());
        } catch (Exception e) {
            log.error("매도 주문 처리 실패: userId={}, stockCode={}", userId, request.getStockCode(), e);
            return MockTradeResponse.failure(request.getStockCode(), "거래 처리 중 오류가 발생했습니다");
        }
    }

    // ======================== 유효성 검증 메서드들 ========================

    /**
     * 거래 요청 기본 유효성 검증
     */
    private void validateTradeRequest(MockTradeRequest request, String tradeType) {
        if (!TradingUtils.validateTradeRequest(
                request.getStockCode(), 
                request.getQuantity(), 
                request.getPrice() != null ? request.getPrice() : 1, // 현재가 거래시 임시값
                tradeType)) {
            throw new IllegalArgumentException("잘못된 거래 요청입니다");
        }
    }

    /**
     * 계좌 잔액 충분성 검증
     */
    private void validateSufficientBalance(Account account, Integer requiredAmount) {
        if (!TradingUtils.isBalanceSufficient(account.getBalance(), requiredAmount)) {
            throw new IllegalArgumentException(
                String.format("잔액이 부족합니다. 필요: %s, 보유: %s", 
                    TradingUtils.formatAmount(requiredAmount), 
                    TradingUtils.formatAmount(account.getBalance()))
            );
        }
    }

    // ======================== 조회 메서드들 (예외 발생) ========================

    /**
     * 사용자 조회 (예외 발생)
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다"));
    }

    /**
     * 계좌 조회 (예외 발생) - accountId 지정 또는 기본 계좌 사용
     * 
     * @param userId 사용자 ID
     * @param accountId 계좌 ID (null이면 기본 계좌)
     * @return 계좌 정보
     */
    private Account getAccountOrThrow(Long userId, Integer accountId) {
        if (accountId != null) {
            // 특정 계좌 타입으로 조회
            // accountId 0 = 실시간 모의투자 계좌 (type=0)
            // accountId 1 = 역사챌린지 계좌 (type=1)  
            // accountId 2 = 자동매매봇 계좌 (type=2)
            return accountRepository.findByUserIdAndType(userId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("계좌를 찾을 수 없습니다. userId: %d, accountType: %d (%s)", 
                        userId, accountId, getAccountTypeName(accountId))));
        } else {
            // 기본 계좌(실시간 모의투자 계좌, type=0) 조회
            return accountRepository.findByUserIdAndType(userId, 0)
                .orElseThrow(() -> new IllegalArgumentException("실시간 모의투자 계좌를 찾을 수 없습니다"));
        }
    }
    
    /**
     * 계좌 타입명 반환
     */
    private String getAccountTypeName(Integer accountType) {
        return switch (accountType) {
            case 0 -> "실시간 모의투자";
            case 1 -> "역사챌린지";
            case 2 -> "자동매매봇";
            default -> "알 수 없는 계좌";
        };
    }

    /**
     * 주식 정보 조회 (예외 발생)
     */
    private StockInfo getStockInfoOrThrow(String stockCode) {
        return stockInfoRepository.findByCode(stockCode)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목입니다"));
    }

    /**
     * 보유 주식 조회 (예외 발생)
     */
    private StockHolding getStockHoldingOrThrow(Long accountId, String stockCode) {
        return stockHoldingRepository.findByAccountIdAndStockCode(accountId, stockCode)
            .orElseThrow(() -> new IllegalArgumentException("보유하지 않은 종목입니다"));
    }

    // ======================== 추가 검증 메서드들 ========================

    /**
     * 보유 주식 수량 충분성 검증
     */
    private void validateSufficientHolding(StockHolding holding, Integer sellQuantity) {
        if (holding.getQuantity() < sellQuantity) {
            throw new IllegalArgumentException(
                String.format("보유 수량이 부족합니다. 보유: %d주, 매도요청: %d주", 
                    holding.getQuantity(), sellQuantity)
            );
        }
    }

    // ======================== 업데이트 메서드들 ========================

    /**
     * 매수 시 계좌 정보 업데이트
     */
    private void updateAccountForBuy(Account account, Integer totalCost) {
        account.setBalance(account.getBalance() - totalCost);
        account.setTotalAsset(account.getTotalAsset()); // 총 자산은 동일 (현금 -> 주식)
        accountRepository.save(account);
        
        log.debug("매수 후 계좌 업데이트 완료: accountId={}, newBalance={}", 
                account.getAccountId(), account.getBalance());
    }

    /**
     * 매도 시 계좌 정보 업데이트
     */
    private void updateAccountForSell(Account account, Integer totalRevenue) {
        account.setBalance(account.getBalance() + totalRevenue);
        accountRepository.save(account);
        
        log.debug("매도 후 계좌 업데이트 완료: accountId={}, newBalance={}", 
                account.getAccountId(), account.getBalance());
    }

    /**
     * 거래 내역 생성 및 저장
     */
    private MockTrade createAndSaveMockTrade(Account account, String stockCode, TradeType tradeType,
                                           Integer quantity, Integer executedPrice, Integer totalAmount) {
        // 종목 정보 조회
        StockInfo stockInfo = stockInfoRepository.findByCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid stock code: " + stockCode));
        
        MockTrade mockTrade = MockTrade.builder()
            .accountId(account.getAccountId())
            .stockId(stockInfo.getStockInfoId())
            .tradeType(tradeType)
            .quantity(quantity)
            .price(executedPrice)
            .tradeAt(LocalDateTime.now())
            .build();
            
        return mockTradeRepository.save(mockTrade);
    }

    /**
     * 매수 시 보유 주식 추가/업데이트
     */
    private void updateStockHoldingForBuy(Account account, MockTradeRequest request, 
                                        Integer executedPrice, Integer totalCost) {
        Optional<StockHolding> existingHolding = 
            stockHoldingRepository.findByAccountIdAndStockCode(account.getAccountId(), request.getStockCode());
            
        if (existingHolding.isPresent()) {
            // 기존 보유 주식이 있는 경우 수량과 총 매수금액 업데이트
            StockHolding holding = existingHolding.get();
            Integer newQuantity = holding.getQuantity() + request.getQuantity();
            Integer newTotalPrice = holding.getTotalPrice() + totalCost;
            
            holding.setQuantity(newQuantity);
            holding.setTotalPrice(newTotalPrice);
            
            stockHoldingRepository.save(holding);
        } else {
            // 종목 정보 조회
            StockInfo stockInfo = stockInfoRepository.findByCode(request.getStockCode())
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + request.getStockCode()));
                
            // 신규 보유 주식 생성
            StockHolding newHolding = StockHolding.builder()
                .accountId(account.getAccountId())
                .stockInfoId(stockInfo.getStockInfoId())
                .quantity(request.getQuantity())
                .totalPrice(totalCost)
                .build();
                
            stockHoldingRepository.save(newHolding);
        }
    }

    /**
     * 매도 시 보유 주식 감소/삭제
     */
    private void updateStockHoldingForSell(StockHolding holding, Integer sellQuantity) {
        Integer remainingQuantity = holding.getQuantity() - sellQuantity;
        
        if (remainingQuantity <= 0) {
            // 전량 매도시 보유 주식 삭제
            stockHoldingRepository.delete(holding);
            log.debug("전량 매도로 보유 주식 삭제: holdingId={}", holding.getHoldingId());
        } else {
            // 부분 매도시 수량 감소 및 총 매수금액 비례 조정
            Integer newTotalPrice = (holding.getTotalPrice() * remainingQuantity) / holding.getQuantity();
            
            holding.setQuantity(remainingQuantity);
            holding.setTotalPrice(newTotalPrice);
            
            stockHoldingRepository.save(holding);
            log.debug("부분 매도로 보유 주식 업데이트: holdingId={}, newQuantity={}", 
                    holding.getHoldingId(), remainingQuantity);
        }
    }

    // ======================== 유틸리티 메서드들 ========================

    /**
     * 실제 체결 가격 결정
     * - 지정가 주문: 요청 가격 사용
     * - 시장가 주문: Redis에서 웹소켓 실시간 가격 → TICKS 테이블 마지막 가격 순으로 조회
     */
    private Integer determineExecutedPrice(Integer requestPrice, StockInfo stockInfo) {
        if (requestPrice != null && requestPrice > 0) {
            return requestPrice; // 지정가 주문
        } else {
            // 시장가 주문 - 1차: Redis에서 웹소켓 실시간 가격 조회
            Integer realtimePrice = realtimeDataService.getLatestPrice(stockInfo.getCode());
            if (realtimePrice != null && realtimePrice > 0) {
                log.debug("종목 {} 웹소켓 실시간 가격 사용: {}원", stockInfo.getCode(), realtimePrice);
                return realtimePrice;
            }
            
            // 2차: TICKS 테이블에서 마지막 확인된 가격 조회
            try {
                Integer lastKnownPrice = ticksRepository.findLatestClosePriceByStockCode(stockInfo.getCode());
                if (lastKnownPrice != null && lastKnownPrice > 0) {
                    log.info("종목 {} 웹소켓 실시간 가격 조회 실패, TICKS 테이블 마지막 가격 사용: {}원", 
                            stockInfo.getCode(), lastKnownPrice);
                    return lastKnownPrice;
                }
            } catch (Exception e) {
                log.error("종목 {} TICKS 테이블 조회 실패", stockInfo.getCode(), e);
            }
            
            // 3차: StockInfo 테이블에는 가격 정보가 없으므로 생략
            
            // 최후: StockInfo에도 가격이 없으면 기본값
            log.error("종목 {} 모든 가격 정보 없음, 기본 가격 사용", stockInfo.getCode());
            return 100000000; // 최후의 기본 가격 (1억원)
        }
    }
}
