package com.example.LAGO.dto.response;

import com.example.LAGO.domain.HistoryChallenge;
import com.example.LAGO.domain.HistoryChallengeData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryChallengeResponse {

    @Schema(description = "챌린지 ID", example = "1")
    private Integer challengeId;

    @Schema(description = "챌린지 테마", example = "코로나")
    private String theme;

    @Schema(description = "종목명", example = "셀트리온")
    private String stockName;

    @Schema(description = "종목코드", example = "068270")
    private String stockCode;

    @Schema(description = "시작일시", example = "2025-08-09 15:00:00")
    private LocalDateTime startDate;

    @Schema(description = "종료일시", example = "2025-08-20 21:00:00")
    private LocalDateTime endDate;

    @Schema(description = "과거일시", example = "2020-06-01 09:00:00")
    private LocalDateTime originDate;

    @Schema(description = "현재 주가", example = "204730")
    private Integer currentPrice;

    @Schema(description = "등락", example = "1500")
    private Integer fluctuationPrice;

    @Schema(description = "등락률", example = "2.14")
    private Float fluctuationRate;

    public HistoryChallengeResponse(HistoryChallenge challenge, HistoryChallengeData currentData) {
        this.challengeId = challenge.getChallengeId();
        this.theme = challenge.getTheme();
        this.stockName = challenge.getStockName();
        this.stockCode = challenge.getStockCode();
        this.startDate = challenge.getStartDate();
        this.endDate = challenge.getEndDate();
        this.originDate = challenge.getOriginDate();
        this.currentPrice = currentData.getClosePrice();
        this.fluctuationPrice = currentData.getClosePrice() - currentData.getOpenPrice();
        this.fluctuationRate = this.getFluctuationPrice().floatValue() / currentData.getOpenPrice() * 100;
    }
}
