package com.example.LAGO.dto.response;

import com.example.LAGO.domain.HistoryChallengeData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryChallengeDataResponse {

    @Schema(description = "행 번호", example = "1")
    private Long rowId;

    @Schema(description = "챌린지 일시", example = "2025-08-09 15:10:00")
    private LocalDateTime eventDateTime;

    @Schema(description = "과거 일시", example = "2020-07-08 15:10:00")
    private LocalDateTime originDateTime;

    @Schema(description = "시가")
    private Integer openPrice;

    @Schema(description = "고가")
    private Integer highPrice;

    @Schema(description = "저가")
    private Integer lowPrice;

    @Schema(description = "종가")
    private Integer closePrice;

    @Schema(description = "거래량")
    private Long volume;

    @Schema(name = "fluctuation_price")
    private Integer fluctuationPrice;

    @Schema(name = "fluctuation_rate")
    private Float fluctuationRate;

    public HistoryChallengeDataResponse(HistoryChallengeData entity) {
        this.eventDateTime = entity.getEventDateTime();
        this.originDateTime = entity.getOriginDateTime();
        this.openPrice = entity.getOpenPrice();
        this.highPrice = entity.getHighPrice();
        this.lowPrice = entity.getLowPrice();
        this.closePrice = entity.getClosePrice();
        this.volume = entity.getVolume().longValue();
        this.fluctuationPrice = this.getClosePrice() - this.getOpenPrice();
        this.fluctuationRate = this.getFluctuationPrice().floatValue() / this.getOpenPrice() * 100;
    }

    public HistoryChallengeDataResponse(Object[] aggregatedData) {
        this.rowId = ((Long) aggregatedData[0]);
        this.eventDateTime = ((Timestamp) aggregatedData[2]).toLocalDateTime();
        this.originDateTime = ((Timestamp) aggregatedData[1]).toLocalDateTime();
        this.openPrice = ((Integer) aggregatedData[3]);
        this.highPrice = ((Integer) aggregatedData[4]);
        this.lowPrice = ((Integer) aggregatedData[5]);
        this.closePrice = ((Integer) aggregatedData[6]);
        this.volume = ((Long) aggregatedData[7]);
        this.fluctuationPrice = this.getClosePrice() - this.getOpenPrice();
        this.fluctuationRate = this.getFluctuationPrice().floatValue() / this.getOpenPrice() * 100;
    }
}