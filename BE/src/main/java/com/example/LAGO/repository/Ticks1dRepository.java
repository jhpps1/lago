package com.example.LAGO.repository;

import com.example.LAGO.domain.Ticks1d;
import com.example.LAGO.domain.Ticks1dId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface Ticks1dRepository extends JpaRepository<Ticks1d, Ticks1dId> {

    /**
     * 종목 코드와 시간 범위로 일봉 데이터 조회
     * StockInfo와 JOIN하여 code로 조회하고 UTC 기준 시간 범위 필터링
     */
    @Query("SELECT t FROM Ticks1d t JOIN FETCH t.stockInfo s WHERE s.code = :code AND t.id.bucket >= :startTime AND t.id.bucket < :endTime ORDER BY t.id.bucket ASC")
    List<Ticks1d> findByCodeAndBucketRange(
            @Param("code") String code,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 종목 코드로 최신 일봉 데이터 조회 (최대 100개)
     */
    @Query("SELECT t FROM Ticks1d t JOIN FETCH t.stockInfo s WHERE s.code = :code ORDER BY t.id.bucket DESC")
    List<Ticks1d> findLatestByCode(@Param("code") String code, Pageable pageable);
}