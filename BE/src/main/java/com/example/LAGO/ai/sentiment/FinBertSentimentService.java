package com.example.LAGO.ai.sentiment;

import com.example.LAGO.ai.sentiment.dto.SentimentRequestDto;
import com.example.LAGO.ai.sentiment.dto.SentimentResponseDto;
import com.example.LAGO.dto.request.FinBertTextRequest;
import com.example.LAGO.dto.response.FinBertResponse;
import com.example.LAGO.dto.response.NewsAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FinBERT 감정 분석 서비스
 * 
 * 지침서 명세:
 * - FinBERT Flask 서버를 활용한 뉴스 감정 분석 서비스
 * - 감정 점수(-1.0 ~ 1.0)를 캐릭터별 AI 매매 전략에 활용
 * - Java 21 Virtual Thread를 활용한 고성능 비동기 처리
 * - Redis 캐싱을 통한 중복 분석 방지 및 성능 최적화
 * 
 * 캐릭터별 활용 방식:
 * - 화끈이: 긍정 신호에 강하게 반응하는 공격적 매매
 * - 적극이: 성장주 관점에서 감정 점수 활용
 * - 균형이: 분산투자 관점에서 신중한 감정 점수 반영
 * - 조심이: 부정 신호에 민감하게 반응하는 보수적 매매
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinBertSentimentService {

    // ======================== 의존성 주입 ========================

    /**
     * FinBERT HTTP 클라이언트
     */
    private final SentimentAnalysisClient sentimentAnalysisClient;
    
    /**
     * RestTemplate for direct FinBERT API calls
     */
    private final RestTemplate restTemplate;
    
    @Value("${finbert.server.host:http://localhost:5000}")
    private String finbertServerHost;

    // ======================== Virtual Thread Executor ========================
    // Virtual Thread 처리는 SentimentAnalysisClient에서 담당

    // ======================== 공개 메서드 ========================

    /**
     * 단일 뉴스 URL에 대한 감정 분석 (비동기)
     * 
     * @param request 감정 분석 요청
     * @return 비동기 감정 분석 결과
     */
    public CompletableFuture<SentimentResponseDto> analyzeSingleNewsAsync(SentimentRequestDto request) {
        log.info("단일 뉴스 감정 분석 시작: url={}, userId={}", request.getUrl(), request.getUserId());
        
        return sentimentAnalysisClient.analyzeSentimentAsync(request)
            .thenApply(response -> {
                if (response.getSuccess()) {
                    log.info("단일 뉴스 감정 분석 완료: url={}, score={}, label={}", 
                            request.getUrl(), response.getSentimentScore(), response.getSentimentLabel());
                } else {
                    log.error("단일 뉴스 감정 분석 실패: url={}, error={}", 
                            request.getUrl(), response.getErrorMessage());
                }
                return response;
            });
    }

    /**
     * 단일 뉴스 URL에 대한 감정 분석 (동기)
     * 
     * @param request 감정 분석 요청
     * @return 감정 분석 결과
     */
    @Cacheable(value = "sentiment_analysis", key = "#request.url", unless = "#result.success == false")
    public SentimentResponseDto analyzeSingleNews(SentimentRequestDto request) {
        log.info("동기 뉴스 감정 분석 시작: url={}", request.getUrl());
        return sentimentAnalysisClient.analyzeSentiment(request);
    }

    /**
     * 다중 뉴스 URL에 대한 배치 감정 분석 (비동기)
     * 
     * Virtual Thread를 활용하여 여러 뉴스를 동시에 분석
     * 
     * @param requests 감정 분석 요청 리스트
     * @return 비동기 감정 분석 결과 리스트
     */
    public CompletableFuture<List<SentimentResponseDto>> analyzeBatchNewsAsync(List<SentimentRequestDto> requests) {
        log.info("배치 뉴스 감정 분석 시작: count={}", requests.size());

        // Virtual Thread로 동시 처리
        List<CompletableFuture<SentimentResponseDto>> futures = requests.stream()
            .map(this::analyzeSingleNewsAsync)
            .toList();

        // 모든 분석 완료 대기
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<SentimentResponseDto> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                
                long successCount = results.stream()
                    .mapToLong(r -> r.getSuccess() ? 1 : 0)
                    .sum();
                
                log.info("배치 뉴스 감정 분석 완료: total={}, success={}, failed={}", 
                        requests.size(), successCount, requests.size() - successCount);
                
                return results;
            });
    }

    /**
     * 특정 주식에 관련된 뉴스들의 종합 감정 점수 계산
     * 
     * 여러 뉴스의 감정 점수를 가중평균하여 종합 점수 산출
     * 
     * @param stockCode 주식 코드
     * @param newsUrls 관련 뉴스 URL 리스트
     * @param userId 요청 사용자 ID
     * @return 종합 감정 분석 결과
     */
    public CompletableFuture<SentimentResponseDto> analyzeStockRelatedNewsAsync(
            String stockCode, List<String> newsUrls, Long userId) {
        
        log.info("주식 관련 뉴스 종합 감정 분석 시작: stockCode={}, newsCount={}, userId={}", 
                stockCode, newsUrls.size(), userId);

        // 요청 리스트 생성
        List<SentimentRequestDto> requests = newsUrls.stream()
            .map(url -> SentimentRequestDto.builder()
                .url(url)
                .stockCode(stockCode)
                .userId(userId)
                .build())
            .toList();

        // 배치 분석 수행
        return analyzeBatchNewsAsync(requests)
            .thenApply(results -> calculateAggregatedSentiment(stockCode, results));
    }

    /**
     * 캐릭터별 맞춤 감정 분석
     * 
     * 각 캐릭터의 투자 성향에 맞춰 감정 점수를 조정
     * 
     * @param request 감정 분석 요청
     * @param characterName 캐릭터명 (화끈이, 적극이, 균형이, 조심이)
     * @return 캐릭터 맞춤 감정 분석 결과
     */
    public CompletableFuture<SentimentResponseDto> analyzeForCharacterAsync(
            SentimentRequestDto request, String characterName) {
        
        log.info("캐릭터별 맞춤 감정 분석 시작: character={}, url={}", characterName, request.getUrl());

        return analyzeSingleNewsAsync(request)
            .thenApply(response -> adjustSentimentForCharacter(response, characterName));
    }

    /**
     * URL 기반 뉴스 감정분석 (본문 + 이미지 추출 포함)
     * 
     * @param url 분석할 뉴스 URL
     * @return FinBERT 감정 분석 결과 (본문, 이미지 포함)
     */
    public NewsAnalysisResult analyzeNewsByUrl(String url) {
        try {
            // FinBERT 서버 상태 확인
            if (!isFinBertServerHealthy()) {
                log.warn("FinBERT 서버가 비정상 상태입니다. 기본값을 반환합니다.");
                return createDefaultNewsAnalysisResult();
            }

            String finbertUrl = finbertServerHost + "/analyze"; // URL 기반 분석 엔드포인트
            
            // 요청 데이터 구성
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            String requestBody = "{\"url\": \"" + url + "\"}";
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.info("FinBERT URL 기반 뉴스 분석 요청: {}", url);

            ResponseEntity<NewsAnalysisResult> response = restTemplate.exchange(
                    finbertUrl, HttpMethod.POST, entity, NewsAnalysisResult.class);

            NewsAnalysisResult result = response.getBody();
            
            if (result != null) {
                log.info("FinBERT URL 분석 완료 - 제목: {}, 본문길이: {}자, 이미지: {}개, 감정: {}", 
                        result.getTitle() != null ? result.getTitle().substring(0, Math.min(50, result.getTitle().length())) : "없음",
                        result.getContent() != null ? result.getContent().length() : 0,
                        result.getImages() != null ? result.getImages().size() : 0,
                        result.getLabel());
                return result;
            }

            log.warn("FinBERT URL 분석 응답이 null입니다. 기본값을 반환합니다.");
            return createDefaultNewsAnalysisResult();

        } catch (Exception e) {
            log.error("FinBERT URL 기반 뉴스 분석 실패: {}", e.getMessage());
            return createDefaultNewsAnalysisResult();
        }
    }

    /**
     * 텍스트 기반 감정분석 (뉴스 수집용)
     * 
     * @param text 분석할 텍스트 (제목 + 내용)
     * @return FinBERT 감정 분석 결과
     */
    public FinBertResponse analyzeTextSentiment(String text) {
        try {
            // FinBERT 서버 상태 확인
            if (!isFinBertServerHealthy()) {
                log.warn("FinBERT 서버가 비정상 상태입니다. 기본값을 반환합니다.");
                return createDefaultFinBertResponse();
            }

            String url = finbertServerHost + "/analyze-text"; // 새로운 엔드포인트
            
            FinBertTextRequest requestDto = FinBertTextRequest.builder()
                    .text(text)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            
            HttpEntity<FinBertTextRequest> entity = new HttpEntity<>(requestDto, headers);

            log.info("FinBERT 텍스트 감정분석 요청 - 텍스트 길이: {}자", text.length());

            ResponseEntity<FinBertResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, FinBertResponse.class);

            FinBertResponse result = response.getBody();
            
            if (result != null) {
                log.info("FinBERT 텍스트 분석 완료 - 결과: {}, 점수: {}, 신호: {}", 
                        result.getLabel(), result.getScore(), result.getTradingSignal());
                return result;
            }

            log.warn("FinBERT 텍스트 응답이 null입니다. 기본값을 반환합니다.");
            return createDefaultFinBertResponse();

        } catch (Exception e) {
            log.error("FinBERT 텍스트 감정분석 실패: {}", e.getMessage());
            return createDefaultFinBertResponse();
        }
    }

    /**
     * FinBERT 서버 상태 확인
     */
    private boolean isFinBertServerHealthy() {
        try {
            String url = finbertServerHost + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            log.debug("FinBERT 서버 상태 확인: {}", isHealthy ? "정상" : "비정상");
            
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("FinBERT 서버 연결 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * FinBERT 서버 연결 실패 시 기본 뉴스 분석 결과 생성
     */
    private NewsAnalysisResult createDefaultNewsAnalysisResult() {
        NewsAnalysisResult result = new NewsAnalysisResult();
        result.setTitle("제목을 가져올 수 없습니다");
        result.setContent("본문을 가져올 수 없습니다");
        result.setImages(java.util.Collections.emptyList());
        result.setLabel("NEUTRAL");
        result.setScore(0.0);
        result.setConfidence(0.0);
        result.setTradingSignal("HOLD");
        result.setConfidenceLevel("low");
        return result;
    }

    /**
     * FinBERT 서버 연결 실패 시 기본 응답 생성 (새로운 방식)
     */
    private FinBertResponse createDefaultFinBertResponse() {
        FinBertResponse response = new FinBertResponse();
        response.setLabel("NEUTRAL");
        response.setScore(0.0);
        response.setConfidence(0.0);
        response.setTradingSignal("HOLD");
        response.setConfidenceLevel("low");
        return response;
    }

    // ======================== 내부 메서드 ========================

    /**
     * 여러 뉴스의 감정 점수를 종합하여 최종 점수 계산
     * 
     * @param stockCode 주식 코드
     * @param results 개별 뉴스 감정 분석 결과
     * @return 종합 감정 분석 결과
     */
    private SentimentResponseDto calculateAggregatedSentiment(String stockCode, List<SentimentResponseDto> results) {
        // 성공한 분석 결과만 필터링
        List<SentimentResponseDto> successResults = results.stream()
            .filter(SentimentResponseDto::getSuccess)
            .toList();

        if (successResults.isEmpty()) {
            log.warn("주식 관련 뉴스 분석 결과 없음: stockCode={}", stockCode);
            return SentimentResponseDto.builder()
                .success(false)
                .errorMessage("분석 가능한 뉴스가 없습니다")
                .build();
        }

        // 가중평균 계산 (신뢰도를 가중치로 사용)
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        
        for (SentimentResponseDto result : successResults) {
            double weight = result.getConfidence() != null ? result.getConfidence() : 0.5;
            totalWeightedScore += result.getSentimentScore() * weight;
            totalWeight += weight;
        }

        double aggregatedScore = totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;
        double avgConfidence = successResults.stream()
            .mapToDouble(r -> r.getConfidence() != null ? r.getConfidence() : 0.5)
            .average()
            .orElse(0.5);

        // 종합 감정 라벨 결정
        String aggregatedLabel = determineAggregatedLabel(aggregatedScore);

        log.info("주식 관련 뉴스 종합 감정 분석 완료: stockCode={}, aggregatedScore={}, confidence={}, newsCount={}", 
                stockCode, aggregatedScore, avgConfidence, successResults.size());

        return SentimentResponseDto.builder()
            .success(true)
            .sentimentScore(aggregatedScore)
            .sentimentLabel(aggregatedLabel)
            .confidence(avgConfidence)
            .newsTitle(String.format("%s 관련 뉴스 %d건 종합 분석", stockCode, successResults.size()))
            .newsSummary(String.format("총 %d건의 뉴스를 분석하여 종합 감정 점수를 산출했습니다.", successResults.size()))
            .build();
    }

    /**
     * 종합 감정 점수를 기반으로 라벨 결정
     * 
     * @param score 종합 감정 점수
     * @return 감정 라벨
     */
    private String determineAggregatedLabel(double score) {
        if (score >= 0.1) return "POSITIVE";
        if (score <= -0.1) return "NEGATIVE";
        return "NEUTRAL";
    }

    /**
     * 캐릭터별 특성에 맞춰 감정 점수 조정
     * 
     * @param originalResponse 원본 감정 분석 결과
     * @param characterName 캐릭터명
     * @return 조정된 감정 분석 결과
     */
    private SentimentResponseDto adjustSentimentForCharacter(SentimentResponseDto originalResponse, String characterName) {
        if (!originalResponse.getSuccess()) {
            return originalResponse;
        }

        double originalScore = originalResponse.getSentimentScore();
        double adjustedScore = originalScore;

        // 캐릭터별 감정 점수 조정
        switch (characterName) {
            case "화끈이" -> {
                // 긍정 신호를 더 강하게, 부정 신호도 더 강하게 반응
                adjustedScore = originalScore * 1.3;
            }
            case "적극이" -> {
                // 긍정 신호에 더 민감하게 반응
                adjustedScore = originalScore > 0 ? originalScore * 1.2 : originalScore * 0.9;
            }
            case "균형이" -> {
                // 극단적인 신호를 완화
                adjustedScore = originalScore * 0.8;
            }
            case "조심이" -> {
                // 부정 신호에 더 민감하게, 긍정 신호는 보수적으로
                adjustedScore = originalScore < 0 ? originalScore * 1.2 : originalScore * 0.7;
            }
        }

        // 점수 범위 제한 (-1.0 ~ 1.0)
        adjustedScore = Math.max(-1.0, Math.min(1.0, adjustedScore));

        log.info("캐릭터별 감정 점수 조정: character={}, original={}, adjusted={}", 
                characterName, originalScore, adjustedScore);

        // 조정된 결과 반환 (Builder 패턴으로 새 객체 생성)
        return SentimentResponseDto.builder()
            .success(originalResponse.getSuccess())
            .sentimentScore(adjustedScore)
            .sentimentLabel(determineAggregatedLabel(adjustedScore))
            .confidence(originalResponse.getConfidence())
            .newsTitle(originalResponse.getNewsTitle())
            .newsSummary(originalResponse.getNewsSummary())
            .hwakkeunSignal(originalResponse.getHwakkeunSignal())
            .jeokgeukSignal(originalResponse.getJeokgeukSignal())
            .gyunhyungSignal(originalResponse.getGyunhyungSignal())
            .josimSignal(originalResponse.getJosimSignal())
            .analyzedAt(originalResponse.getAnalyzedAt())
            .processingTimeMs(originalResponse.getProcessingTimeMs())
            .build();
    }
}
