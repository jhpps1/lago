package com.example.LAGO.constants;

/**
 * 매매 관련 상수 정의
 * 지침서 명세: 매직 넘버 제거 및 중앙 집중식 상수 관리
 */
public final class TradingConstants {
    
    private TradingConstants() {
        // 유틸리티 클래스는 인스턴스화 금지
    }
    
    // ===== RSI 관련 상수 =====
    /**
     * RSI 과매도 임계값 (30% 이하 시 매수 신호)
     */
    public static final float RSI_OVERSOLD_THRESHOLD = 30.0f;
    
    /**
     * RSI 과매수 임계값 (70% 이상 시 매도 신호)
     */
    public static final float RSI_OVERBOUGHT_THRESHOLD = 70.0f;
    
    /**
     * RSI 계산 기본 기간 (14일)
     */
    public static final int RSI_DEFAULT_PERIOD = 14;
    
    /**
     * RSI 계산 기간 (기본값과 동일, 호환성 위해 별도 정의)
     */
    public static final int RSI_PERIOD = 14;
    
    // ===== 데이터 기간 관련 상수 =====
    /**
     * 분석에 필요한 최대 데이터 기간 (일)
     */
    public static final int REQUIRED_DATA_PERIOD = 120;
    
    /**
     * 분석에 필요한 최소 데이터 기간 (일)
     */
    public static final int MIN_DATA_PERIOD = 20;
    
    // ===== MACD 관련 상수 =====
    /**
     * MACD 단기 EMA 기간 (12일)
     */
    public static final int MACD_SHORT_PERIOD = 12;
    
    /**
     * MACD 장기 EMA 기간 (26일)
     */
    public static final int MACD_LONG_PERIOD = 26;
    
    /**
     * MACD 시그널 라인 EMA 기간 (9일)
     */
    public static final int MACD_SIGNAL_PERIOD = 9;
    
    /**
     * MACD 강한 모멘텀 임계값 (히스토그램 절대값)
     */
    public static final float MACD_STRONG_MOMENTUM_THRESHOLD = 0.5f;
    
    // ===== 볼린저 밴드 관련 상수 =====
    /**
     * 볼린저 밴드 기본 기간 (20일)
     */
    public static final int BOLLINGER_DEFAULT_PERIOD = 20;
    
    /**
     * 볼린저 밴드 계산 기간 (기본값과 동일, 호환성 위해 별도 정의)
     */
    public static final int BOLLINGER_PERIOD = 20;
    
    /**
     * 볼린저 밴드 표준편차 배수 (2배)
     */
    public static final float BOLLINGER_STANDARD_DEVIATION_MULTIPLIER = 2.0f;
    
    /**
     * 볼린저 밴드 터치 허용 오차 (상단밴드의 95%)
     */
    public static final float BOLLINGER_BAND_TOUCH_TOLERANCE = 0.95f;
    
    // ===== 이동평균선 관련 상수 =====
    /**
     * 단기 이동평균 기간 (5일)
     */
    public static final int MA_SHORT_PERIOD = 5;
    
    /**
     * 중기 이동평균 기간 (20일)
     */
    public static final int MA_MEDIUM_PERIOD = 20;
    
    /**
     * 장기 이동평균 기간 (60일)
     */
    public static final int MA_LONG_PERIOD = 60;
    
    /**
     * 추세 강도 판단 임계값 (%)
     */
    public static final float TREND_STRENGTH_THRESHOLD = 3.0f;
    
    // ===== 매매 전략 관련 상수 =====
    /**
     * 공격적 전략 매수/매도 신호 임계값
     */
    public static final int AGGRESSIVE_SIGNAL_THRESHOLD = 2;
    
    /**
     * 보수적 전략 매수/매도 신호 임계값
     */
    public static final int CONSERVATIVE_SIGNAL_THRESHOLD = 3;
    
    /**
     * 골든크로스/데드크로스 신호 가중치
     */
    public static final int CROSS_SIGNAL_WEIGHT = 2;
    
    /**
     * 일반 신호 가중치
     */
    public static final int NORMAL_SIGNAL_WEIGHT = 1;
    
