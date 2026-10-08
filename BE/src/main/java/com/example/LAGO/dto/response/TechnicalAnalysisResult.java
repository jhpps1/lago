package com.example.LAGO.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 기술적 분석 결과 DTO
 * 지침서 명세: 차트 패턴 분석 및 기술적 지표 결과를 담는 DTO
 */
@Data
@Builder
public class TechnicalAnalysisResult {
    
    // 기본 가격 정보
    private String stockCode;
    private Float currentPrice;
    private Float openPrice;
    private Float highPrice;
    private Float lowPrice;
    private Float volume;
    private Float fluctuationRate;
    
    // RSI (Relative Strength Index) - 상대강도지수
    private Float rsi;
    
    // MACD (Moving Average Convergence Divergence) - 이동평균수렴확산
    private Float macdLine;
    private Float signalLine;
    private Float histogram;
    
    // 볼린저 밴드 (Bollinger Bands)
    private Float bollingerUpperBand;
    private Float bollingerMiddleBand;
    private Float bollingerLowerBand;
    
    // 이동평균선 (Moving Averages)
    private Float ma5;   // 5일 이동평균
    private Float ma10;  // 10일 이동평균
    private Float ma20;  // 20일 이동평균
    private Float ma60;  // 60일 이동평균
    private Float ma120; // 120일 이동평균
    
    // 크로스 신호
    private boolean isGoldenCross;  // 골든크로스 (단기이평선이 장기이평선 상향돌파)
    private boolean isDeathCross;   // 데드크로스 (단기이평선이 장기이평선 하향돌파)
    
    // 거래량 분석
    private Float volumeRatio;      // 거래량 비율 (평균 대비)
    private boolean isVolumeSpike;  // 거래량 급증 여부
    
    // 추세 분석
    private String trendDirection;  // UP, DOWN, SIDEWAYS
    private Float trendStrength;    // 추세 강도 (0-100)
    
    // 지지/저항선
    private Float supportLevel;     // 지지선
    private Float resistanceLevel;  // 저항선
    
    // 변동성 지표
    private Float volatility;       // 변동성 (%)
    private Float atr;             // Average True Range
    
    // 스토캐스틱
    private Float stochasticK;
    private Float stochasticD;
    
    // 윌리엄스 %R
    private Float williamsR;
    
    // CCI (Commodity Channel Index)
    private Float cci;
    
    // 패턴 분석 결과
    private String chartPattern;    // 차트 패턴 (헤드앤숄더, 삼각형, 플래그 등)
    private Float patternStrength;  // 패턴 신뢰도
    
    // 종합 신호
    private String overallSignal;   // BUY, SELL, HOLD
    private Integer signalStrength; // 신호 강도 (1-10)
    private String signalReason;    // 신호 근거
    
    // 분석 메타데이터
    private String analysisTime;    // 분석 시간
    private String analysisVersion; // 분석 버전
    private String[] warningMessages; // 경고 메시지
}
