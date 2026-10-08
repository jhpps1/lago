package com.example.LAGO.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OhlcDataDto {

    // Python에서 'date' 컬럼으로 사용될 필드
    private LocalDateTime date;

    @JsonProperty("open_price")
    private int openPrice;

    @JsonProperty("high_price")
    private int highPrice;

    @JsonProperty("low_price")
    private int lowPrice;

    @JsonProperty("close_price")
    private int closePrice;

    private long volume;
}
