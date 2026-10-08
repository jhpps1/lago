package com.example.LAGO.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 기술적 분석 계산 유틸리티
 * 지침서 명세: 중복된 계산 로직을 중앙 집중화하여 코드 재사용성 향상
 */
@Slf4j
public final class TechnicalAnalysisUtils {
    
    private TechnicalAnalysisUtils() {
        // 유틸리티 클래스는 인스턴스화 금지
    }
    
    /**
     * 단순 이동평균(SMA) 계산
     * 
     * @param values 계산할 값들의 리스트
     * @param period 이동평균 기간
     * @param startIndex 계산 시작 인덱스
     * @return 계산된 이동평균값, 데이터 부족 시 null
     */
    public static Float calculateSMA(List<Float> values, int period, int startIndex) {
        try {
            if (values == null || values.size() < startIndex + period) {
                log.warn("SMA 계산을 위한 데이터가 부족합니다. 필요: {}, 보유: {}", 
                    period, values != null ? values.size() - startIndex : 0);
                return null;
            }
            
            float sum = 0;
            for (int i = startIndex; i < startIndex + period; i++) {
                Float value = values.get(i);
                if (value == null) {
                    log.warn("SMA 계산 중 null 값 발견 - 인덱스: {}", i);
                    return null;
                }
                sum += value;
            }
            
            return sum / period;
            
        } catch (Exception e) {
            log.error("SMA 계산 중 오류 발생 - period: {}, startIndex: {}", period, startIndex, e);
            return null;
        }
    }
    
    /**
     * 지수 이동평균(EMA) 계산
     * 
     * @param values 계산할 값들의 리스트
     * @param period EMA 기간
     * @return 계산된 EMA값, 데이터 부족 시 null
     */
    public static Float calculateEMA(List<Float> values, int period) {
        try {
            if (values == null || values.size() < period) {
                log.warn("EMA 계산을 위한 데이터가 부족합니다. 필요: {}, 보유: {}", 
                    period, values != null ? values.size() : 0);
                return null;
            }
            
            float multiplier = 2.0f / (period + 1);
            
            // 첫 번째 EMA는 SMA로 계산
            Float firstSMA = calculateSMA(values, period, values.size() - period);
            if (firstSMA == null) {
                return null;
            }
            
            float ema = firstSMA;
            
            // 역순으로 최신 데이터까지 EMA 계산
            for (int i = values.size() - period + 1; i < values.size(); i++) {
                Float currentValue = values.get(i);
                if (currentValue == null) {
                    log.warn("EMA 계산 중 null 값 발견 - 인덱스: {}", i);
                    return null;
                }
                ema = (currentValue * multiplier) + (ema * (1 - multiplier));
            }
            
            return ema;
            
        } catch (Exception e) {
            log.error("EMA 계산 중 오류 발생 - period: {}", period, e);
            return null;
        }
    }
    
    /**
     * 표준편차 계산
     * 
     * @param values 계산할 값들의 리스트
     * @param mean 평균값
     * @param period 계산 기간
     * @param startIndex 계산 시작 인덱스
     * @return 계산된 표준편차값
     */
    public static Float calculateStandardDeviation(List<Float> values, float mean, int period, int startIndex) {
        try {
            if (values == null || values.size() < startIndex + period) {
                log.warn("표준편차 계산을 위한 데이터가 부족합니다");
                return null;
            }
            
            float variance = 0;
            for (int i = startIndex; i < startIndex + period; i++) {
                Float value = values.get(i);
                if (value == null) {
                    log.warn("표준편차 계산 중 null 값 발견 - 인덱스: {}", i);
                    return null;
                }
                float diff = value - mean;
                variance += diff * diff;
            }
            
            return (float) Math.sqrt(variance / period);
            
        } catch (Exception e) {
            log.error("표준편차 계산 중 오류 발생", e);
            return null;
        }
    }
    
    /**
     * 가격 변화율 계산
     * 
     * @param currentPrice 현재 가격
     * @param previousPrice 이전 가격
     * @return 변화율 (%), 계산 불가 시 0.0f
     */
    public static float calculatePriceChangeRate(Float currentPrice, Float previousPrice) {
        if (currentPrice == null || previousPrice == null || previousPrice == 0) {
            return 0.0f;
        }
        
        return ((currentPrice - previousPrice) / previousPrice) * 100;
    }
    
    /**
     * 안전한 나눗셈 연산
     * 0으로 나누기 오류를 방지하고 null 안전성 보장
     * 
     * @param dividend 피제수
     * @param divisor 제수
     * @param defaultValue 기본값 (0으로 나누기 시 반환할 값)
     * @return 나눗셈 결과 또는 기본값
     */
    public static float safeDivide(Float dividend, Float divisor, float defaultValue) {
        if (dividend == null || divisor == null || divisor == 0) {
            return defaultValue;
        }
        
        return dividend / divisor;
    }
    
    /**
     * 리스트에서 null이 아닌 값들의 개수 반환
     * 
     * @param values 확인할 값들의 리스트
     * @return null이 아닌 값들의 개수
     */
    public static int countNonNullValues(List<Float> values) {
        if (values == null) {
            return 0;
        }
        
        return (int) values.stream()
            .filter(value -> value != null)
            .count();
    }
    
    /**
     * 가격이 특정 범위 내에 있는지 확인
     * 
     * @param price 확인할 가격
     * @param lowerBound 하한값
     * @param upperBound 상한값
     * @param tolerance 허용 오차 (0.0 ~ 1.0)
     * @return 범위 내 여부
     */
    public static boolean isPriceInRange(Float price, Float lowerBound, Float upperBound, float tolerance) {
        if (price == null || lowerBound == null || upperBound == null) {
            return false;
        }
        
        float adjustedLowerBound = lowerBound * (1 - tolerance);
        float adjustedUpperBound = upperBound * (1 + tolerance);
        
        return price >= adjustedLowerBound && price <= adjustedUpperBound;
    }
}
