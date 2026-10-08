package com.example.LAGO.repository;

import com.example.LAGO.domain.StockYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockYearRepository extends JpaRepository<StockYear, Integer> {

    // stockInfoId, 연도 구간, 오름차순 조회
    List<StockYear> findByStockInfoStockInfoIdAndDateBetweenOrderByDateAsc(
            Integer stockInfoId,
            Integer start,
            Integer end
    );

    List<StockYear> findByStockInfo_StockInfoIdAndNewDateBetweenOrderByNewDateAsc(Integer stockInfoId,
                                                                         LocalDateTime startDate,
                                                                         LocalDateTime endDate);
}