package com.example.LAGO.service;

import com.example.LAGO.domain.StockMonth;
import com.example.LAGO.dto.StockMonthDto;
import com.example.LAGO.repository.StockMonthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockMonthService {

    private final StockMonthRepository stockMonthRepository;

    @Autowired
    public StockMonthService(StockMonthRepository stockMonthRepository) {
        this.stockMonthRepository = stockMonthRepository;
    }

    // 특정 종목의 특정 구간 월봉 데이터 조회 (DTO로 변환)
    public List<StockMonthDto> getStockMonths(Integer stockInfoId, Integer start, Integer end) {
        List<StockMonth> entityList = stockMonthRepository
                .findByStockInfo_StockInfoIdAndDateBetweenOrderByDateAsc(stockInfoId, start, end);

        // Entity → DTO 변환
        return entityList.stream()
                .map(StockMonthDto::fromEntity) // 정적 메서드 사용 (권장)
                .collect(Collectors.toList());
    }
}
