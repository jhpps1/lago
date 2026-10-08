package com.example.LAGO.ai.strategy;

import com.example.LAGO.ai.sentiment.FinBertSentimentService;
import com.example.LAGO.ai.sentiment.dto.SentimentResponseDto;
import com.example.LAGO.ai.strategy.dto.CharacterTradingRecommendation;
import com.example.LAGO.dto.response.TechnicalAnalysisResult;
import com.example.LAGO.ai.strategy.dto.TradingSignal;
import com.example.LAGO.constants.TradingConstants;
import com.example.LAGO.domain.*;
import com.example.LAGO.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 캐릭터 기반 AI 트레이딩 전략 서비스
 * 
 * 지침서 명세:
 * - 투자성향별 캐릭터 이름으로 전략 설계 (프론트 API 친화적)
 * - FinBERT 감정 분석과 기술적 분석을 통합한 매매 결정
 * - Java 21 Virtual Thread를 활용한 고성능 AI 전략 처리
 * 
 * 연동된 EC2 DB 테이블:
 * - USERS: 사용자 투자 성향 (personality)
 * - AI_STRATEGY: AI 전략 정보
 * - STOCK_DAY: 일봉 데이터 (기술적 분석용)
 * - STOCK_INFO: 주식 기본 정보
 * 
 * 캐릭터별 전략:
 * - 화끈이 (공격투자형): 긍정 신호에 강하게 반응, 고위험 고수익 추구
 * - 적극이 (적극투자형): 성장주 중심, 적극적 매매
 * - 균형이 (위험중립형): 분산투자, 균형잡힌 포트폴리오
 * - 조심이 (안정추구형): 가치투자, 부정 신호에 민감
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingStrategyService {

    // ======================== 의존성 주입 ========================

    /**
     * FinBERT 감정 분석 서비스
     */
    private final FinBertSentimentService finBertSentimentService;

    /**
     * 사용자 리포지토리
     */
    private final UserRepository userRepository;

    /**
     * AI 전략 리포지토리
     */
    private final AiStrategyRepository aiStrategyRepository;

    /**
     * 주식 정보 리포지토리
     */
    private final StockInfoRepository stockInfoRepository;

    /**
     * 주식 일봉 데이터 리포지토리
     */
    private final StockDayRepository stockDayRepository;

    // ======================== Virtual Thread Executor ========================

    /**
     * Java 21 Virtual Thread를 활용한 고성능 AI 전략 처리
     */
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ======================== 캐릭터별 전략 실행 ========================

    /**
     * 화끈이 전략 실행 (공격투자형)
     * 
     * 전략 특징:
     * - FinBERT 긍정 신호에 강하게 반응
     * - 고위험 고수익 추구
     * - 단기 모멘텀 중시
     * - 손절/익절 폭이 큼
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @param newsUrls 관련 뉴스 URL 리스트
     * @return 화끈이 매매 추천 결과
     */
    public CompletableFuture<CharacterTradingRecommendation> executeHwakkeunStrategy(
            Long userId, String stockCode, List<String> newsUrls) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("화끈이 전략 실행 시작: userId={}, stockCode={}, newsCount={}", 
                        userId, stockCode, newsUrls.size());

                // 1. 사용자 정보 조회
                User user = getUserOrThrow(userId);
                
                // 2. FinBERT 감정 분석
                SentimentResponseDto sentiment = analyzeSentimentForStock(stockCode, newsUrls, userId);
                
                // 3. 기술적 분석
                TechnicalAnalysisResult technical = performTechnicalAnalysis(stockCode);
                
                // 4. 화끈이 매매 신호 생성
                TradingSignal signal = generateHwakkeunTradingSignal(sentiment, technical);
                
                // 5. 전략 결과 저장
                AiStrategy strategy = saveStrategyResult(user, stockCode, TradingConstants.CHARACTER_HWAKKEUN, 
                        signal, sentiment, technical);

                log.info("화끈이 전략 실행 완료: userId={}, stockCode={}, signal={}, confidence={}", 
                        userId, stockCode, signal.getAction(), signal.getConfidence());

                return CharacterTradingRecommendation.builder()
                    .characterName(TradingConstants.CHARACTER_HWAKKEUN)
                    .userId(userId)
                    .stockCode(stockCode)
                    .tradingSignal(signal)
                    .sentimentAnalysis(sentiment)
                    .technicalAnalysis(technical)
                    .strategyId(strategy != null ? 1L : null) // TODO: AiStrategy 엔티티 구조 확인 후 수정
                    .success(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("화끈이 전략 실행 실패: userId={}, stockCode={}", userId, stockCode, e);
                return createErrorRecommendation(TradingConstants.CHARACTER_HWAKKEUN, userId, stockCode, e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    /**
     * 적극이 전략 실행 (적극투자형)
     * 
     * 전략 특징:
     * - 성장주 중심 투자
     * - FinBERT 긍정 신호에 민감
     * - 기술적 분석 중시
     * - 적극적 매매
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @param newsUrls 관련 뉴스 URL 리스트
     * @return 적극이 매매 추천 결과
     */
    public CompletableFuture<CharacterTradingRecommendation> executeJeokgeukStrategy(
            Long userId, String stockCode, List<String> newsUrls) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("적극이 전략 실행 시작: userId={}, stockCode={}, newsCount={}", 
                        userId, stockCode, newsUrls.size());

                User user = getUserOrThrow(userId);
                SentimentResponseDto sentiment = analyzeSentimentForStock(stockCode, newsUrls, userId);
                TechnicalAnalysisResult technical = performTechnicalAnalysis(stockCode);
                
                TradingSignal signal = generateJeokgeukTradingSignal(sentiment, technical);
                
                AiStrategy strategy = saveStrategyResult(user, stockCode, TradingConstants.CHARACTER_JEOKGEUK, 
                        signal, sentiment, technical);

                log.info("적극이 전략 실행 완료: userId={}, stockCode={}, signal={", 
                        userId, stockCode, signal.getAction());

                return CharacterTradingRecommendation.builder()
                    .characterName(TradingConstants.CHARACTER_JEOKGEUK)
                    .userId(userId)
                    .stockCode(stockCode)
                    .tradingSignal(signal)
                    .sentimentAnalysis(sentiment)
                    .technicalAnalysis(technical)
                    .strategyId(strategy != null ? 1L : null) // TODO: AiStrategy 엔티티 구조 확인 후 수정
                    .success(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("적극이 전략 실행 실패: userId={}, stockCode={}", userId, stockCode, e);
                return createErrorRecommendation(TradingConstants.CHARACTER_JEOKGEUK, userId, stockCode, e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    /**
     * 균형이 전략 실행 (위험중립형)
     * 
     * 전략 특징:
     * - 분산투자 중시
     * - 균형잡힌 포트폴리오 추구
     * - 리스크 관리 중시
     * - 안정적 수익 추구
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @param newsUrls 관련 뉴스 URL 리스트
     * @return 균형이 매매 추천 결과
     */
    public CompletableFuture<CharacterTradingRecommendation> executeGyunhyungStrategy(
            Long userId, String stockCode, List<String> newsUrls) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("균형이 전략 실행 시작: userId={}, stockCode={}, newsCount={}", 
                        userId, stockCode, newsUrls.size());

                User user = getUserOrThrow(userId);
                SentimentResponseDto sentiment = analyzeSentimentForStock(stockCode, newsUrls, userId);
                TechnicalAnalysisResult technical = performTechnicalAnalysis(stockCode);
                
                TradingSignal signal = generateGyunhyungTradingSignal(sentiment, technical);
                
                AiStrategy strategy = saveStrategyResult(user, stockCode, TradingConstants.CHARACTER_GYUNHYUNG, 
                        signal, sentiment, technical);

                log.info("균형이 전략 실행 완료: userId={}, stockCode={}, signal={}", 
                        userId, stockCode, signal.getAction());

                return CharacterTradingRecommendation.builder()
                    .characterName(TradingConstants.CHARACTER_GYUNHYUNG)
                    .userId(userId)
                    .stockCode(stockCode)
                    .tradingSignal(signal)
                    .sentimentAnalysis(sentiment)
                    .technicalAnalysis(technical)
                    .strategyId(strategy != null ? 1L : null) // TODO: AiStrategy 엔티티 구조 확인 후 수정
                    .success(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("균형이 전략 실행 실패: userId={}, stockCode={}", userId, stockCode, e);
                return createErrorRecommendation(TradingConstants.CHARACTER_GYUNHYUNG, userId, stockCode, e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    /**
     * 조심이 전략 실행 (안정추구형)
     * 
     * 전략 특징:
     * - 가치투자 중심
     * - FinBERT 부정 신호에 민감
     * - 보수적 매매
     * - 안정성 우선
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @param newsUrls 관련 뉴스 URL 리스트
     * @return 조심이 매매 추천 결과
     */
    public CompletableFuture<CharacterTradingRecommendation> executeJosimStrategy(
            Long userId, String stockCode, List<String> newsUrls) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("조심이 전략 실행 시작: userId={}, stockCode={}, newsCount={}", 
                        userId, stockCode, newsUrls.size());

                User user = getUserOrThrow(userId);
                SentimentResponseDto sentiment = analyzeSentimentForStock(stockCode, newsUrls, userId);
                TechnicalAnalysisResult technical = performTechnicalAnalysis(stockCode);
                
                TradingSignal signal = generateJosimTradingSignal(sentiment, technical);
                
                AiStrategy strategy = saveStrategyResult(user, stockCode, TradingConstants.CHARACTER_JOSIM, 
                        signal, sentiment, technical);

                log.info("조심이 전략 실행 완료: userId={}, stockCode={}, signal={}", 
                        userId, stockCode, signal.getAction());

                return CharacterTradingRecommendation.builder()
                    .characterName(TradingConstants.CHARACTER_JOSIM)
                    .userId(userId)
                    .stockCode(stockCode)
                    .tradingSignal(signal)
                    .sentimentAnalysis(sentiment)
                    .technicalAnalysis(technical)
                    .strategyId(strategy != null ? 1L : null) // TODO: AiStrategy 엔티티 구조 확인 후 수정
                    .success(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            } catch (Exception e) {
                log.error("조심이 전략 실행 실패: userId={}, stockCode={}", userId, stockCode, e);
                return createErrorRecommendation(TradingConstants.CHARACTER_JOSIM, userId, stockCode, e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    // ======================== 통합 전략 메서드 ========================

    /**
     * 사용자 성향에 맞는 캐릭터 전략 자동 실행
     * 
     * @param userId 사용자 ID
     * @param stockCode 종목 코드
     * @param newsUrls 관련 뉴스 URL 리스트
     * @return 사용자 맞춤 매매 추천 결과
     */
    public CompletableFuture<CharacterTradingRecommendation> executeUserPersonalizedStrategy(
            Long userId, String stockCode, List<String> newsUrls) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 사용자 성향 조회
                User user = getUserOrThrow(userId);
                String characterName = TradingConstants.getCharacterName(String.valueOf(user.getPersonality()));
                
                log.info("사용자 맞춤 전략 실행: userId={}, personality={}, character={}, stockCode={}", 
                        userId, user.getPersonality(), characterName, stockCode);

                // 성향별 전략 실행
                return switch (characterName) {
                    case TradingConstants.CHARACTER_HWAKKEUN -> executeHwakkeunStrategy(userId, stockCode, newsUrls).join();
                    case TradingConstants.CHARACTER_JEOKGEUK -> executeJeokgeukStrategy(userId, stockCode, newsUrls).join();
                    case TradingConstants.CHARACTER_GYUNHYUNG -> executeGyunhyungStrategy(userId, stockCode, newsUrls).join();
                    case TradingConstants.CHARACTER_JOSIM -> executeJosimStrategy(userId, stockCode, newsUrls).join();
                    default -> {
                        log.warn("알 수 없는 투자 성향: userId={}, personality={}", userId, user.getPersonality());
                        yield executeGyunhyungStrategy(userId, stockCode, newsUrls).join(); // 기본값: 균형이
                    }
                };

            } catch (Exception e) {
                log.error("사용자 맞춤 전략 실행 실패: userId={}, stockCode={}", userId, stockCode, e);
                return createErrorRecommendation("UNKNOWN", userId, stockCode, e.getMessage());
            }
        }, virtualThreadExecutor);
    }

    // ======================== 내부 메서드 ========================

    /**
     * 사용자 정보 조회 (예외 발생)
     */
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 주식의 FinBERT 감정 분석 수행
     */
    private SentimentResponseDto analyzeSentimentForStock(String stockCode, List<String> newsUrls, Long userId) {
        try {
            return finBertSentimentService.analyzeStockRelatedNewsAsync(stockCode, newsUrls, userId).join();
        } catch (Exception e) {
            log.error("감정 분석 실패: stockCode={}, userId={}", stockCode, userId, e);
            return SentimentResponseDto.builder()
                .success(false)
                .errorMessage("감정 분석 실패: " + e.getMessage())
                .build();
        }
    }

    /**
     * 기술적 분석 수행
     */
    private TechnicalAnalysisResult performTechnicalAnalysis(String stockCode) {
        try {
            // 1. 주식 정보 조회
            StockInfo stockInfo = stockInfoRepository.findByCode(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("주식 정보를 찾을 수 없습니다: " + stockCode));

            // 2. EC2 DB에서 최근 60일 주식 데이터 조회
            List<StockDay> recentData = stockDayRepository.findByStockInfoStockInfoIdOrderByDateDesc(
                stockInfo.getStockInfoId(), Pageable.ofSize(60));
            
            if (recentData.isEmpty()) {
                throw new IllegalArgumentException("주식 데이터를 찾을 수 없습니다: " + stockCode);
            }

            StockDay latestData = recentData.get(0);
            
            // 기술적 지표 계산
            double ma5 = calculateMovingAverage(recentData, 5);
            double ma20 = calculateMovingAverage(recentData, 20);
            double ma60 = calculateMovingAverage(recentData, 60);
            double rsi = calculateRSI(recentData, 14);
            
            // 기술적 점수 계산 및 신호 결정
            double technicalScoreRaw = calculateTechnicalScore(latestData, ma5, ma20, ma60, rsi);
            String technicalSignal = determineTechnicalSignal(technicalScoreRaw);

            // overallSignal/strength로 매핑
            String overallSignal = switch (technicalSignal) {
                case "BULLISH" -> "BUY";
                case "BEARISH" -> "SELL";
                default -> "HOLD";
            };
            int signalStrength = (int) Math.max(1, Math.min(10, Math.round(Math.abs(technicalScoreRaw) * 10)));

            return TechnicalAnalysisResult.builder()
                .stockCode(stockCode)
                .currentPrice((float) latestData.getClosePrice())
                .fluctuationRate(latestData.getFluctuationRate() != null ? latestData.getFluctuationRate().floatValue() : null)
                .ma5((float) ma5)
                .ma20((float) ma20)
                .ma60((float) ma60)
                .rsi((float) rsi)
                .overallSignal(overallSignal)
                .signalStrength(signalStrength)
                .signalReason(generateTechnicalSummary(technicalScoreRaw, rsi, ma5, ma20))
                .analysisTime(LocalDateTime.now().toString())
                .analysisVersion("1.0")
                .build();

        } catch (Exception e) {
            log.error("기술적 분석 실패: stockCode={}", stockCode, e);
            return TechnicalAnalysisResult.builder()
                .stockCode(stockCode)
                .overallSignal("HOLD")
                .signalStrength(0)
                .signalReason("기술적 분석 실패: " + e.getMessage())
                .analysisTime(LocalDateTime.now().toString())
                .analysisVersion("1.0")
                .build();
        }
    }

    /**
     * 이동평균 계산
     */
    private double calculateMovingAverage(List<StockDay> data, int period) {
        if (data.size() < period) return 0.0;
        
        return data.stream()
            .limit(period)
            .mapToInt(StockDay::getClosePrice)
            .average()
            .orElse(0.0);
    }

    /**
     * RSI 계산
     */
    private double calculateRSI(List<StockDay> data, int period) {
        if (data.size() < period + 1) return 50.0; // 기본값
        
        double totalGain = 0.0;
        double totalLoss = 0.0;
        
        for (int i = 0; i < period; i++) {
            if (i + 1 < data.size()) {
                int priceDiff = data.get(i).getClosePrice() - data.get(i + 1).getClosePrice();
                if (priceDiff > 0) {
                    totalGain += priceDiff;
                } else {
                    totalLoss += Math.abs(priceDiff);
                }
            }
        }
        
        if (totalLoss == 0) return 100.0;
        
        double rs = totalGain / totalLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * 기술적 점수 계산
     */
    private double calculateTechnicalScore(StockDay latestData, double ma5, double ma20, double ma60, double rsi) {
        double score = 0.0;
        int currentPrice = latestData.getClosePrice();
        
        // 이동평균선 배열 점수
        if (currentPrice > ma5) score += 0.3;
        if (ma5 > ma20) score += 0.2;
        if (ma20 > ma60) score += 0.2;
        
        // RSI 점수
        if (rsi > 70) score -= 0.2; // 과매수
        else if (rsi < 30) score += 0.2; // 과매도
        
        // 변동률 점수
        double changeRate = latestData.getFluctuationRate().doubleValue();
        if (changeRate > 3) score += 0.1;
        else if (changeRate < -3) score -= 0.1;
        
        return Math.max(-1.0, Math.min(1.0, score));
    }

    /**
     * 기술적 신호 결정
     */
    private String determineTechnicalSignal(double score) {
        if (score >= 0.3) return "BULLISH";
        if (score <= -0.3) return "BEARISH";
        return "NEUTRAL";
    }

    /**
     * 기술적 분석 요약 생성
     */
    private String generateTechnicalSummary(double score, double rsi, double ma5, double ma20) {
        StringBuilder summary = new StringBuilder();
        
        if (score > 0.3) {
            summary.append("강한 상승 신호. ");
        } else if (score > 0) {
            summary.append("약한 상승 신호. ");
        } else if (score < -0.3) {
            summary.append("강한 하락 신호. ");
        } else if (score < 0) {
            summary.append("약한 하락 신호. ");
        } else {
            summary.append("중립적 신호. ");
        }
        
        if (rsi > 70) {
            summary.append("RSI 과매수 구간.");
        } else if (rsi < 30) {
            summary.append("RSI 과매도 구간.");
        }
        
        if (ma5 > ma20) {
            summary.append(" 단기 상승 추세.");
        } else {
            summary.append(" 단기 하락 추세.");
        }
        
        return summary.toString();
    }

    /**
     * 기술적 분석 결과에서 정규화된 점수 파생 (-1.0 ~ 1.0)
     */
    private double deriveNormalizedTechnicalScore(TechnicalAnalysisResult technical) {
        if (technical == null || technical.getOverallSignal() == null || technical.getSignalStrength() == null) {
            return 0.0;
        }
        double base = Math.max(0, Math.min(10, technical.getSignalStrength())) / 10.0; // 0.0 ~ 1.0
        return switch (technical.getOverallSignal()) {
            case "BUY", "BULLISH" -> base;
            case "SELL", "BEARISH" -> -base;
            default -> 0.0; // HOLD or unknown
        };
    }

    /**
     * 화끈이 매매 신호 생성
     */
    private TradingSignal generateHwakkeunTradingSignal(SentimentResponseDto sentiment, TechnicalAnalysisResult technical) {
        double sentimentScore = sentiment.getSuccess() ? sentiment.getSentimentScore() : 0.0;
        double technicalScore = deriveNormalizedTechnicalScore(technical);
        
        // 화끈이는 감정 점수에 1.3배 가중치
        double combinedScore = (sentimentScore * 1.3 + technicalScore) / 2.0;
        
        String action;
        double strength = Math.abs(combinedScore);
        
        if (combinedScore >= 0.6) action = "STRONG_BUY";
        else if (combinedScore >= 0.3) action = "BUY";
        else if (combinedScore >= -0.2) action = "HOLD";
        else if (combinedScore >= -0.5) action = "WEAK_SELL";
        else action = "STRONG_SELL";
        
        String reasoning = String.format("FinBERT 감정점수: %.2f, 기술적점수: %.2f, 화끈이 가중점수: %.2f", 
                sentimentScore, technicalScore, combinedScore);
        
        return TradingSignal.builder()
            .action(action)
            .strength(strength)
            .confidence(Math.min(0.9, (sentiment.getConfidence() != null ? sentiment.getConfidence() : 0.5) + 0.1))
            .reasoning(reasoning)
            .build();
    }

    /**
     * 적극이 매매 신호 생성
     */
    private TradingSignal generateJeokgeukTradingSignal(SentimentResponseDto sentiment, TechnicalAnalysisResult technical) {
        double sentimentScore = sentiment.getSuccess() ? sentiment.getSentimentScore() : 0.0;
        double technicalScore = deriveNormalizedTechnicalScore(technical);
        
        // 적극이는 기술적 분석에 1.2배 가중치
        double combinedScore = (sentimentScore + technicalScore * 1.2) / 2.0;
        
        String action;
        if (combinedScore >= 0.4) action = "BUY";
        else if (combinedScore >= 0.1) action = "WEAK_BUY";
        else if (combinedScore >= -0.3) action = "HOLD";
        else action = "SELL";
        
        String reasoning = String.format("FinBERT 감정점수: %.2f, 기술적점수: %.2f, 적극이 가중점수: %.2f", 
                sentimentScore, technicalScore, combinedScore);
        
        return TradingSignal.builder()
            .action(action)
            .strength(Math.abs(combinedScore))
            .confidence(sentiment.getConfidence() != null ? sentiment.getConfidence() : 0.5)
            .reasoning(reasoning)
            .build();
    }

    /**
     * 균형이 매매 신호 생성
     */
    private TradingSignal generateGyunhyungTradingSignal(SentimentResponseDto sentiment, TechnicalAnalysisResult technical) {
        double sentimentScore = sentiment.getSuccess() ? sentiment.getSentimentScore() : 0.0;
        double technicalScore = deriveNormalizedTechnicalScore(technical);
        
        // 균형이는 두 점수를 동등하게 평가하되 보수적으로
        double combinedScore = (sentimentScore + technicalScore) / 2.0 * 0.8;
        
        String action;
        if (combinedScore >= 0.5) action = "WEAK_BUY";
        else if (combinedScore >= -0.5) action = "HOLD";
        else action = "WEAK_SELL";
        
        String reasoning = String.format("FinBERT 감정점수: %.2f, 기술적점수: %.2f, 균형이 보수점수: %.2f", 
                sentimentScore, technicalScore, combinedScore);
        
        return TradingSignal.builder()
            .action(action)
            .strength(Math.abs(combinedScore))
            .confidence(sentiment.getConfidence() != null ? sentiment.getConfidence() : 0.5)
            .reasoning(reasoning)
            .build();
    }

    /**
     * 조심이 매매 신호 생성
     */
    private TradingSignal generateJosimTradingSignal(SentimentResponseDto sentiment, TechnicalAnalysisResult technical) {
        double sentimentScore = sentiment.getSuccess() ? sentiment.getSentimentScore() : 0.0;
        double technicalScore = deriveNormalizedTechnicalScore(technical);
        
        // 조심이는 부정 신호에 1.5배 가중치
        double adjustedSentiment = sentimentScore < 0 ? sentimentScore * 1.5 : sentimentScore * 0.7;
        double combinedScore = (adjustedSentiment + technicalScore) / 2.0;
        
        String action;
        if (combinedScore >= 0.7) action = "WEAK_BUY";
        else if (combinedScore >= 0.3) action = "WATCH";
        else if (combinedScore >= -0.1) action = "HOLD";
        else if (combinedScore >= -0.3) action = "WEAK_SELL";
        else action = "SELL";
        
        String reasoning = String.format("FinBERT 감정점수: %.2f, 기술적점수: %.2f, 조심이 보수점수: %.2f", 
                sentimentScore, technicalScore, combinedScore);
        
        return TradingSignal.builder()
            .action(action)
            .strength(Math.abs(combinedScore))
            .confidence(sentiment.getConfidence() != null ? sentiment.getConfidence() : 0.5)
            .reasoning(reasoning)
            .build();
    }

    /**
     * AI 전략 결과 저장
     * 
     * EC2 DB AI_STRATEGY 테이블에 전략 실행 결과 저장
     * 
     * @param user 사용자 정보
     * @param stockCode 종목 코드
     * @param characterName 캐릭터명 (화끈이/적극이/균형이/조심이)
     * @param signal 매매 신호
     * @param sentiment 감정 분석 결과
     * @param technical 기술적 분석 결과
     * @return 저장된 AI 전략 엔티티
     */
    private AiStrategy saveStrategyResult(User user, String stockCode, String characterName, 
                                        TradingSignal signal, SentimentResponseDto sentiment, 
                                        TechnicalAnalysisResult technical) {
        try {
            // 전략 실행 결과를 프롬프트로 생성
            String prompt = generateStrategyPrompt(stockCode, characterName, signal, sentiment, technical);
            
            // AI 전략 엔티티 생성
            AiStrategy strategy = AiStrategy.builder()
                .userId(user.getUserId())
                .strategy(characterName)
                .prompt(prompt)
                .build();
            
            // DB에 저장
            AiStrategy savedStrategy = aiStrategyRepository.save(strategy);
            
            log.info("AI 전략 결과 저장 완료: strategyId={}, userId={}, character={}, stockCode={}, action={}", 
                    savedStrategy.getStrategyId(), user.getUserId(), characterName, stockCode, signal.getAction());
            
            return savedStrategy;
            
        } catch (Exception e) {
            log.error("AI 전략 결과 저장 실패: userId={}, character={}, stockCode={}", 
                    user.getUserId(), characterName, stockCode, e);
            // 저장 실패 시에도 null이 아닌 빈 객체 반환
            return AiStrategy.builder()
                .userId(user.getUserId())
                .strategy(characterName)
                .prompt("저장 실패: " + e.getMessage())
                .build();
        }
    }

    /**
     * 전략 실행 결과를 프롬프트로 생성
     * 
     * @param stockCode 종목 코드
     * @param characterName 캐릭터명
     * @param signal 매매 신호
     * @param sentiment 감정 분석 결과
     * @param technical 기술적 분석 결과
     * @return 생성된 프롬프트
     */
    private String generateStrategyPrompt(String stockCode, String characterName, TradingSignal signal, 
                                        SentimentResponseDto sentiment, TechnicalAnalysisResult technical) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append(String.format("[%s 전략 실행 결과]\n", characterName));
        promptBuilder.append(String.format("종목: %s\n", stockCode));
        promptBuilder.append(String.format("매매 신호: %s (강도: %.2f, 신뢰도: %.2f)\n", 
                signal.getAction(), signal.getStrength(), signal.getConfidence()));
        
        if (sentiment.getSuccess()) {
            promptBuilder.append(String.format("FinBERT 감정 분석: %.3f (신뢰도: %.2f)\n", 
                    sentiment.getSentimentScore(), sentiment.getConfidence()));
        } else {
            promptBuilder.append("FinBERT 감정 분석: 실패\n");
        }
        
        promptBuilder.append(String.format("기술적 분석: %s (강도: %d/10)\n", 
                technical.getOverallSignal(), technical.getSignalStrength() != null ? technical.getSignalStrength() : 0));
        promptBuilder.append(String.format("판단 근거: %s\n", technical.getSignalReason() != null ? technical.getSignalReason() : signal.getReasoning()));
        
        return promptBuilder.toString();
    }

    /**
     * 오류 추천 결과 생성
     */
    private CharacterTradingRecommendation createErrorRecommendation(String characterName, Long userId, 
                                                                   String stockCode, String errorMessage) {
        return CharacterTradingRecommendation.builder()
            .characterName(characterName)
            .userId(userId)
            .stockCode(stockCode)
            .success(false)
            .errorMessage(errorMessage)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
