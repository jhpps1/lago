package com.example.LAGO.service;

import com.example.LAGO.dto.StockChartDto;
import com.example.LAGO.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockChartService {
    
    private final Ticks1mRepository ticks1mRepository;
    private final Ticks3mRepository ticks3mRepository;
    private final Ticks5mRepository ticks5mRepository;
    private final Ticks10mRepository ticks10mRepository;
    private final Ticks15mRepository ticks15mRepository;
    private final Ticks30mRepository ticks30mRepository;
    private final Ticks60mRepository ticks60mRepository;
    private final Ticks1dRepository ticks1dRepository;
    private final Ticks1wRepository ticks1wRepository;
    private final Ticks1monRepository ticks1monRepository;
    private final Ticks1yRepository ticks1yRepository;
    
    /**
     * 특정 종목의 기간별 차트 데이터 조회 (KST 입력 -> UTC 조회 -> KST 응답)
     * 
     * @param code 종목 코드 (예: "005930")
     * @param interval 시간 간격 ("1m", "3m", "5m", "10m", "15m", "30m", "60m")
     * @param startDateKst 시작 시간 (KST 기준)
     * @param endDateKst 종료 시간 (KST 기준)
     * @return 차트 데이터 리스트 (KST 시간으로 변환)
     */
    public List<StockChartDto> getChartDataByCodeAndInterval(String code, String interval, 
                                                            LocalDateTime startDateKst, LocalDateTime endDateKst) {
        try {
            log.info("차트 데이터 조회 시작: code={}, interval={}, start={}, end={}", 
                    code, interval, startDateKst, endDateKst);
            
            // KST -> UTC 변환
            ZoneId kstZone = ZoneId.of("Asia/Seoul");
            ZoneId utcZone = ZoneId.of("UTC");
            
            OffsetDateTime startTimeUtc = startDateKst.atZone(kstZone)
                    .withZoneSameInstant(utcZone)
                    .toOffsetDateTime();
            OffsetDateTime endTimeUtc = endDateKst.atZone(kstZone)
                    .withZoneSameInstant(utcZone)
                    .toOffsetDateTime();
            
            log.debug("시간 변환 완료: startUtc={}, endUtc={}", startTimeUtc, endTimeUtc);
            
            // interval별 Repository 분기
            List<StockChartDto> chartData = switch (interval) {
                case "1m" -> ticks1mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "3m" -> ticks3mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "5m" -> ticks5mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "10m" -> ticks10mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "15m" -> ticks15mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "30m" -> ticks30mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "60m" -> ticks60mRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1D" -> ticks1dRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1W" -> ticks1wRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1M" -> ticks1monRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1Y" -> ticks1yRepository.findByCodeAndBucketRange(code, startTimeUtc, endTimeUtc)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                default -> throw new IllegalArgumentException("지원하지 않는 시간 간격: " + interval);
            };
            
            log.info("차트 데이터 조회 완료: code={}, interval={}, 조회된 건수={}", 
                    code, interval, chartData.size());
            
            return chartData;
                    
        } catch (Exception e) {
            log.error("차트 데이터 조회 실패: code={}, interval={}, start={}, end={}", 
                     code, interval, startDateKst, endDateKst, e);
            return List.of();
        }
    }
    
    /**
     * 특정 종목의 최신 차트 데이터 조회 (최대 100개)
     * 
     * @param code 종목 코드
     * @param interval 시간 간격
     * @return 최신 차트 데이터 (KST 시간으로 변환)
     */
    public List<StockChartDto> getLatestChartDataByCode(String code, String interval) {
        try {
            log.info("최신 차트 데이터 조회: code={}, interval={}", code, interval);
            
            Pageable pageable = Pageable.ofSize(100);
            
            // interval별 Repository 분기
            List<StockChartDto> chartData = switch (interval) {
                case "1m" -> ticks1mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "3m" -> ticks3mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "5m" -> ticks5mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "10m" -> ticks10mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "15m" -> ticks15mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "30m" -> ticks30mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "60m" -> ticks60mRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1D" -> ticks1dRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1W" -> ticks1wRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1M" -> ticks1monRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                case "1Y" -> ticks1yRepository.findLatestByCode(code, pageable)
                        .stream().map(tick -> convertToDto(tick, interval)).collect(Collectors.toList());
                default -> throw new IllegalArgumentException("지원하지 않는 시간 간격: " + interval);
            };
            
            log.info("최신 차트 데이터 조회 완료: code={}, interval={}, 조회된 건수={}", 
                    code, interval, chartData.size());
            
            return chartData;
                    
        } catch (Exception e) {
            log.error("최신 차트 데이터 조회 실패: code={}, interval={}", code, interval, e);
            return List.of();
        }
    }
    
    /**
     * 지원하는 시간 간격 목록 반환
     * 
     * @return 지원하는 시간 간격 리스트
     */
    public List<String> getSupportedIntervals() {
        return List.of("1m", "3m", "5m", "10m", "15m", "30m", "60m", "1D", "1W", "1M", "1Y");
    }
    
    /**
     * 시간 간격 유효성 검증
     * 
     * @param interval 시간 간격
     * @return 유효 여부
     */
    public boolean isValidInterval(String interval) {
        return getSupportedIntervals().contains(interval);
    }
    
    /**
     * 엔티티를 StockChartDto로 변환 (타입 안전한 제네릭 메서드)
     * 
     * @param tick 엔티티 객체
     * @param interval 시간 간격
     * @return StockChartDto
     */
    private StockChartDto convertToDto(Object tick, String interval) {
        try {
            log.debug("엔티티 변환 시작: tick 클래스={}, interval={}", tick.getClass().getSimpleName(), interval);
            
            // 공통 인터페이스가 없으므로 리플렉션 사용
            var getBucketMethod = tick.getClass().getMethod("getBucket");
            var getStockInfoIdMethod = tick.getClass().getMethod("getStockInfoId");
            var getOpenPriceMethod = tick.getClass().getMethod("getOpenPrice");
            var getHighPriceMethod = tick.getClass().getMethod("getHighPrice");
            var getLowPriceMethod = tick.getClass().getMethod("getLowPrice");
            var getClosePriceMethod = tick.getClass().getMethod("getClosePrice");
            var getVolumeMethod = tick.getClass().getMethod("getVolume");
            var getStockInfoMethod = tick.getClass().getMethod("getStockInfo");
            
            // UTC -> KST 변환
            OffsetDateTime bucketUtc = (OffsetDateTime) getBucketMethod.invoke(tick);
            LocalDateTime bucketKst = bucketUtc.atZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
            
            // StockInfo에서 code 추출
            Object stockInfo = getStockInfoMethod.invoke(tick);
            String code = null;
            if (stockInfo != null) {
                var getCodeMethod = stockInfo.getClass().getMethod("getCode");
                code = (String) getCodeMethod.invoke(stockInfo);
            }
            
            return StockChartDto.builder()
                    .stockInfoId((Integer) getStockInfoIdMethod.invoke(tick))
                    .bucket(bucketKst)
                    .code(code)
                    .interval(interval)
                    .openPrice((Integer) getOpenPriceMethod.invoke(tick))
                    .highPrice((Integer) getHighPriceMethod.invoke(tick))
                    .lowPrice((Integer) getLowPriceMethod.invoke(tick))
                    .closePrice((Integer) getClosePriceMethod.invoke(tick))
                    .volume((Long) getVolumeMethod.invoke(tick))
                    .build();
                    
        } catch (Exception e) {
            log.error("엔티티 변환 실패: tick={}, interval={}", tick, interval, e);
            throw new RuntimeException("엔티티 변환 중 오류 발생", e);
        }
    }
}