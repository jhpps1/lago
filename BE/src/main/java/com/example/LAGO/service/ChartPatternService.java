package com.example.LAGO.service;

import com.example.LAGO.domain.ChartPattern;
import com.example.LAGO.dto.response.ChartPatternResponse;
import com.example.LAGO.repository.ChartPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 차트 패턴 서비스
 * 
 * 지침서 명세: Validation/Exception 모든 입력/출력/관계/필수값/에러 꼼꼼히 처리
 * 연동된 EC2 DB CHART_PATTERN 테이블 비즈니스 로직 처리
 * 
 * @author D203팀 백엔드 개발자  
 * @since 2025-08-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChartPatternService {

    private final ChartPatternRepository chartPatternRepository;

    /**
     * 전체 차트 패턴 목록 조회
     * 차트 학습 화면에서 전체 차트 패턴 목록을 조회합니다.
     * 
     * @return 차트 패턴 응답 리스트
     * @throws RuntimeException 데이터베이스 조회 실패 시
     */
    public List<ChartPatternResponse> getAllChartPatterns() {
        try {
            log.info("차트 패턴 전체 목록 조회 시작");
            
            List<ChartPattern> chartPatterns = chartPatternRepository.findAllOrderByPatternId();
            
            if (chartPatterns == null || chartPatterns.isEmpty()) {
                log.warn("조회된 차트 패턴이 없습니다.");
                return List.of();
            }
            
            List<ChartPatternResponse> responses = chartPatterns.stream()
                    .map(ChartPatternResponse::from)
                    .collect(Collectors.toList());
            
            log.info("차트 패턴 전체 목록 조회 완료. 조회된 패턴 수: {}", responses.size());
            return responses;
            
        } catch (Exception e) {
            log.error("차트 패턴 목록 조회 중 오류 발생", e);
            throw new RuntimeException("차트 패턴 목록 조회에 실패했습니다.", e);
        }
    }
}