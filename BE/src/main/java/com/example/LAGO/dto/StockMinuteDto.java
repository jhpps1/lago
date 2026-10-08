package com.example.LAGO.dto;

import com.example.LAGO.domain.StockMinute;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.example.LAGO.domain.StockInfo;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StockMinuteDto {
    private Integer stockInfoId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime date;
    private Integer openPrice;
    private Integer highPrice;
    private Integer lowPrice;
    private Integer closePrice;
    private Integer volume;

    // Entity -> DTO 변환
    public static StockMinuteDto fromEntity(StockMinute entity) {
        return StockMinuteDto.builder()
                .stockInfoId(entity.getStockInfo().getStockInfoId())
                .date(entity.getDate())
                .openPrice(entity.getOpenPrice())
                .highPrice(entity.getHighPrice())
                .lowPrice(entity.getLowPrice())
                .closePrice(entity.getClosePrice())
                .volume(entity.getVolume())
                .build();
    }
}
