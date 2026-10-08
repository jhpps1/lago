package com.example.LAGO.repository;

import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.domain.StockMinute;
import com.example.LAGO.domain.Ticks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicksRepository extends JpaRepository<Ticks, Integer> {

    @Query(value = "SELECT " +
                "time_bucket(CAST(:interval AS INTERVAL), t.ts) AS bucket, " +
                "FIRST(t.open_price, t.ts) AS open, " +
                "MAX(t.high_price) AS high, " +
                "MIN(t.low_price) AS low, " +
                "LAST(t.close_price, t.ts) AS close, " +
                "SUM(t.volume) AS volume " +
            "FROM ticks t " +
                "JOIN stock_info si ON t.stock_info_id = si.stock_info_id " +
            "WHERE si.code = :stockCode " +
                "AND t.ts BETWEEN :fromDateTime AND :toDateTime " +
            "GROUP BY bucket " +
            "ORDER BY bucket", nativeQuery = true)
    List<Object[]> findAggregatedByStockCodeAndDate(
            @Param("stockCode") String stockCode,
            @Param("interval") String interval,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    /**
     * 특정 종목의 최신 종가(현재가) 조회
     * 매매 처리 시 실시간 가격 정보를 위해 사용
     * 
     * @param stockCode 종목 코드 (예: "005930")
     * @return 최신 종가, 데이터가 없으면 null
     */
    @Query(value = "SELECT t.close_price " +
            "FROM ticks t " +
            "JOIN stock_info si ON t.stock_info_id = si.stock_info_id " +
            "WHERE si.code = :stockCode " +
            "ORDER BY t.ts DESC " +
            "LIMIT 1", nativeQuery = true)
    Integer findLatestClosePriceByStockCode(@Param("stockCode") String stockCode);

    /**
     * 특정 종목의 최신 N개 ticks 데이터 조회 (기술적 분석용)
     * 
     * @param stockCode 종목 코드 (예: "005930")
     * @param limit 조회할 데이터 개수
     * @return 최신 ticks 데이터 리스트 (시간 역순)
     */
    @Query(value = "SELECT t.ts, t.open_price, t.high_price, t.low_price, t.close_price, t.volume " +
            "FROM ticks t " +
            "JOIN stock_info si ON t.stock_info_id = si.stock_info_id " +
            "WHERE si.code = :stockCode " +
            "ORDER BY t.ts DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findLatestTicksByStockCode(@Param("stockCode") String stockCode, @Param("limit") int limit);

}