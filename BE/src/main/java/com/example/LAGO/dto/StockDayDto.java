package com.example.LAGO.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDate;
import com.example.LAGO.domain.StockDay;

@Getter
@Setter
@Builder
public class StockDayDto {
    private LocalDate date;
    private Integer openPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Integer closePrice;
    private Integer volume;
    private Float fluctuationRate; // 필요 없으면 생략

    // Entity → DTO 변환용 정적 메서드
    public static StockDayDto fromEntity(StockDay entity) {
        return StockDayDto.builder()
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
