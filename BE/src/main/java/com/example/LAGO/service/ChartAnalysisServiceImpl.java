package com.example.LAGO.service;

import com.example.LAGO.constants.ChartMode;
import com.example.LAGO.constants.Interval;
import com.example.LAGO.domain.*;
import com.example.LAGO.dto.OhlcDataDto;
import com.example.LAGO.dto.response.ChartAnalysisResponse;
import com.example.LAGO.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class ChartAnalysisServiceImpl implements ChartAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ChartAnalysisServiceImpl.class);

    private final WebClient.Builder webClientBuilder;
    private final StockInfoRepository stockInfoRepository;
    private final TicksRepository ticksRepository;
    private final HistoryChallengeDataRepository challengeDataRepository;
    private final HistoryChallengeRepository historyChallengeRepository; // 추가

    @Value("${chart-analysis.url}")
    private String chartAnalysisUrl;

    @Override
    public List<ChartAnalysisResponse> analyzePatterns(String stockCode, ChartMode chartMode, Interval interval, LocalDateTime fromDateTime, LocalDateTime toDateTime) {

        // interval 문자열 매핑
        String intervalString = Interval.intervalToString(interval);

        // 주가데이터 (ohlc) 조회
        List<OhlcDataDto> ohlcData = switch (chartMode) {
            case MOCK -> {
                // 1. 종목 코드 확인
                stockInfoRepository.findByCode(stockCode)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid stock code (XXXXXX): " + stockCode));

                // 2. 모의투자 데이터를 조회 (stockCode와 날짜 범위로)
                List<Object[]> rawData = ticksRepository.findAggregatedByStockCodeAndDate(
                        stockCode, intervalString, fromDateTime, toDateTime
                );

                // 3. 조회된 Object[] 데이터를 OhlcDataDto 리스트로 매핑
                yield mapRawDataToOhlcDataDtoList(rawData);
            }
            case CHALLENGE -> {
                // 1. 현재 활성화된 역사 챌린지를 종목 코드로 조회
                HistoryChallenge activeChallenge = historyChallengeRepository.findActiveChallengeByStockCode(stockCode, LocalDateTime.now());

                if (activeChallenge == null) {
                    log.warn("활성화된 역사 챌린지를 찾을 수 없습니다. Stock Code: {}", stockCode);
                    yield Collections.emptyList(); // 활성화된 챌린지가 없으면 빈 리스트 반환
                }

                // 2. 역사 챌린지 데이터를 조회 (stockCode와 날짜 범위로)
                List<Object[]> rawData = challengeDataRepository.findAggregatedByStockCodeAndDateRangeAndInterval(
                        stockCode, intervalString, fromDateTime, toDateTime
                );

                // 3. 조회된 Object[] 데이터를 OhlcDataDto 리스트로 매핑
                yield mapRawDataToOhlcDataDtoList(rawData);
            }
        };

        log.info("{}개의 {} 데이터를 조회했습니다. (Stock Code: {}, 기간: {} ~ {})",
                ohlcData.size(), interval.getCode(), stockCode, fromDateTime, toDateTime);

        // No Content error - 주가 데이터 없음
        if (ohlcData.isEmpty()) {
            throw new RuntimeException("조회된 데이터가 없습니다.");
        }

        try {
            log.info("Python 분석 서버로 차트 패턴 분석을 요청합니다...");
            WebClient webClient = webClientBuilder.baseUrl(chartAnalysisUrl + "/detect-patterns").build();

            return webClient.post()
                    .bodyValue(ohlcData)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ChartAnalysisResponse>>() {})
                    .block(Duration.ofSeconds(30));
        } catch (WebClientResponseException e) {
            log.error("Python 서버로부터 에러 응답 수신: status={}, body={}", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Python 서버 분석 요청 실패: " + e.getResponseBodyAsString(), e);
        }
    }

    // Object[] 데이터를 OhlcDataDto 리스트로 매핑하는 헬퍼 메소드
    private static List<OhlcDataDto> mapRawDataToOhlcDataDtoList(List<Object[]> rawData) {
        return rawData.stream().map(row -> {
            LocalDateTime mappedDate;
            Object dateObject = row[0];

            if (dateObject instanceof Timestamp) {
                mappedDate = ((Timestamp) dateObject).toLocalDateTime();
            } else if (dateObject instanceof Instant) {
                mappedDate = ((Instant) dateObject).atZone(ZoneId.systemDefault()).toLocalDateTime();
            } else if (dateObject instanceof OffsetDateTime) {
                mappedDate = ((OffsetDateTime) dateObject).toLocalDateTime();
            } else if (dateObject instanceof LocalDateTime) {
                mappedDate = (LocalDateTime) dateObject;
            } else {
                throw new IllegalArgumentException("지원하지 않는 날짜 타입입니다: " + dateObject.getClass().getName());
            }

            return OhlcDataDto.builder()
                    .date(mappedDate)
                    .openPrice(((Number) row[1]).intValue())
                    .highPrice(((Number) row[2]).intValue())
                    .lowPrice(((Number) row[3]).intValue())
                    .closePrice(((Number) row[4]).intValue())
                    .volume(((Number) row[5]).longValue())
                    .build();
        }).collect(Collectors.toList());
    }
}