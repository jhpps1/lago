package com.example.LAGO.scheduler;

import com.example.LAGO.domain.DailyQuizSchedule;
import com.example.LAGO.repository.DailyQuizScheduleRepository;
import com.example.LAGO.repository.QuizRepository;
import com.example.LAGO.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyQuizScheduler {

    private final DailyQuizScheduleRepository dailyQuizScheduleRepository;
    private final QuizRepository quizRepository;
    private final PushNotificationService pushNotificationService;
    private final Random random = new Random();

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void scheduleDailyQuiz() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        if (dailyQuizScheduleRepository.existsByQuizDate(tomorrow)) {
            log.info("Daily quiz already scheduled for {}", tomorrow);
            return;
        }

        List<com.example.LAGO.domain.Quiz> availableQuizzes = quizRepository.findAvailableForDailyQuiz();
        if (availableQuizzes.isEmpty()) {
            log.error("No unused quizzes available for daily scheduling");
            return;
        }

        com.example.LAGO.domain.Quiz selectedQuiz = availableQuizzes.get(random.nextInt(availableQuizzes.size()));
        
        // 선택된 퀴즈의 daily_date를 현재 시간으로 설정 (사용됨 표시)
        selectedQuiz.setDailyDate(LocalDateTime.now());
        quizRepository.save(selectedQuiz);
        
        LocalTime randomTime = generateRandomTime();
        LocalDateTime startTime = tomorrow.atTime(randomTime);

        DailyQuizSchedule schedule = DailyQuizSchedule.builder()
                .scheduleId(generateScheduleId())
                .quizDate(tomorrow)
                .quizId(selectedQuiz.getQuizId())
                .startTime(startTime)
                .build();

        dailyQuizScheduleRepository.save(schedule);
        log.info("Scheduled daily quiz {} for {} at {}", selectedQuiz.getQuizId(), tomorrow, randomTime);
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkAndSendDailyQuizNotification() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        DailyQuizSchedule schedule = dailyQuizScheduleRepository.findByQuizDate(today)
                .orElse(null);
        
        if (schedule == null) {
            return;
        }
        
        // 정확히 스케줄된 start_time에 알림 발송 (1분 단위로 체크)
        LocalDateTime scheduledStartTime = schedule.getStartTime();
        
        if (now.getYear() == scheduledStartTime.getYear() &&
            now.getMonth() == scheduledStartTime.getMonth() &&
            now.getDayOfMonth() == scheduledStartTime.getDayOfMonth() &&
            now.getHour() == scheduledStartTime.getHour() &&
            now.getMinute() == scheduledStartTime.getMinute()) {
            
            com.example.LAGO.domain.Quiz quiz = quizRepository.findById(schedule.getQuizId())
                    .orElse(null);
                    
            if (quiz != null) {
                String notificationTitle = quiz.getQuestion();
                if (notificationTitle.length() > 50) {
                    notificationTitle = notificationTitle.substring(0, 50) + "...";
                }
                
                pushNotificationService.sendDailyQuizNotificationToAll(notificationTitle);
                log.info("🔔 Sent daily quiz notification at scheduled time {} for quiz {}", 
                    scheduledStartTime, quiz.getQuizId());
            }
        }
    }

    private LocalTime generateRandomTime() {
        int hour = 16 + random.nextInt(7);
        int minute = random.nextInt(60);
        return LocalTime.of(hour, minute);
    }

    private Integer generateScheduleId() {
        return (int) System.currentTimeMillis();
    }
}