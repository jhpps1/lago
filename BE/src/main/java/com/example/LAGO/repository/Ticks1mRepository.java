package com.example.LAGO.repository;

import com.example.LAGO.domain.Ticks1m;
import com.example.LAGO.domain.Ticks1mId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface Ticks1mRepository extends JpaRepository<Ticks1m, Ticks1mId> {

    /**
     * 종목 코드와 시간 범위로 1분봉 데이터 조회
     * StockInfo와 JOIN하여 code로 조회하고 UTC 기준 시간 범위 필터링
     * 
     * @param code 종목 코드 (예: "005930")
     * @param startTime 시작 시간 (UTC)
     * @param endTime 종료 시간 (UTC)
     * @return 1분봉 데이터 리스트 (시간순 정렬)
     */
    @Query("SELECT t FROM Ticks1m t JOIN FETCH t.stockInfo s WHERE s.code = :code AND t.id.bucket >= :startTime AND t.id.bucket < :endTime ORDER BY t.id.bucket ASC")
    List<Ticks1m> findByCodeAndBucketRange(
            @Param("code") String code,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 종목 코드로 최신 1분봉 데이터 조회 (최대 100개)
     * 
     * @param code 종목 코드
     * @param pageable 페이징 정보 (LIMIT 적용)
     * @return 최신 1분봉 데이터 리스트
     */
    @Query("SELECT t FROM Ticks1m t JOIN FETCH t.stockInfo s WHERE s.code = :code ORDER BY t.id.bucket DESC")
    List<Ticks1m> findLatestByCode(@Param("code") String code, Pageable pageable);
}