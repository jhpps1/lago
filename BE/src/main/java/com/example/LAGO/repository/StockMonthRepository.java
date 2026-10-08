package com.example.LAGO.repository;

import com.example.LAGO.domain.StockMonth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMonthRepository extends JpaRepository<StockMonth, Integer> {

    // int 타입 날짜 구간, 오름차순 조회
    List<StockMonth> findByStockInfo_StockInfoIdAndDateBetweenOrderByDateAsc(
            Integer stockInfoId,
            Integer start,
            Integer end
    );

    List<StockMonth> findByStockInfo_StockInfoIdAndNewDateBetweenOrderByNewDateAsc(Integer stockInfoId,
                                                                           LocalDateTime startDate,
                                                                           LocalDateTime endDate);
}