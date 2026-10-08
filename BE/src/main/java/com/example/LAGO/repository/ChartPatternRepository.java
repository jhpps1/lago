package com.example.LAGO.repository;

import com.example.LAGO.domain.ChartPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 차트 패턴 레포지토리
 * 지침서 명세: DB-Entity-Repository 일치 검증 필수
 * 연동된 EC2 DB CHART_PATTERN 테이블 조회
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-09
 */
@Repository
public interface ChartPatternRepository extends JpaRepository<ChartPattern, Integer> {

    /**
     * 모든 차트 패턴 조회
     * pattern_id 기준 정렬
     */
    @Query("SELECT cp FROM ChartPattern cp ORDER BY cp.patternId ASC")
    List<ChartPattern> findAllOrderByPatternId();
}