package com.example.LAGO.ai.sentiment;

import com.example.LAGO.ai.sentiment.dto.SentimentRequestDto;
import com.example.LAGO.ai.sentiment.dto.SentimentResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * FinBERT 감정 분석 HTTP 클라이언트
 * 
 * 지침서 명세:
 * - FinBERT Flask 서버와 HTTP 통신하여 뉴스 감정 분석 수행
 * - Java 21 Virtual Thread를 활용한 고성능 비동기 처리
 * - 감정 점수(-1.0 ~ 1.0)를 캐릭터별 매매 전략에 활용
 * 
 * 기술 스택:
 * - Java 21 Virtual Thread: 대량 동시 요청 처리
 * - RestTemplate: HTTP 통신
 * - CompletableFuture: 비동기 응답 처리
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisClient {

    // ======================== 의존성 주입 ========================

    /**
     * HTTP 통신을 위한 RestTemplate
     */
    private final RestTemplate restTemplate;

    /**
     * JSON 파싱을 위한 ObjectMapper
     */
    private final ObjectMapper objectMapper;

    // ======================== 설정 값 ========================

    /**
     * FinBERT Flask 서버 호스트
     * application.yml에서 주입: finbert.server.host
     */
    @Value("${finbert.server.host:http://localhost:5000}")
    private String finbertServerHost;

    /**
     * FinBERT 감정 분석 API 엔드포인트
     */
    @Value("${finbert.server.analyze-endpoint:/analyze}")
    private String analyzeEndpoint;

    /**
     * FinBERT 서버 상태 확인 엔드포인트
     */
    @Value("${finbert.server.health-endpoint:/health}")
    private String healthEndpoint;

    /**
     * HTTP 요청 타임아웃 (밀리초)
     */
    @Value("${finbert.client.timeout:10000}")
    private int requestTimeout;

    // ======================== Virtual Thread Executor ========================

    /**
     * Java 21 Virtual Thread를 활용한 고성능 비동기 처리
     * FinBERT 분석 요청의 대량 동시 처리 가능
     */
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // ======================== 공개 메서드 ========================

    /**
     * 뉴스 URL에 대한 감정 분석 수행 (비동기)
     * 
     * 처리 흐름:
     * 1. Virtual Thread로 비동기 요청 시작
     * 2. FinBERT Flask 서버에 HTTP POST 요청
     * 3. 응답 파싱 및 캐릭터별 매매 신호 생성
     * 4. SentimentResponseDto 반환
     * 
     * @param request 감정 분석 요청 정보
     * @return 비동기 감정 분석 결과
     */
    public CompletableFuture<SentimentResponseDto> analyzeSentimentAsync(SentimentRequestDto request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                log.info("FinBERT 감정 분석 시작: url={}, userId={}, stockCode={}", 
                        request.getUrl(), request.getUserId(), request.getStockCode());

                // 1. FinBERT 서버 상태 확인
                if (!isFinbertServerHealthy()) {
                    return createErrorResponse("FinBERT 서버가 응답하지 않습니다", startTime);
                }

                // 2. HTTP 요청 준비
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> requestBody = Map.of(
                    "url", request.getUrl()
                );

                HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

                // 3. FinBERT 서버에 분석 요청
                String finbertUrl = finbertServerHost + analyzeEndpoint;
                ResponseEntity<String> response = restTemplate.exchange(
                    finbertUrl,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
                );

                // 4. 응답 파싱
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return parseFinbertResponse(response.getBody(), startTime);
                } else {
                    log.error("FinBERT 서버 응답 오류: status={}, body={}", 
                            response.getStatusCode(), response.getBody());
                    return createErrorResponse("FinBERT 서버 응답 오류", startTime);
                }

            } catch (RestClientException e) {
                log.error("FinBERT 서버 통신 실패: url={}", request.getUrl(), e);
                return createErrorResponse("FinBERT 서버 통신 실패: " + e.getMessage(), System.currentTimeMillis());
            } catch (Exception e) {
                log.error("FinBERT 감정 분석 처리 중 예외 발생: url={}", request.getUrl(), e);
                return createErrorResponse("감정 분석 처리 실패: " + e.getMessage(), System.currentTimeMillis());
            }
        }, virtualThreadExecutor);
    }

    /**
     * 뉴스 URL에 대한 감정 분석 수행 (동기)
     * 
     * @param request 감정 분석 요청 정보
     * @return 감정 분석 결과
     */
    public SentimentResponseDto analyzeSentiment(SentimentRequestDto request) {
        try {
            return analyzeSentimentAsync(request).join();
        } catch (Exception e) {
            log.error("동기 감정 분석 실패: url={}", request.getUrl(), e);
            return createErrorResponse("감정 분석 실패: " + e.getMessage(), System.currentTimeMillis());
        }
    }

    // ======================== 내부 메서드 ========================

    /**
     * FinBERT 서버 상태 확인
     * 
     * @return 서버 정상 여부
     */
    private boolean isFinbertServerHealthy() {
        try {
            String healthUrl = finbertServerHost + healthEndpoint;
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode healthResponse = objectMapper.readTree(response.getBody());
                return "healthy".equals(healthResponse.path("status").asText()) &&
                       healthResponse.path("model_loaded").asBoolean();
            }
            return false;
        } catch (Exception e) {
            log.warn("FinBERT 서버 상태 확인 실패", e);
            return false;
        }
    }

    /**
     * FinBERT 서버 응답 파싱
     * 
     * @param responseBody FinBERT 서버 응답 JSON
     * @param startTime 요청 시작 시간
     * @return 파싱된 감정 분석 결과
     */
    private SentimentResponseDto parseFinbertResponse(String responseBody, long startTime) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            
            // FinBERT 응답에서 핵심 정보 추출
            double sentimentScore = jsonResponse.path("sentiment_score").asDouble();
            String sentimentLabel = jsonResponse.path("sentiment").asText();
            double confidence = jsonResponse.path("confidence").asDouble();
            String newsTitle = jsonResponse.path("title").asText();
            String newsSummary = jsonResponse.path("content").asText();

            // 캐릭터별 매매 신호 생성
            CharacterTradingSignals signals = generateCharacterSignals(sentimentScore, confidence);

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("FinBERT 감정 분석 완료: score={}, label={}, confidence={}, processingTime={}ms", 
                    sentimentScore, sentimentLabel, confidence, processingTime);

            return SentimentResponseDto.builder()
                .success(true)
                .sentimentScore(sentimentScore)
                .sentimentLabel(sentimentLabel)
                .confidence(confidence)
                .newsTitle(newsTitle)
                .newsSummary(newsSummary.length() > 200 ? newsSummary.substring(0, 200) + "..." : newsSummary)
                .hwakkeunSignal(signals.hwakkeunSignal)
                .jeokgeukSignal(signals.jeokgeukSignal)
                .gyunhyungSignal(signals.gyunhyungSignal)
                .josimSignal(signals.josimSignal)
                .analyzedAt(LocalDateTime.now())
                .processingTimeMs(processingTime)
                .build();

        } catch (Exception e) {
            log.error("FinBERT 응답 파싱 실패: responseBody={}", responseBody, e);
            return createErrorResponse("응답 파싱 실패: " + e.getMessage(), startTime);
        }
    }

    /**
     * 감정 점수를 기반으로 캐릭터별 매매 신호 생성
     * 
     * 캐릭터별 특성:
     * - 화끈이: 긍정 신호에 강하게 반응하는 공격적 매매
     * - 적극이: 성장주 관점에서 적극적 매매
     * - 균형이: 안정적 분산투자 관점에서 신중한 매매
     * - 조심이: 부정 신호에 민감하게 반응하는 보수적 매매
     * 
     * @param sentimentScore FinBERT 감정 점수 (-1.0 ~ 1.0)
     * @param confidence 분석 신뢰도 (0.0 ~ 1.0)
     * @return 캐릭터별 매매 신호
     */
    private CharacterTradingSignals generateCharacterSignals(double sentimentScore, double confidence) {
        // 신뢰도가 낮으면 신호 강도 조정
        double adjustedScore = sentimentScore * confidence;

        String hwakkeunSignal = generateHwakkeunSignal(adjustedScore);
        String jeokgeukSignal = generateJeokgeukSignal(adjustedScore);
        String gyunhyungSignal = generateGyunhyungSignal(adjustedScore);
        String josimSignal = generateJosimSignal(adjustedScore);

        return new CharacterTradingSignals(hwakkeunSignal, jeokgeukSignal, gyunhyungSignal, josimSignal);
    }

    /**
     * 화끈이 매매 신호 생성 (공격적 투자자)
     */
    private String generateHwakkeunSignal(double score) {
        if (score >= 0.5) return "STRONG_BUY";
        if (score >= 0.2) return "BUY";
        if (score >= -0.1) return "HOLD";
        if (score >= -0.3) return "WEAK_SELL";
        return "STRONG_SELL";
    }

    /**
     * 적극이 매매 신호 생성 (성장주 투자자)
     */
    private String generateJeokgeukSignal(double score) {
        if (score >= 0.3) return "BUY";
        if (score >= 0.1) return "WEAK_BUY";
        if (score >= -0.2) return "HOLD";
        if (score >= -0.4) return "WEAK_SELL";
        return "SELL";
    }

    /**
     * 균형이 매매 신호 생성 (균형 투자자)
     */
    private String generateGyunhyungSignal(double score) {
        if (score >= 0.4) return "WEAK_BUY";
        if (score >= -0.4) return "HOLD";
        return "WEAK_SELL";
    }

    /**
     * 조심이 매매 신호 생성 (보수적 투자자)
     */
    private String generateJosimSignal(double score) {
        if (score >= 0.6) return "WEAK_BUY";
        if (score >= 0.2) return "WATCH";
        if (score >= -0.1) return "HOLD";
        if (score >= -0.2) return "WEAK_SELL";
        return "SELL";
    }

    /**
     * 오류 응답 생성
     * 
     * @param errorMessage 오류 메시지
     * @param startTime 요청 시작 시간
     * @return 오류 응답 DTO
     */
    private SentimentResponseDto createErrorResponse(String errorMessage, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        return SentimentResponseDto.builder()
            .success(false)
            .errorMessage(errorMessage)
            .analyzedAt(LocalDateTime.now())
            .processingTimeMs(processingTime)
            .build();
    }

    // ======================== 내부 클래스 ========================

    /**
     * 캐릭터별 매매 신호를 담는 내부 클래스
     */
    private record CharacterTradingSignals(
        String hwakkeunSignal,
        String jeokgeukSignal,
        String gyunhyungSignal,
        String josimSignal
    ) {}
}
