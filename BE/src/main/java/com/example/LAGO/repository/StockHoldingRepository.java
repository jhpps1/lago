package com.example.LAGO.repository;

import com.example.LAGO.domain.StockHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * StockHolding 엔티티 Repository
 * 지침서 명세: STOCK_HOLDING 테이블 연동
 */
@Repository
public interface StockHoldingRepository extends JpaRepository<StockHolding, Long> {

    /**
     * 계좌별 보유 주식 조회
     * @param accountId 계좌 ID
     * @return 보유 주식 목록
     */
    @Query("SELECT sh FROM StockHolding sh WHERE sh.account.accountId = :accountId AND sh.quantity > 0")
    List<StockHolding> findByAccountId(@Param("accountId") Long accountId);

    /**
     * 특정 계좌의 특정 종목 보유 정보 조회
     * @param accountId 계좌 ID
     * @param stockCode 종목 코드
     * @return 보유 주식 정보
     */
    @Query("SELECT sh FROM StockHolding sh JOIN sh.stockInfo si WHERE sh.account.accountId = :accountId AND si.code = :stockCode")
    Optional<StockHolding> findByAccountIdAndStockCode(@Param("accountId") Long accountId, @Param("stockCode") String stockCode);

    /**
     * 사용자별 보유 주식 조회 (모든 계좌)
     * @param userId 사용자 ID
     * @return 보유 주식 목록
     */
    @Query("SELECT sh FROM StockHolding sh WHERE sh.account.userId = :userId AND sh.quantity > 0")
    List<StockHolding> findByUserId(@Param("userId") Long userId);

    /**
     * 사용자별 특정 계좌 타입의 보유 주식 조회
     * @param userId 사용자 ID
     * @param accountType 계좌 타입
     * @return 보유 주식 목록
     */
    @Query("SELECT sh FROM StockHolding sh JOIN sh.account a WHERE a.userId = :userId AND a.type = :accountType AND sh.quantity > 0")
    List<StockHolding> findByUserIdAndAccountType(@Param("userId") Long userId, @Param("accountType") Integer accountType);

    /**
     * 사용자와 종목으로 보유 주식 조회 (AI 봇용)
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @return 보유 주식 목록
     */
    @Query("SELECT sh FROM StockHolding sh JOIN sh.stockInfo si WHERE sh.account.userId = :userId AND si.code = :stockCode AND sh.quantity > 0")
    List<StockHolding> findByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);

    /**
     * 특정 종목을 보유한 모든 계좌 조회
     * @param stockCode 종목 코드
     * @return 해당 종목 보유 목록
     */
    @Query("SELECT sh FROM StockHolding sh JOIN sh.stockInfo si WHERE si.code = :stockCode AND sh.quantity > 0")
    List<StockHolding> findByStockCode(@Param("stockCode") String stockCode);

    /**
     * 계좌의 총 보유 종목 수 조회
     * @param accountId 계좌 ID
     * @return 보유 종목 수
     */
    @Query("SELECT COUNT(sh) FROM StockHolding sh WHERE sh.account.accountId = :accountId AND sh.quantity > 0")
    Long countByAccountId(@Param("accountId") Long accountId);

    /**
     * 계좌의 총 투자 금액 조회
     * @param accountId 계좌 ID
     * @return 총 투자 금액
     */
    @Query("SELECT COALESCE(SUM(sh.totalPrice), 0) FROM StockHolding sh WHERE sh.account.accountId = :accountId AND sh.quantity > 0")
    Long getTotalInvestmentByAccountId(@Param("accountId") Long accountId);

    // TODO: 현재 평가 금액은 실시간 주가가 필요하여 서비스 레이어에서 계산
    // @Query("SELECT COALESCE(SUM(sh.currentValue), 0) FROM StockHolding sh WHERE sh.account.accountId = :accountId AND sh.quantity > 0")
    // Long getCurrentValueByAccountId(@Param("accountId") Long accountId);

    /**
     * 모든 사용자의 주식 보유량 배치 조회 (랭킹용)
     * @param accountType 계좌 타입 (0: 모의투자, 2: AI봇)
     * @return 모든 보유 주식 목록
     */
    @Query("""
        SELECT sh FROM StockHolding sh 
        JOIN sh.account a 
        WHERE a.type = :accountType 
        AND sh.quantity > 0
        """)
    List<StockHolding> findAllByAccountType(@Param("accountType") Integer accountType);

    // TODO: 수익률 정렬은 실시간 계산이 필요하여 서비스 레이어에서 처리
    // @Query("SELECT sh FROM StockHolding sh WHERE sh.account.accountId = :accountId AND sh.quantity > 0 ORDER BY sh.profitLossRate DESC")  
    // List<StockHolding> findTopProfitableStocks(@Param("accountId") Long accountId, @Param("limit") int limit);

    // @Query("SELECT sh FROM StockHolding sh WHERE sh.account.accountId = :accountId AND sh.quantity > 0 ORDER BY sh.profitLossRate ASC")
    // List<StockHolding> findTopLosingStocks(@Param("accountId") Long accountId, @Param("limit") int limit);
}
