package com.example.LAGO.service;

import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.dto.StockInfoDto;
import com.example.LAGO.repository.StockInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockInfoService {

    private final StockInfoRepository stockInfoRepository;

    // 주식 종목 정보 전체 조회
    public List<StockInfoDto> getAllStockInfo() {
        List<StockInfo> stockInfos = stockInfoRepository.findAll();
        return stockInfos.stream()
                .map(StockInfoDto::new)
                .collect(Collectors.toList());
    }


    // 종목 코드로 주식 종목 정보 조회
    public StockInfoDto getStockInfoByCode(String code) {
        Optional<StockInfo> stockInfo = stockInfoRepository.findByCode(code);
        return stockInfo.map(StockInfoDto::new).orElse(null);
    }

    // 종목 코드 존재 여부 확인
    public boolean existByCode(String code) {
        return stockInfoRepository.existsByCode(code);
    }
}
