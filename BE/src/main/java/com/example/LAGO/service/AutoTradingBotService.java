package com.example.LAGO.service;

import com.example.LAGO.constants.TradingConstants;
import com.example.LAGO.domain.Account;
import com.example.LAGO.domain.News;
import com.example.LAGO.domain.TradeType;
import com.example.LAGO.domain.User;
import com.example.LAGO.dto.request.TradeRequest;
import com.example.LAGO.dto.response.TechnicalAnalysisResult;
import com.example.LAGO.repository.AccountRepository;
import com.example.LAGO.repository.NewsRepository;
import com.example.LAGO.repository.UserRepository;
import com.example.LAGO.realtime.AiOrderStreamPublisher;
import com.example.LAGO.realtime.RealtimeDataService;
import com.example.LAGO.repository.TicksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FinBERT 뉴스분석 + 기술적분석 기반 AI 자동매매봇 서비스
 * 
 * 핵심 기능:
 * - 매분마다 실행되는 자동매매 스케줄러
 * - 뉴스 감정분석 점수와 기술적분석 통합 판단
 * - 각 AI 봇의 성향별 차별화된 매매 전략
 * - 주문은 Redis Stream에 넣고, 소비자가 기존 매매 처리로 체결한다
 * 
 * @author LAGO D203팀
 * @since 2025-08-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoTradingBotService {

    // ======================== 의존성 주입 ========================
    
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final NewsRepository newsRepository;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final TicksRepository ticksRepository;
    private final AiOrderStreamPublisher aiOrderStreamPublisher;
    private final NewsService newsService;
    private final RealtimeDataService realtimeDataService;
    private final AtomicBoolean cycleRunning = new AtomicBoolean(false);
    
    // ======================== 상수 정의 ========================
    
    /** 삼성전자 종목 코드 */
    private static final String SAMSUNG_STOCK_CODE = "005930";
    
    /** AI 봇 계좌 타입 */
    private static final Integer AI_BOT_ACCOUNT_TYPE = 2;
    
    /** 매매 신호 임계값 */
    private static final double BUY_THRESHOLD = 0.1;  // 낮춤: 더 쉽게 매수 신호
    private static final double SELL_THRESHOLD = -0.1; // 낮춤: 더 쉽게 매도 신호
    
    /** 기본 매매 수량 */
    private static final int DEFAULT_QUANTITY = 1;
    
    // ======================== 메인 스케줄러 ========================
    
    /**
     * AI 자동매매 메인 스케줄러
     * 매분마다 실행되어 모든 AI 봇의 매매를 처리
     */
    @Scheduled(fixedRate = 60000) // 매분 실행 (60초)
    public void executeAutoTrading() {
        if (!cycleRunning.compareAndSet(false, true)) {
            log.info("이전 매매 주기가 아직 끝나서 이번 주기는 건너뜀");
            return;
        }
        log.info("🤖 === AI 자동매매 실행 시작: {} ===", LocalDateTime.now());
        
        try {
            // 1. 활성 AI 봇들 조회
            List<User> activeBots = getActiveAiBots();
            if (activeBots.isEmpty()) {
                log.info("⚠️ 활성 AI 봇이 없습니다. 다음 주기를 대기합니다.");
                return;
            }
            
            log.info("📊 활성 AI 봇 {}개 발견, 매매 분석을 시작합니다.", activeBots.size());
            
            // 2. 삼성전자 뉴스 감정분석 점수 조회
            double sentimentScore = getLatestSamsungSentiment();
            log.info("📰 삼성전자 뉴스 감정점수: {}", sentimentScore);
            
            // 3. 삼성전자 기술적 분석
            TechnicalAnalysisResult technical = getTechnicalAnalysis();
            log.info("📈 삼성전자 기술적분석: RSI={}, MACD={}", 
                    technical != null ? technical.getRsi() : "N/A",
                    technical != null ? technical.getMacdLine() : "N/A");
            
            // 4. 각 봇별 매매 실행 (병렬 처리)
            activeBots.stream().forEach(bot -> {
                try {
                    executeTradeForBot(bot, sentimentScore, technical);
                } catch (Exception e) {
                    log.error("🔥 AI 봇 {} 매매 실행 중 오류: {}", bot.getNickname(), e.getMessage(), e);
                }
            });
            
            log.info("✅ === 모든 AI 봇 매매 실행 완료: {} ===", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("🔥 자동매매 스케줄러 실행 중 오류 발생", e);
        } finally {
            cycleRunning.set(false);
        }
    }
    
    // ======================== AI 봇 조회 ========================
    
    /**
     * Type 2 계좌를 보유한 활성 AI 봇들 조회
     */
    private List<User> getActiveAiBots() {
        try {
            List<User> aiBots = userRepository.findByIsAiTrueAndDeletedAtIsNull();
            
            // Type 2 계좌 보유 여부 확인
            List<User> activeBots = aiBots.stream()
                    .filter(this::hasAiBotAccount)
                    .toList();
            
            log.debug("🔍 총 AI 봇: {}개, 활성 봇: {}개", aiBots.size(), activeBots.size());
            return activeBots;
            
        } catch (Exception e) {
            log.error("🔥 활성 AI 봇 조회 실패", e);
            return List.of();
        }
    }
    
    /**
     * AI 봇의 Type 2 계좌 보유 여부 확인
     */
    private boolean hasAiBotAccount(User aiBot) {
        try {
            return accountRepository.findByUserIdAndType(
                    aiBot.getUserId(), 
                    AI_BOT_ACCOUNT_TYPE
            ).isPresent();
            
        } catch (Exception e) {
            log.warn("⚠️ AI 봇 {}의 계좌 확인 실패: {}", aiBot.getNickname(), e.getMessage());
            return false;
        }
    }
    
    // ======================== 뉴스 감정분석 ========================
    
    /**
     * 최근 뉴스에서 삼성전자 관련 감정분석 점수 조회
     * @return 감정점수 (-1.0 ~ 1.0, 없으면 0.0)
     */
    private double getLatestSamsungSentiment() {
        try {
            newsService.collectSingleWatchlistNews(
                    SAMSUNG_STOCK_CODE, "삼성전자", List.of("삼성전자", "삼전"), 3);

            // 최근 2시간 이내 뉴스 중 삼성전자 관련 뉴스 조회
            LocalDateTime since = LocalDateTime.now().minusHours(2);
            
            List<News> recentNews = newsRepository.findByPublishedAtAfterAndTitleContainingOrContentContaining(
                    since, "삼성전자", "삼성전자"
            );
            
            if (recentNews.isEmpty()) {
                log.debug("📰 최근 삼성전자 뉴스가 없음, 중립 점수 사용");
                return 0.0;
            }
            
            // sentiment 필드에서 점수 추출하여 평균 계산
            double averageSentiment = recentNews.stream()
                    .filter(news -> news.getSentiment() != null)
                    .map(this::parseSentimentScore)
                    .filter(score -> score != null)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            averageSentiment = Math.max(-1.0, Math.min(1.0, averageSentiment));
            
            log.debug("📊 삼성전자 뉴스 {}개 분석, 평균 감정점수: {}", recentNews.size(), averageSentiment);
            
            return averageSentiment;
            
        } catch (Exception e) {
            log.error("🔥 뉴스 감정분석 점수 조회 실패", e);
            return 0.0;
        }
    }
    
    /**
     * sentiment 문자열에서 숫자 점수 추출
     */
    private Double parseSentimentScore(News news) {
        try {
            String sentiment = news.getSentiment();
            if (sentiment == null || sentiment.isBlank()) {
                return null;
            }
            String trimmed = sentiment.trim();

            if (trimmed.contains(":")) {
                String[] parts = trimmed.split(":");
                if (parts.length > 1) {
                    double value = Double.parseDouble(parts[1].trim());
                    if (parts[0].toLowerCase().contains("negative")) {
                        value = -Math.abs(value);
                    }
                    return clampSentiment(value);
                }
            }

            try {
                return clampSentiment(Double.parseDouble(trimmed));
            } catch (NumberFormatException ignored) {
                String label = trimmed.toUpperCase();
                if (label.contains("POSITIVE")) {
                    return 1.0;
                }
                if (label.contains("NEGATIVE")) {
                    return -1.0;
                }
                if (label.contains("NEUTRAL")) {
                    return 0.0;
                }
                return null;
            }
        } catch (Exception e) {
            log.debug("sentiment 파싱 실패: {}", news.getSentiment());
            return null;
        }
    }

    private double clampSentiment(double value) {
        if (Math.abs(value) > 1.0) {
            value = value / 100.0;
        }
        return Math.max(-1.0, Math.min(1.0, value));
    }
    
    // ======================== 기술적 분석 ========================
    
    /**
     * 삼성전자 기술적 분석 결과 조회
     */
    private TechnicalAnalysisResult getTechnicalAnalysis() {
        try {
            return technicalAnalysisService.analyzeStock(SAMSUNG_STOCK_CODE);
            
        } catch (Exception e) {
            log.error("🔥 기술적 분석 실행 실패", e);
            return null;
        }
    }
    
    // ======================== 개별 봇 매매 실행 ========================
    
    /**
     * 개별 AI 봇의 매매 실행
     */
    private void executeTradeForBot(User bot, double sentimentScore, TechnicalAnalysisResult technical) {
        log.info("🤖 AI 봇 [{}] 매매 분석 시작", bot.getNickname());
        
        try {
            // 1. 봇의 계좌 조회
            Account account = accountRepository.findByUserIdAndType(
                    bot.getUserId(), AI_BOT_ACCOUNT_TYPE
            ).orElse(null);
            
            if (account == null) {
                log.warn("⚠️ AI 봇 [{}]의 Type 2 계좌를 찾을 수 없음", bot.getNickname());
                return;
            }
            
            // 2. 봇 성향별 매매 신호 계산
            TradingDecision decision = calculateTradingDecision(bot, sentimentScore, technical);
            
            // 3. 매매 실행
            if (decision.getAction() != TradeAction.HOLD) {
                executeTrade(bot, decision);
            } else {
                log.info("📊 AI 봇 [{}]: 현재 관망 (통합점수: {:.3f})", 
                        bot.getNickname(), decision.getScore());
            }
            
        } catch (Exception e) {
            log.error("🔥 AI 봇 [{}] 매매 실행 실패", bot.getNickname(), e);
        }
    }
    
    /**
     * 봇 성향별 매매 신호. README와 TradingStrategyService의 가중치를 따른다.
     */
    private TradingDecision calculateTradingDecision(User bot, double sentiment,
                                                   TechnicalAnalysisResult technical) {
        
        String nickname = bot.getNickname();
        if (nickname == null) nickname = "균형이";
        
        double technicalScore = technicalScore(technical);
        double priceChange = priceChangeScore(technical);
        
        TradeAction action = TradeAction.HOLD;
        double finalScore;
        String strategy;
        
        switch (nickname) {
            case "화끈이": // 뉴스 감정 1.3배
                finalScore = (sentiment * 1.3 + technicalScore + priceChange) / 3.0;
                strategy = "뉴스 1.3배";
                if (finalScore >= 0.3) {
                    action = TradeAction.BUY;
                } else if (finalScore < -0.2) {
                    action = TradeAction.SELL;
                }
                break;
                
            case "적극이": // 기술적 분석 1.2배. RSI, MACD는 technicalScore에 들어 있다
                finalScore = (sentiment + technicalScore * 1.2 + priceChange) / 3.0;
                strategy = "기술 1.2배";
                if (finalScore >= 0.1) {
                    action = TradeAction.BUY;
                } else if (finalScore < -0.3) {
                    action = TradeAction.SELL;
                }
                break;
                
            case "균형이": // 뉴스와 기술을 같게 보고 0.8로 줄인다
                finalScore = (sentiment + technicalScore + priceChange) / 3.0 * 0.8;
                strategy = "뉴스/기술 동등";
                if (finalScore >= 0.5) {
                    action = TradeAction.BUY;
                } else if (finalScore < -0.5) {
                    action = TradeAction.SELL;
                }
                break;
                
            case "조심이": // 부정 뉴스만 1.5배
                double adjustedSentiment = sentiment < 0 ? sentiment * 1.5 : sentiment * 0.7;
                finalScore = (adjustedSentiment + technicalScore + priceChange) / 3.0;
                strategy = "부정 신호 1.5배";
                if (finalScore >= 0.7) {
                    action = TradeAction.BUY;
                } else if (finalScore < -0.3) {
                    action = TradeAction.SELL;
                }
                break;
                
            default:
                finalScore = (sentiment + technicalScore + priceChange) / 3.0 * 0.8;
                strategy = "기본 균형";
                if (finalScore >= BUY_THRESHOLD) {
                    action = TradeAction.BUY;
                } else if (finalScore <= SELL_THRESHOLD) {
                    action = TradeAction.SELL;
                }
                break;
        }
        
        log.info("📊 AI 봇 [{}] 분석결과 - 감정:{:.3f}, 기술:{:.3f}, 통합:{:.3f} → {} ({})", 
                nickname, sentiment, technicalScore, finalScore, action, strategy);
        
        return new TradingDecision(action, finalScore, DEFAULT_QUANTITY);
    }
    
    /**
     * 기술적 분석 종합 신호. RSI, MACD, 볼린저, 이평, 크로스가 반영된 값이다.
     */
    private double technicalScore(TechnicalAnalysisResult technical) {
        if (technical != null && technical.getOverallSignal() != null && technical.getSignalStrength() != null) {
            double base = Math.max(0, Math.min(10, technical.getSignalStrength())) / 10.0;
            return switch (technical.getOverallSignal()) {
                case "BUY", "BULLISH" -> base;
                case "SELL", "BEARISH" -> -base;
                default -> 0.0;
            };
        }
        return calculateTechnicalScore(technical);
    }

    /**
     * KIS 전일대비 등락률을 -1~1로 줄인다. 5% 움직임을 만점으로 본다.
     */
    private double priceChangeScore(TechnicalAnalysisResult technical) {
        if (technical == null || technical.getFluctuationRate() == null) {
            return 0.0;
        }
        return Math.max(-1.0, Math.min(1.0, technical.getFluctuationRate() / 5.0));
    }

    /**
     * 기술적 분석 결과를 점수로 변환 (-1.0 ~ 1.0)
     */
    private double calculateTechnicalScore(TechnicalAnalysisResult technical) {
        if (technical == null) return 0.0;
        
        double score = 0.0;
        int indicators = 0;
        
        // RSI 점수 (0~100 → -1~1)
        if (technical.getRsi() != null) {
            double rsi = technical.getRsi();
            if (rsi < 30) {
                score += 0.8; // 과매도, 매수 신호
            } else if (rsi > 70) {
                score -= 0.8; // 과매수, 매도 신호
            } else {
                score += (50 - rsi) / 50.0; // 50 기준으로 정규화
            }
            indicators++;
        }
        
        // MACD 점수 (히스토그램 사용)
        if (technical.getHistogram() != null) {
            score += technical.getHistogram() > 0 ? 0.5 : -0.5;
            indicators++;
        }
        
        return indicators > 0 ? score / indicators : 0.0;
    }
    
    /**
     * 삼성전자 현재가 조회
     */
    private Integer getCurrentPrice() {
        try {
            Integer livePrice = realtimeDataService.getLatestPrice(SAMSUNG_STOCK_CODE);
            if (livePrice != null && livePrice > 0) {
                log.debug("삼성전자 KIS 실시간가: {}원", livePrice);
                return livePrice;
            }

            Integer currentPrice = ticksRepository.findLatestClosePriceByStockCode(SAMSUNG_STOCK_CODE);
            if (currentPrice != null && currentPrice > 0) {
                log.debug("📊 삼성전자 현재가: {}원", currentPrice);
                return currentPrice;
            }

            log.warn("⚠️ 삼성전자 실시간 가격이 없어 이번 주문은 건너뜀");
            return null;
            
        } catch (Exception e) {
            log.error("🔥 현재가 조회 실패", e);
            return null;
        }
    }
    
    /**
     * 주문을 Redis Stream에 넣는다. 체결은 AiOrderStreamConsumer가 한다.
     */
    private void executeTrade(User bot, TradingDecision decision) {
        try {
            Integer currentPrice = getCurrentPrice();
            if (currentPrice == null) {
                return;
            }

            TradeRequest request = TradeRequest.builder()
                    .userId(bot.getUserId())
                    .stockCode(SAMSUNG_STOCK_CODE)
                    .tradeType(decision.getAction() == TradeAction.BUY ? TradeType.BUY : TradeType.SELL)
                    .quantity(decision.getQuantity())
                    .price(currentPrice)
                    .accountType(AI_BOT_ACCOUNT_TYPE)
                    .build();

            aiOrderStreamPublisher.publish(request);
            log.info("AI 봇 [{}] {} 주문 적재: {}주 @{} (점수: {})",
                    bot.getNickname(),
                    decision.getAction() == TradeAction.BUY ? "매수" : "매도",
                    decision.getQuantity(),
                    currentPrice,
                    decision.getScore());
        } catch (Exception e) {
            log.error("AI 봇 [{}] 주문 스트림 적재 실패", bot.getNickname(), e);
        }
    }
    
    // ======================== 내부 클래스 ========================
    
    /**
     * 매매 결정 결과
     */
    private static class TradingDecision {
        private final TradeAction action;
        private final double score;
        private final int quantity;
        
        public TradingDecision(TradeAction action, double score, int quantity) {
            this.action = action;
            this.score = score;
            this.quantity = quantity;
        }
        
        public TradeAction getAction() { return action; }
        public double getScore() { return score; }
        public int getQuantity() { return quantity; }
    }
    
    /**
     * 매매 액션
     */
    private enum TradeAction {
        BUY, SELL, HOLD
    }
}