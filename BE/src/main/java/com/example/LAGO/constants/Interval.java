package com.example.LAGO.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum Interval {

    MINUTE("1m"),
    MINUTE3("3m"),
    MINUTE5("5m"),
    MINUTE10("10m"),
    MINUTE15("15m"),
    MINUTE30("30m"),
    MINUTE60("60m"),
    DAY("1D"),
    WEEK("1W"),
    MONTH("1M"),
    YEAR("1Y")
    ;

    private final String code;

    @JsonCreator
    public static Interval fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Stream.of(Interval.values())
                .filter(i -> i.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 간격 코드입니다: " + code));
    }

    public static String intervalToString(Interval interval) {
        return switch (interval) {
            case MINUTE -> "1 minute";
            case MINUTE3 -> "3 minute";
            case MINUTE5 -> "5 minute";
            case MINUTE10 -> "10 minute";
            case MINUTE15 -> "15 minute";
            case MINUTE30 -> "30 minute";
            case MINUTE60 -> "1 hour";
            case DAY -> "1 day";
            case WEEK -> "1 week";
            case MONTH -> "1 month";
            case YEAR -> "1 year";
        };
    }
}
