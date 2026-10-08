package com.example.LAGO.scheduler;

import com.example.LAGO.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.news.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NewsScheduler {
    
    private final NewsService newsService;
    
    @Scheduled(fixedRate = 1200000) // 20분마다 실행 (1200000ms = 20분)
    public void collectGoogleRssNews() {
        log.info("=== Google RSS 뉴스 수집 스케줄러 시작 - {} ===", LocalDateTime.now());
        
        try {
            // FinBERT 서비스를 통한 실시간 뉴스 수집
            newsService.collectRealtimeNews();
            
            // 잠시 대기 
            Thread.sleep(3000);
            
            // 주요 관심종목 뉴스 수집 (비활성화)
            // newsService.collectWatchlistNewsFromDB();
            
            log.info("=== Google RSS 뉴스 수집 스케줄러 완료 - {} ===", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Google RSS 뉴스 수집 스케줄러 실행 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    // 매일 자정에 오래된 뉴스 정리 (선택사항)
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldNews() {
        log.info("=== 오래된 뉴스 정리 시작 - {} ===", LocalDateTime.now());
        
        try {
            // NewsService에 구현된 정리 메서드 호출
            int deletedCount = newsService.cleanupOldNews();
            log.info("=== 오래된 뉴스 {}개 정리 완료 - {} ===", deletedCount, LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("오래된 뉴스 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}