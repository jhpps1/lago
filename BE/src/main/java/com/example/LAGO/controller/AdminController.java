package com.example.LAGO.controller;

import com.example.LAGO.domain.DailyQuizSchedule;
import com.example.LAGO.domain.Quiz;
import com.example.LAGO.repository.DailyQuizScheduleRepository;
import com.example.LAGO.repository.QuizRepository;
import com.example.LAGO.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 API")
public class AdminController {

    private final DailyQuizScheduleRepository dailyQuizScheduleRepository;
    private final QuizRepository quizRepository;
    private final PushNotificationService pushNotificationService;
    private final Random random = new Random();

    @PostMapping("/daily-quiz/schedule-today")
    @Operation(summary = "오늘 데일리 퀴즈 수동 스케줄링", description = "테스트용: 오늘 데일리 퀴즈를 수동으로 스케줄링합니다.")
    public ResponseEntity<String> scheduleToday(@RequestParam("quizId") Integer quizId) {
        LocalDate today = LocalDate.now();
        
        if (dailyQuizScheduleRepository.existsByQuizDate(today)) {
            return ResponseEntity.ok("Today's quiz already scheduled");
        }

        Optional<Quiz> selectedQuiz = quizRepository.findById(quizId);
        if (selectedQuiz.isEmpty()) {
            return ResponseEntity.badRequest().body("Quiz not found with ID: " + quizId);
        }
        
        Quiz quiz = selectedQuiz.get();
        
        DailyQuizSchedule schedule = DailyQuizSchedule.builder()
                .scheduleId((int) System.currentTimeMillis())
                .quizDate(today)
                .quizId(quiz.getQuizId())
                .startTime(LocalDateTime.now())
                .build();

        dailyQuizScheduleRepository.save(schedule);
        
        // 수동 생성시 즉시 푸시 알림 발송
        String notificationTitle = quiz.getQuestion();
        if (notificationTitle.length() > 50) {
            notificationTitle = notificationTitle.substring(0, 50) + "...";
        }
        pushNotificationService.sendDailyQuizNotificationToAll(notificationTitle);
        
        return ResponseEntity.ok("Today's quiz scheduled and notification sent: " + quiz.getQuestion());
    }

    @PostMapping("/test-push")
    @Operation(summary = "테스트 푸시 알림 발송", description = "테스트용: 푸시 알림을 즉시 발송합니다.")
    public ResponseEntity<String> testPush() {
        pushNotificationService.sendTestNotification("테스트 알림", "푸시 알림 테스트입니다!");
        return ResponseEntity.ok("Test push notification sent!");
    }
}