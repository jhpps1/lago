package com.example.LAGO.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.example.LAGO.domain.StockMonth;

@Getter
@Setter
@Builder
public class StockMonthDto {
    private Integer date;
    private Integer openPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Integer closePrice;
    private Long volume;
    private Float fluctuationRate; // 필요 없으면 생략

    // Entity -> DTO 변환
    public static StockMonthDto fromEntity(StockMonth entity) {
        return StockMonthDto.builder()
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
