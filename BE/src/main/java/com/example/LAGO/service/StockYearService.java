package com.example.LAGO.service;

import com.example.LAGO.domain.StockYear;
import com.example.LAGO.dto.StockYearDto;
import com.example.LAGO.repository.StockYearRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockYearService {

    private final StockYearRepository stockYearRepository;

    @Autowired
    public StockYearService(StockYearRepository stockYearRepository) {
        this.stockYearRepository = stockYearRepository;
    }

    public List<StockYearDto> getStockYears(Integer stockInfoId, Integer start, Integer end) {
        List<StockYear> entityList = stockYearRepository
                .findByStockInfoStockInfoIdAndDateBetweenOrderByDateAsc(stockInfoId, start, end);

        return entityList.stream()
                .map(StockYearDto::fromEntity)
                .collect(Collectors.toList());
    }
}