    // ===== 데이터 요구사항 =====
    /**
     * 기술적 분석을 위한 최소 데이터 요구량 (120일)
     */
    public static final int MIN_DATA_SIZE_FOR_ANALYSIS = 120;
    
    /**
     * RSI 계산을 위한 최소 데이터 요구량
     */
    public static final int MIN_DATA_SIZE_FOR_RSI = RSI_DEFAULT_PERIOD + 1;
    
    /**
     * MACD 계산을 위한 최소 데이터 요구량
     */
    public static final int MIN_DATA_SIZE_FOR_MACD = MACD_LONG_PERIOD;
    
    // ===== 신호 강도 및 점수 =====
    /**
     * 최대 신호 강도 (10점 만점)
     */
    public static final int MAX_SIGNAL_STRENGTH = 10;
    
    /**
     * 기본 신호 강도 (분석 불가 시)
     */
    public static final int DEFAULT_SIGNAL_STRENGTH = 1;
    
    /**
     * 신호 강도 배수 (신호 개수 * 2)
     */
    public static final int SIGNAL_STRENGTH_MULTIPLIER = 2;
    
    // ===== 매매 타입 상수 =====
    /**
     * 매수 주문 타입
     */
    public static final String TRADE_TYPE_BUY = "BUY";
    
    /**
     * 매도 주문 타입
     */
    public static final String TRADE_TYPE_SELL = "SELL";
    
    /**
     * 홀드 신호 (매매하지 않음)
     */
    public static final String SIGNAL_HOLD = "HOLD";
    
    // ===== AI 캐릭터별 투자 전략 상수 =====
    
    /**
     * 투자 성향별 캐릭터 이름 (DB personality 값과 매핑)
     */
    public static final String PERSONALITY_AGGRESSIVE = "공격투자형";      // 화끈이
    public static final String PERSONALITY_ACTIVE = "적극투자형";         // 적극이  
    public static final String PERSONALITY_NEUTRAL = "위험중립형";        // 균형이
    public static final String PERSONALITY_CONSERVATIVE = "안정추구형";    // 조심이
    
    /**
     * AI 캐릭터 이름 (프론트엔드 API용)
     */
    public static final String CHARACTER_HWAKKEUN = "화끈이";    // 공격투자형
    public static final String CHARACTER_JEOKGEUK = "적극이";   // 적극투자형  
    public static final String CHARACTER_GYUNHYUNG = "균형이";  // 위험중립형
    public static final String CHARACTER_JOSIM = "조심이";      // 안정추구형
    
    /**
     * 투자 성향 → 캐릭터 이름 매핑 (레거시 호환)
     * @deprecated PersonalityType enum 사용 권장
     */
    @Deprecated
    public static String getCharacterName(String personality) {
        return switch (personality) {
            case PERSONALITY_AGGRESSIVE -> CHARACTER_HWAKKEUN;
            case PERSONALITY_ACTIVE -> CHARACTER_JEOKGEUK;
            case PERSONALITY_NEUTRAL -> CHARACTER_GYUNHYUNG;
            case PERSONALITY_CONSERVATIVE -> CHARACTER_JOSIM;
            default -> "알수없음";
        };
    }
    
    /**
     * 캐릭터 이름 → 투자 성향 매핑 (레거시 호환)
     * @deprecated PersonalityType enum 사용 권장
     */
    @Deprecated
    public static String getPersonalityFromCharacter(String characterName) {
        return switch (characterName) {
            case CHARACTER_HWAKKEUN -> PERSONALITY_AGGRESSIVE;
            case CHARACTER_JEOKGEUK -> PERSONALITY_ACTIVE;
            case CHARACTER_GYUNHYUNG -> PERSONALITY_NEUTRAL;
            case CHARACTER_JOSIM -> PERSONALITY_CONSERVATIVE;
            default -> null;
        };
    }
    
    // ===== 거래 관련 상수 =====
    /**
     * 수수료율 (0.2%)
     */
    public static final double COMMISSION_RATE = 0.002;
    
    /**
     * 매도 세금율 (0.1%)
     */
    public static final double SELL_TAX_RATE = 0.001;
    
