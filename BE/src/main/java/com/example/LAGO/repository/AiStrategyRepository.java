package com.example.LAGO.repository;

import com.example.LAGO.domain.AiStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 전략 리포지토리
 * 
 * EC2 연동 DB AI_STRATEGY 테이블 관련 데이터베이스 접근 처리
 * 
 * 실제 테이블 구조:
 * - strategy_id (int PK)
 * - user_id (int FK)
 * - strategy (varchar50) - 캐릭터명 (화끈이/적극이/균형이/조심이)
 * - prompt (text) - 전략 설명/판단 근거
 * - created_at (datetime)
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Repository
public interface AiStrategyRepository extends JpaRepository<AiStrategy, Long> {

    /**
     * 사용자별 AI 전략 조회 (최신순)
     * 
     * @param userId 사용자 ID
     * @return 사용자의 AI 전략 목록
     */
    List<AiStrategy> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 사용자별 특정 전략 조회 (최신순)
     * 
     * @param userId 사용자 ID
     * @param strategy 전략명 (화끈이/적극이/균형이/조심이)
     * @return 해당 전략 목록
     */
    List<AiStrategy> findByUserIdAndStrategyOrderByCreatedAtDesc(Long userId, String strategy);
    
    /**
     * 사용자별 특정 전략 단일 조회 (최신)
     * 
     * @param userId 사용자 ID
     * @param strategy 전략명
     * @return 해당 전략 (최신)
     */
    Optional<AiStrategy> findByUserIdAndStrategy(Long userId, String strategy);

    /**
     * 사용자의 최근 전략 조회
     * 
     * @param userId 사용자 ID
     * @return 최근 전략
     */
    Optional<AiStrategy> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 기간 내 사용자 전략 조회
     * 
     * @param userId 사용자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간의 전략 목록
     */
    @Query("SELECT a FROM AiStrategy a WHERE a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AiStrategy> findByUserIdAndCreatedAtBetween(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 전략별 사용 횟수 조회
     * 
     * @param strategy 전략명 (화끈이/적극이/균형이/조심이)
     * @return 사용 횟수
     */
    long countByStrategy(String strategy);

    /**
     * 사용자별 전략 사용 횟수 조회
     * 
     * @param userId 사용자 ID
     * @param strategy 전략명
     * @return 사용자의 해당 전략 사용 횟수
     */
    long countByUserIdAndStrategy(Long userId, String strategy);

    /**
     * 오늘 생성된 사용자 전략 조회
     * 
     * @param userId 사용자 ID
     * @return 오늘 생성된 전략 목록
     */
//    @Query("SELECT a FROM AiStrategy a WHERE a.userId = :userId AND DATE(a.createdAt) = CURRENT_DATE ORDER BY a.createdAt DESC")
//    List<AiStrategy> findTodayStrategiesByUserId(@Param("userId") Long userId);

    /**
     * 최근 N일간 인기 전략 조회
     * 
     * @param days 조회할 일수
     * @return 인기 전략 목록 (사용 횟수 기준)
     */
    @Query(value = "SELECT strategy, COUNT(*) as usage_count FROM ai_strategy " +
           "WHERE created_at >= CURRENT_DATE - INTERVAL ':days days' " +
           "GROUP BY strategy ORDER BY usage_count DESC", nativeQuery = true)
    List<Object[]> findPopularStrategiesInLastDays(@Param("days") int days);
}
