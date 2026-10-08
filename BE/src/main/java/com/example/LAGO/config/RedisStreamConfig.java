//package com.example.LAGO.config;
//
//import com.example.LAGO.realtime.RedisStreamConsumer;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.ContextClosedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@EnableScheduling
//public class RedisStreamConfig {
//
//    @Autowired
//    private RedisStreamConsumer redisStreamConsumer;
//
//    /**
//     * 애플리케이션이 완전히 시작된 후 Redis Stream Consumer 시작
//     */
//    @EventListener(ApplicationReadyEvent.class)
//    public void onApplicationReady() {
//        log.info("🚀 애플리케이션 준비 완료 - Redis Stream Consumer 시작");
//        try {
//            redisStreamConsumer.startConsumer();
//            log.info("✅ Redis Stream Consumer 시작 성공");
//        } catch (Exception e) {
//            log.error("❌ Redis Stream Consumer 시작 실패: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 애플리케이션 종료 시 Redis Stream Consumer 중지
//     */
//    @EventListener(ContextClosedEvent.class)
//    public void onContextClosed() {
//        log.info("🛑 애플리케이션 종료 중 - Redis Stream Consumer 중지");
//        try {
//            redisStreamConsumer.stopConsumer();
//            log.info("✅ Redis Stream Consumer 중지 완료");
//        } catch (Exception e) {
//            log.error("❌ Redis Stream Consumer 중지 실패: {}", e.getMessage(), e);
//        }
//    }
//}