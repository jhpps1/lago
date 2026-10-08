package com.example.LAGO.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
public class PushNotificationService {

    private static final String DAILY_QUIZ_TOPIC = "daily_quiz";

    /**
     * 전체 사용자에게 데일리 퀴즈 알림 발송
     *
     * @param quizTitle 퀴즈 제목 (문제 일부)
     */
    public void sendDailyQuizNotificationToAll(String quizTitle) {
        try {
            Message message = Message.builder()
                    .setTopic(DAILY_QUIZ_TOPIC)
                    .putData("type", "daily_quiz")
                    .putData("title", "📚 새로운 데일리 퀴즈!")
                    .putData("body", quizTitle)
                    .putData("quiz_date", LocalDate.now().toString())
                    .putData("action", "open_daily_quiz")
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent daily quiz notification: {} - Response: {}", quizTitle, response);
            
        } catch (Exception e) {
            log.error("Failed to send daily quiz notification: {}", quizTitle, e);
        }
    }

    /**
     * 테스트용 푸시 알림 발송
     *
     * @param title 제목
     * @param body 내용
     */
    public void sendTestNotification(String title, String body) {
        try {
            Message message = Message.builder()
                    .setTopic(DAILY_QUIZ_TOPIC)
                    .putData("type", "test")
                    .putData("title", title)
                    .putData("body", body)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent test notification: {} - Response: {}", title, response);
            
        } catch (Exception e) {
            log.error("Failed to send test notification: {} - {}", title, body, e);
        }
    }
}