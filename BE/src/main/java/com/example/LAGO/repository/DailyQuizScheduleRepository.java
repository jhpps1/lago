package com.example.LAGO.repository;

import com.example.LAGO.domain.DailyQuizSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyQuizScheduleRepository extends JpaRepository<DailyQuizSchedule, Integer> {
    
    Optional<DailyQuizSchedule> findByQuizDate(LocalDate quizDate);
    
    boolean existsByQuizDate(LocalDate quizDate);
}