    /**
     * AI 봇 초기 자금 (1천만원)
     */
    public static final int AI_BOT_INITIAL_BALANCE = 10_000_000;
    
    // ===== 계좌 타입 상수 =====
    /**
     * 모의투자 계좌 타입 (정수값) - AI 봇도 이 타입 사용
     */
    public static final Integer ACCOUNT_TYPE_MOCK_TRADING = 0;
    
    /**
     * 역사 챌린지 계좌 타입 (정수값)
     */
    public static final Integer ACCOUNT_TYPE_HISTORICAL_CHALLENGE = 1;
    
    /**
     * AI 봇 계좌 타입 (정수값)
     */
    public static final Integer ACCOUNT_TYPE_AI_BOT = 2;
    
    // ===== 계좌 타입 상수 (레거시 String - 호환성 유지) =====
    /**
     * 현시점 계좌 타입 (레거시)
     */
    public static final String ACCOUNT_TYPE_CURRENT = "현시점";
    
    /**
     * AI 봇 계좌 타입 (레거시)
     */
    public static final String ACCOUNT_TYPE_AI_BOT_LEGACY = "ai_bot";
    
    /**
     * 역사 챌린지 계좌 타입 (레거시)
     */
    public static final String ACCOUNT_TYPE_HISTORY = "역사챌린지";
    
    /**
     * 거래 완료 상태
     */
    public static final String TRADE_STATUS_COMPLETED = "COMPLETED";
    
    // ===== 기본 거래 설정 =====
    /**
     * 기본 거래 수량
     */
    public static final int DEFAULT_TRADE_QUANTITY = 1;
    
    /**
     * 최대 거래 수량
     */
    public static final int MAX_TRADE_QUANTITY = 1000;
    
    /**
     * 최소 거래 금액
     */
    public static final int MIN_TRADE_AMOUNT = 10000;
    
    // ===== 매매 신호 상수 =====
    /**
     * 매수 신호
     */
    public static final String SIGNAL_BUY = "BUY";
    
    /**
     * 매도 신호
     */
    public static final String SIGNAL_SELL = "SELL";
    
    // ===== 전략 타입 상수 =====
    /**
     * 보수적 전략
     */
    public static final String STRATEGY_CONSERVATIVE = "CONSERVATIVE";
    
    /**
     * 공격적 전략
     */
    public static final String STRATEGY_AGGRESSIVE = "AGGRESSIVE";
    
    /**
     * 균형적 전략
     */
    public static final String STRATEGY_BALANCED = "BALANCED";
    
    // ===== AI 자동매매 관련 상수 =====
    /**
     * 자동매매 실행 간격 (밀리초) - 1분마다 실행
     */
    public static final long AUTO_TRADING_INTERVAL_MS = 60_000L;
    
    /**
     * 자동매매 시작 지연 시간 (밀리초) - 30초 후 시작
     */
    public static final long AUTO_TRADING_INITIAL_DELAY_MS = 30_000L;
    
    /**
     * 기본 AI 전략
     */
    public static final String DEFAULT_AI_STRATEGY = "균형이";
    
    /**
     * 동시 거래 가능한 최대 종목 수
     */
    public static final int MAX_CONCURRENT_STOCKS = 10;
    
    /**
     * 최소 주식 가격 (원)
     */
    public static final int MIN_STOCK_PRICE = 1000;
    
    /**
     * 최대 주식 가격 (원)
     */
    public static final int MAX_STOCK_PRICE = 1000000;
    
    /**
     * 최소 거래량
     */
    public static final long MIN_TRADING_VOLUME = 10000L;
    
    /**
     * 최대 변동률 (%)
     */
    public static final float MAX_FLUCTUATION_RATE = 15.0f;
    
    /**
     * 포지션 크기 비율 (계좌 잔액 대비)
     */
    public static final double POSITION_SIZE_RATIO = 0.1; // 10%
    
    /**
     * 최대 포지션 크기 (주)
     */
    public static final int MAX_POSITION_SIZE = 100;
    
    /**
     * 기본 매도 수량 (주)
     */
    public static final int DEFAULT_SELL_QUANTITY = 10;
}
