package com.example.LAGO.repository;

import com.example.LAGO.domain.Ticks1y;
import com.example.LAGO.domain.Ticks1yId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface Ticks1yRepository extends JpaRepository<Ticks1y, Ticks1yId> {

    /**
     * 종목 코드와 시간 범위로 연봉 데이터 조회
     * StockInfo와 JOIN하여 code로 조회하고 UTC 기준 시간 범위 필터링
     */
    @Query("SELECT t FROM Ticks1y t JOIN FETCH t.stockInfo s WHERE s.code = :code AND t.id.bucket >= :startTime AND t.id.bucket < :endTime ORDER BY t.id.bucket ASC")
    List<Ticks1y> findByCodeAndBucketRange(
            @Param("code") String code,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 종목 코드로 최신 연봉 데이터 조회 (최대 100개)
     */
    @Query("SELECT t FROM Ticks1y t JOIN FETCH t.stockInfo s WHERE s.code = :code ORDER BY t.id.bucket DESC")
    List<Ticks1y> findLatestByCode(@Param("code") String code, Pageable pageable);
}