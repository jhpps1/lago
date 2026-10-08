package com.example.LAGO.repository;

import com.example.LAGO.domain.DailySolved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySolvedRepository extends JpaRepository<DailySolved, Integer> {
    
    Optional<DailySolved> findByUserIdAndQuizIdAndSolvedAt(Integer userId, Integer quizId, LocalDate solvedAt);
    
    boolean existsByUserIdAndQuizIdAndSolvedAt(Integer userId, Integer quizId, LocalDate solvedAt);
    
    @Query("SELECT COUNT(*) FROM DailySolved ds WHERE ds.quizId = :quizId AND ds.solvedAt = :solvedAt AND ds.solvedTimeSeconds < :solvedTimeSeconds")
    Long countFasterSolvers(@Param("quizId") Integer quizId, @Param("solvedAt") LocalDate solvedAt, @Param("solvedTimeSeconds") Integer solvedTimeSeconds);
    
    List<DailySolved> findByQuizIdAndSolvedAtOrderBySolvedTimeSecondsAsc(Integer quizId, LocalDate solvedAt);
    
    @Query("SELECT ds FROM DailySolved ds WHERE ds.userId = :userId ORDER BY ds.solvedAt DESC")
    List<DailySolved> findByUserIdOrderBySolvedAtDesc(@Param("userId") Integer userId);
    
    @Query("SELECT ds FROM DailySolved ds WHERE ds.userId = :userId ORDER BY ds.solvedAt DESC LIMIT 1")
    Optional<DailySolved> findLatestByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT ds FROM DailySolved ds WHERE ds.userId = :userId AND ds.solvedAt BETWEEN :startDate AND :endDate ORDER BY ds.solvedAt DESC")
    List<DailySolved> findByUserIdAndSolvedAtBetween(@Param("userId") Integer userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}