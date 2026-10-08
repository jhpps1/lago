package com.example.LAGO.repository;

import com.example.LAGO.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 퀴즈 Repository
 * QUIZ 테이블 데이터 접근 계층
 */
@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    
    /**
     * 특정 quiz_id가 아닌 다른 퀴즈들 조회 (랜덤 선택용)
     *
     * @param excludeQuizId 제외할 quiz_id
     * @return 해당 ID를 제외한 모든 퀴즈 리스트
     */
    @Query("SELECT q FROM Quiz q WHERE q.quizId != :excludeQuizId")
    List<Quiz> findAllExcludingQuizId(@Param("excludeQuizId") Integer excludeQuizId);

    /**
     * 모든 퀴즈 조회 (quiz_id 오름차순 정렬)
     *
     * @return 모든 퀴즈 리스트
     */
    @Query("SELECT q FROM Quiz q ORDER BY q.quizId ASC")
    List<Quiz> findAllOrderByQuizId();

    /**
     * 일일 퀴즈 조회 (특정 날짜)
     *
     * @param date 날짜
     * @return 해당 날짜의 일일 퀴즈
     */
    @Query("SELECT q FROM Quiz q WHERE DATE(q.dailyDate) = DATE(:date)")
    Optional<Quiz> findByDailyDate(@Param("date") LocalDateTime date);

    /**
     * 용어 ID로 관련 퀴즈 조회
     *
     * @param termId 투자 용어 ID
     * @return 해당 용어와 관련된 퀴즈들
     */
    List<Quiz> findByTermId(Integer termId);

    /**
     * 데일리 퀴즈로 사용되지 않은 퀴즈들 조회 (daily_date가 null인 것들)
     *
     * @return daily_date가 null인 퀴즈 리스트
     */
    @Query("SELECT q FROM Quiz q WHERE q.dailyDate IS NULL")
    List<Quiz> findAvailableForDailyQuiz();
}