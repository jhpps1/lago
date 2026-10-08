package com.example.LAGO.repository;

import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.domain.StockMinute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockMinuteRepository extends JpaRepository<StockMinute, Integer> {
    // 구간 조회 (stock_info_id, 시작일~종료일)
    List<StockMinute> findByStockInfoAndDateBetweenOrderByDateAsc(
            StockInfo stockInfoId,
            LocalDateTime start,
            LocalDateTime end
    );

    // 특정 종목의, 특정 시간 분 데이터(최신 시간) 한 건 조회
    StockMinute findTopByStockInfoAndDateOrderByDateDesc(StockInfo stockInfoId, LocalDateTime date);

    // StockInfo.code로 조회
    List<StockMinute> findByStockInfo_CodeAndDateBetweenOrderByDateAsc(
            String code,
            LocalDateTime start,
            LocalDateTime end
    );
    StockMinute findTopByStockInfoCodeAndDateOrderByDateDesc(String code, LocalDateTime date);
    
    // 종목 코드별 최신 분봉 데이터 조회 (AI 자동매매용)
    Optional<StockMinute> findTopByStockInfo_CodeOrderByDateDesc(String code);

}
