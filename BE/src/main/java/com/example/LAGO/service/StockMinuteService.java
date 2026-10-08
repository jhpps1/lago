package com.example.LAGO.service;

import com.example.LAGO.domain.StockMinute;
import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.dto.StockMinuteDto;
import com.example.LAGO.repository.StockInfoRepository;
import com.example.LAGO.repository.StockMinuteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockMinuteService {

    private final StockMinuteRepository stockMinuteRepository;
    private final StockInfoRepository stockInfoRepository;

    @Autowired
    public StockMinuteService(
            StockMinuteRepository stockMinuteRepository,
            StockInfoRepository stockInfoRepository // 생성자에 추가
    ) {
        this.stockMinuteRepository = stockMinuteRepository;
        this.stockInfoRepository = stockInfoRepository;
    }


    // stock_info_id로 조회
    // 특정 종목의 특정 구간(시작~종료) 1분봉 데이터를 조회하여 DTO로 변환
    public List<StockMinuteDto> getMinutes(Integer stockInfoId, LocalDateTime start, LocalDateTime end) {
        // 1. stockInfoId로 StockInfo 객체 조회
        StockInfo stockInfo = stockInfoRepository.findById(stockInfoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 종목이 없습니다. id=" + stockInfoId));

        // 2. 객체로 Repository 쿼리 호출
        List<StockMinute> entityList = stockMinuteRepository
                .findByStockInfoAndDateBetweenOrderByDateAsc(stockInfo, start, end);

        // 3. Entity → DTO 변환
        return entityList.stream()
                .map(StockMinuteDto::fromEntity)
                .collect(Collectors.toList());
    }

    // StockInfo.code 로 조회
    public List<StockMinuteDto> getMinutesByCode(String code, LocalDateTime start, LocalDateTime end) {
        // (선택) 존재 검증
        if (!stockInfoRepository.existsByCode(code)) {
            throw new IllegalArgumentException("해당 종목 코드가 없습니다. code=" + code);
        }

        return stockMinuteRepository
                .findByStockInfo_CodeAndDateBetweenOrderByDateAsc(code, start, end)
                .stream()
                .map(StockMinuteDto::fromEntity)
                .collect(Collectors.toList());
    }
}
