package com.example.LAGO.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.LAGO.domain.Account;
import com.example.LAGO.domain.MockTrade;
import com.example.LAGO.dto.AccountDto;
import com.example.LAGO.dto.response.TransactionHistoryResponse;
import com.example.LAGO.repository.AccountRepository;
import com.example.LAGO.repository.MockTradeRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * 계좌 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final MockTradeRepository mockTradeRepository;
    
    private static final Integer MOCK_TRADING_INITIAL_BALANCE = 1000000; // 100만원
    private static final Integer HISTORICAL_CHALLENGE_INITIAL_BALANCE = 1000000; // 백만원
    private static final Integer MOCK_TRADING_TYPE = 0;
    private static final Integer HISTORICAL_CHALLENGE_TYPE = 1;

    /**
     * accountId로 계좌 단건 조회
     */
    public AccountDto getAccountById(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "계좌를 찾을 수 없습니다. id=" + accountId));

        // Entity -> DTO 매핑 (DB/Entity 필드명 기준)
        return AccountDto.builder()
                .accountId(account.getAccountId())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .totalAsset(account.getTotalAsset())
                .profit(account.getProfit())
                .profitRate(account.getProfitRate())
                .type(account.getType())
                .build();
    }
    
    /**
     * 신규 사용자용 계좌 두 개 생성 (모의투자 + 역사챌린지)
     */
    @Transactional
    public void createInitialAccountsForUser(Long userId) {
        createMockTradingAccount(userId);
        createHistoricalChallengeAccount(userId);
    }
    
    /**
     * 모의투자 계좌 생성
     */
    @Transactional
    public Account createMockTradingAccount(Long userId) {
        Account mockTradingAccount = Account.builder()
                .userId(userId)
                .balance(MOCK_TRADING_INITIAL_BALANCE)
                .totalAsset(MOCK_TRADING_INITIAL_BALANCE)
                .profit(0)
                .profitRate(0.0)
                .type(MOCK_TRADING_TYPE)
                .build();
        
        return accountRepository.save(mockTradingAccount);
    }
    
    /**
     * 역사챌린지 계좌 생성
     */
    @Transactional
    public Account createHistoricalChallengeAccount(Long userId) {
        Account historicalAccount = Account.builder()
                .userId(userId)
                .balance(HISTORICAL_CHALLENGE_INITIAL_BALANCE)
                .totalAsset(HISTORICAL_CHALLENGE_INITIAL_BALANCE)
                .profit(0)
                .profitRate(0.0)
                .type(HISTORICAL_CHALLENGE_TYPE)
                .build();
        
        return accountRepository.save(historicalAccount);
    }

    /**
     * 사용자 ID로 전체 거래 내역 조회 (모의투자 계좌만)
     * type=0인 계좌의 거래 내역만 조회
     */
    public List<TransactionHistoryResponse> getTransactionHistoryByUserId(Long userId) {
        List<MockTrade> trades = mockTradeRepository.findAllTransactionsByUserId(userId);

        return trades.stream()
                .map(this::convertToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 ID와 종목코드로 특정 종목의 거래 내역 조회 (모의투자 계좌만)
     * type=0인 계좌의 특정 종목 거래 내역만 조회
     */
    public List<TransactionHistoryResponse> getTransactionHistoryByUserIdAndStockCode(Long userId, String stockCode) {
        List<MockTrade> trades = mockTradeRepository.findTransactionsByUserIdAndStockCode(userId, stockCode);

        return trades.stream()
                .map(this::convertToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * AI ID로 AI 봇의 전체 거래 내역 조회 (AI 봇 계좌만)
     * type=2인 계좌의 거래 내역만 조회
     */
    public List<TransactionHistoryResponse> getTransactionHistoryByAiId(Integer aiId) {
        // 1. AI ID로 User ID들 조회
        List<Long> userIds = mockTradeRepository.findUserIdsByAiId(aiId);
        
        if (userIds.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }
        
        // 2. User ID들로 거래 내역 조회
        List<MockTrade> trades = mockTradeRepository.findAllTransactionsByUserIds(userIds);
        
        return trades.stream()
                .map(this::convertToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * AI ID와 종목코드로 AI 봇의 특정 종목 거래 내역 조회 (AI 봇 계좌만)
     * type=2인 계좌의 특정 종목 거래 내역만 조회
     */
    public List<TransactionHistoryResponse> getTransactionHistoryByAiIdAndStockCode(Integer aiId, String stockCode) {
        // 1. AI ID로 User ID들 조회
        List<Long> userIds = mockTradeRepository.findUserIdsByAiId(aiId);
        
        if (userIds.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }
        
        // 2. User ID들과 종목코드로 거래 내역 조회
        List<MockTrade> trades = mockTradeRepository.findTransactionsByUserIdsAndStockCode(userIds, stockCode);
        
        return trades.stream()
                .map(this::convertToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 ID로 역사챌린지 거래 내역 조회 (역사챌린지 계좌만)
     * type=1인 계좌의 068270 종목 거래 내역만 조회
     */
    public List<TransactionHistoryResponse> getHistoricalChallengeTransactionHistoryByUserId(Long userId) {
        List<MockTrade> trades = mockTradeRepository.findHistoricalChallengeTransactionsByUserId(userId);
        
        return trades.stream()
                .map(this::convertToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * MockTrade 엔티티를 TransactionHistoryResponse DTO로 변환
     */
    private TransactionHistoryResponse convertToTransactionHistoryResponse(MockTrade trade) {
        return TransactionHistoryResponse.builder()
                .tradeId(trade.getTradeId())
                .accountId(trade.getAccountId())
                .stockName(trade.getStockInfo() != null ? trade.getStockInfo().getName() : null)
                .stockId(trade.getStockInfo() != null ? trade.getStockInfo().getCode() : null)
                .quantity(trade.getQuantity()) // null 가능
                .buySell(trade.getTradeType() != null ? trade.getTradeType().name() : null)
                .price(trade.getPrice())
                .tradeAt(trade.getTradeAt())
                .isQuiz(trade.getIsQuiz())
                .build();
    }

    /**
     * 퀴즈 보너스 투자금을 사용자의 모의투자 계좌에 추가
     */
    @Transactional
    public void addQuizBonus(Long userId, Integer bonusAmount) {
        Account mockTradingAccount = accountRepository.findByUserIdAndType(userId, MOCK_TRADING_TYPE)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "모의투자 계좌를 찾을 수 없습니다. userId=" + userId));

        // 잔액과 총 자산에 보너스 추가
        mockTradingAccount.setBalance(mockTradingAccount.getBalance() + bonusAmount);
        mockTradingAccount.setTotalAsset(mockTradingAccount.getTotalAsset() + bonusAmount);

        accountRepository.save(mockTradingAccount);
    }
}