package com.example.LAGO.repository;

import com.example.LAGO.domain.Account;
import com.example.LAGO.domain.MockTrade;
import com.example.LAGO.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MockTradeRepository extends JpaRepository<MockTrade, Long> {
    @Query("SELECT COUNT(mt) FROM MockTrade mt WHERE mt.accountId = :accountId")
    Long countByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT AVG(mt.price * mt.quantity) FROM MockTrade mt WHERE mt.accountId = :accountId")
    Double findAvgTradeValue(@Param("accountId") Long accountId);

    /**
     * 계좌별 현재 보유 종목 정보 조회
     * 매수/매도를 계산해서 현재 보유량이 0보다 큰 종목만 반환
     */
    @Query("""
        SELECT mt.stockId,
               SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.quantity ELSE -mt.quantity END) as currentQuantity,
               SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.price * mt.quantity ELSE -mt.price * mt.quantity END) as totalPurchaseAmount
        FROM MockTrade mt 
        WHERE mt.accountId = :accountId
        GROUP BY mt.stockId
        HAVING SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.quantity ELSE -mt.quantity END) > 0
        """)
    List<Object[]> findCurrentHoldingsByAccountId(@Param("accountId") Long accountId);

    /**
     * 특정 계좌 타입의 계좌별 현재 보유 종목 정보 조회
     * 매수/매도를 계산해서 현재 보유량이 0보다 큰 종목만 반환
     */
    @Query("""
        SELECT mt.stockId,
               SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.quantity ELSE -mt.quantity END) as currentQuantity,
               SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.price * mt.quantity ELSE -mt.price * mt.quantity END) as totalPurchaseAmount
        FROM MockTrade mt 
        JOIN Account a ON mt.accountId = a.accountId
        WHERE mt.accountId = :accountId AND a.type = :accountType
        GROUP BY mt.stockId
        HAVING SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.quantity ELSE -mt.quantity END) > 0
        """)
    List<Object[]> findCurrentHoldingsByAccountIdAndType(@Param("accountId") Long accountId, @Param("accountType") Integer accountType);

    /**
     * 유저+계좌타입 기준으로 보유 종목 합산 조회 (유저의 해당타입 모든 계좌 합산)
     * 매수/매도를 계산해서 현재 보유량이 0보다 큰 종목만 반환
     */
    @Query("""
        SELECT mt.stockId,
               SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.quantity ELSE -mt.quantity END) as currentQuantity
        FROM MockTrade mt
        JOIN Account a ON mt.accountId = a.accountId
        WHERE a.userId = :userId AND a.type = :accountType
        GROUP BY mt.stockId
        HAVING SUM(CASE WHEN mt.tradeType = 'BUY' THEN mt.quantity ELSE -mt.quantity END) > 0
        """)
    List<Object[]> findCurrentHoldingsByUserIdAndType(@Param("userId") Long userId, @Param("accountType") Integer accountType);

    /**
     * 특정 사용자의 모의투자 계좌(type=0)에 대한 모든 거래 내역 조회
     * StockInfo와 조인하여 주식명과 코드 정보도 함께 가져옴
     * quiz 데이터(stock_id가 null인 경우)도 포함
     */
    @Query("""
        SELECT mt FROM MockTrade mt
        LEFT JOIN FETCH mt.stockInfo si
        WHERE mt.accountId IN (
            SELECT a.accountId FROM Account a 
            WHERE a.userId = :userId AND a.type = 0
        )
        ORDER BY mt.tradeAt DESC
        """)
    List<MockTrade> findAllTransactionsByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 모의투자 계좌(type=0)에 대한 특정 종목의 거래 내역 조회
     * 종목코드로 필터링하여 해당 종목의 거래 내역만 조회
     */
    @Query("""
        SELECT mt FROM MockTrade mt
        LEFT JOIN FETCH mt.stockInfo si
        JOIN mt.account a
        WHERE a.userId = :userId AND a.type = 0
        AND si.code = :stockCode
        ORDER BY mt.tradeAt DESC
        """)
    List<MockTrade> findTransactionsByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);

    /**
     * AI ID로 User ID 조회
     */
    @Query("SELECT u.userId FROM User u WHERE u.aiId = :aiId AND u.isAi = true")
    List<Long> findUserIdsByAiId(@Param("aiId") Integer aiId);

    /**
     * 특정 사용자들의 AI 봇 계좌(type=2)에 대한 모든 거래 내역 조회
     */
    @Query("""
        SELECT mt FROM MockTrade mt
        LEFT JOIN FETCH mt.stockInfo si
        WHERE mt.accountId IN (
            SELECT a.accountId FROM Account a 
            WHERE a.userId IN :userIds AND a.type = 2
        )
        ORDER BY mt.tradeAt DESC
        """)
    List<MockTrade> findAllTransactionsByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 특정 사용자들의 AI 봇 계좌(type=2)에 대한 특정 종목의 거래 내역 조회
     */
    @Query("""
        SELECT mt FROM MockTrade mt
        LEFT JOIN FETCH mt.stockInfo si
        WHERE mt.accountId IN (
            SELECT a.accountId FROM Account a 
            WHERE a.userId IN :userIds AND a.type = 2
        )
        AND si.code = :stockCode
        ORDER BY mt.tradeAt DESC
        """)
    List<MockTrade> findTransactionsByUserIdsAndStockCode(@Param("userIds") List<Long> userIds, @Param("stockCode") String stockCode);

    /**
     * 특정 사용자의 역사챌린지 계좌(type=1)에 대한 068270 종목의 거래 내역 조회
     * 역사챌린지는 고정 종목(068270)만 거래 가능
     */
    @Query("""
        SELECT mt FROM MockTrade mt
        LEFT JOIN FETCH mt.stockInfo si
        WHERE mt.accountId IN (
            SELECT a.accountId FROM Account a 
            WHERE a.userId = :userId AND a.type = 1
        )
        AND si.code = '068270'
        ORDER BY mt.tradeAt DESC
        """)
    List<MockTrade> findHistoricalChallengeTransactionsByUserId(@Param("userId") Long userId);
}
