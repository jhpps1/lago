package com.example.LAGO.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.example.LAGO.domain.StockYear;

@Getter
@Setter
@Builder
public class StockYearDto {
    private Integer date;
    private Integer openPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Integer closePrice;
    private Long volume;
    private Float fluctuationRate; // 필요 없으면 생략

    // Entity -> DTO 변환
    public static StockYearDto fromEntity(StockYear entity) {
        return StockYearDto.builder()
                .date(entity.getDate())
                .openPrice(entity.getOpenPrice())
                .highPrice(entity.getHighPrice())
                .lowPrice(entity.getLowPrice())
                .closePrice(entity.getClosePrice())
                .volume(entity.getVolume())
                .fluctuationRate(entity.getFluctuationRate())
                .build();
    }
}

