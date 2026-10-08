package com.example.LAGO.repository;

import com.example.LAGO.domain.StockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

public interface StockInfoRepository extends JpaRepository<StockInfo, Integer> {

    /**
     * 종목 ID로 주식 정보 조회
     *
     * @param stockInfoId ID
     * @return 주식 정보
     */
    Optional<StockInfo> findByStockInfoId(int stockInfoId);
    
    /**
     * 종목 코드로 주식 정보 조회 (호환성)
     */
    @Query("SELECT s FROM StockInfo s WHERE s.code = :code")
    Optional<StockInfo> findByCode(@Param("code") String code);

    /**
     * 종목명으로 주식 정보 조회는 현재 지원하지 않음
     * StockInfo의 name 필드 매핑 문제로 인해 제거됨
     */

    /**
     * 시장별 주식 정보 조회
     *
     * @param market 시장 구분 (KOSPI/KOSDAQ)
     * @return 주식 정보 리스트
     */
    java.util.List<StockInfo> findByMarket(String market);

    // 종목코드로 stock_info_id만 직접 조회 (성능 최적화)
    @Query("SELECT s.stockInfoId FROM StockInfo s WHERE s.code = :stockCode")
    Optional<Integer> findStockInfoIdByStockCode(@Param("stockCode") String stockCode);
    
    // 종목코드로 stock_info_id만 직접 조회 (호환성)
    @Query("SELECT s.stockInfoId FROM StockInfo s WHERE s.code = :code")
    Optional<Integer> findStockInfoIdByCode(@Param("code") String code);

    // 캐싱을 위한 존재 여부 확인
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM StockInfo s WHERE s.code = :stockCode")
    boolean existsByStockCode(@Param("stockCode") String stockCode);
    
    // 캐싱을 위한 존재 여부 확인 (호환성)
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM StockInfo s WHERE s.code = :code")
    boolean existsByCode(@Param("code") String code);
    
    // 상위 20개 주요 종목 조회 (뉴스 수집용)
    List<StockInfo> findTop20ByOrderByStockInfoIdAsc();
    
    // 주요 종목 10개 조회 (marketCap 필드가 DB에 없으므로 ID 기준으로 변경)
    List<StockInfo> findTop10ByOrderByStockInfoIdAsc();
}