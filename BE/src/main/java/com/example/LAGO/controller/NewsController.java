package com.example.LAGO.controller;

import com.example.LAGO.dto.response.NewsResponse;
import com.example.LAGO.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "뉴스 API", description = "Google RSS 기반 뉴스 수집 및 조회 API")
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    @Operation(
            summary = "전체 뉴스 목록 조회",
            description = "수집된 모든 뉴스를 최신순으로 조회합니다."
    )
    public ResponseEntity<Page<NewsResponse>> getNewsList(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NewsResponse> newsPage = newsService.getNewsList(pageable);

            log.info("뉴스 목록 조회 - 페이지: {}, 크기: {}, 총 {}개", page, size, newsPage.getTotalElements());

            return ResponseEntity.ok(newsPage);

        } catch (Exception e) {
            log.error("뉴스 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/stock/{stockCode}")
    @Operation(
            summary = "종목별 뉴스 조회",
            description = "특정 종목의 뉴스만 조회합니다."
    )
    public ResponseEntity<Page<NewsResponse>> getNewsByStock(
            @Parameter(description = "종목 코드", example = "005930")
            @PathVariable String stockCode,

            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NewsResponse> newsPage = newsService.getNewsByStock(stockCode, pageable);

            log.info("종목별 뉴스 조회 - 종목: {}, 총 {}개", stockCode, newsPage.getTotalElements());

            return ResponseEntity.ok(newsPage);

        } catch (Exception e) {
            log.error("종목별 뉴스 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/watchlist")
    @Operation(
            summary = "관심종목 뉴스 조회",
            description = "사용자의 관심종목 뉴스만 조회합니다."
    )
    public ResponseEntity<Page<NewsResponse>> getWatchlistNews(
            @Parameter(description = "사용자 ID", example = "1")
            @RequestParam Long userId,

            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NewsResponse> newsPage = newsService.getWatchlistNews(userId, pageable);

            log.info("관심종목 뉴스 조회 - 사용자: {}, 총 {}개", userId, newsPage.getTotalElements());

            return ResponseEntity.ok(newsPage);

        } catch (Exception e) {
            log.error("관심종목 뉴스 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "뉴스 상세 조회",
            description = "특정 뉴스의 상세 정보를 조회합니다."
    )
    public ResponseEntity<NewsResponse> getNewsById(
            @Parameter(description = "뉴스 ID", example = "1")
            @PathVariable Long id
    ) {
        try {
            NewsResponse news = newsService.getNewsById(id);
            log.info("뉴스 상세 조회 - ID: {}, 제목: {}", id, news.getTitle());

            return ResponseEntity.ok(news);

        } catch (RuntimeException e) {
            log.error("뉴스 상세 조회 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("뉴스 상세 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/collect/realtime")
    @Operation(
            summary = "실시간 뉴스 수동 수집",
            description = "실시간 경제/투자 뉴스를 즉시 수집합니다. (자동: 20분마다)"
    )
    public ResponseEntity<Map<String, Object>> collectRealtimeNews() {
        try {
            newsService.collectRealtimeNews();

            return ResponseEntity.ok(Map.of(
                    "message", "실시간 뉴스 수집을 시작했습니다.",
                    "type", "REALTIME",
                    "status", "STARTED"
            ));

        } catch (Exception e) {
            log.error("실시간 뉴스 수집 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "실시간 뉴스 수집에 실패했습니다",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/collect/watchlist")
    @Operation(
            summary = "관심종목 뉴스 수동 수집",
            description = "DB의 관심종목 뉴스를 즉시 수집합니다. (자동: 20분마다)"
    )
    public ResponseEntity<Map<String, Object>> collectWatchlistNews() {
        try {
            newsService.collectWatchlistNewsFromDB();

            return ResponseEntity.ok(Map.of(
                    "message", "관심종목 뉴스 수집을 시작했습니다.",
                    "type", "WATCHLIST",
                    "status", "STARTED"
            ));

        } catch (Exception e) {
            log.error("관심종목 뉴스 수집 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "관심종목 뉴스 수집에 실패했습니다",
                            "message", e.getMessage()
                    ));
        }
    }


    @PostMapping("/collect/historical")
    @Operation(
            summary = "역사적 챌린지 뉴스 수집",
            description = "특정 날짜의 주요 종목 뉴스를 수집합니다."
    )
    public ResponseEntity<Map<String, Object>> collectHistoricalNews(
            @Parameter(description = "수집할 날짜 (YYYY-MM-DD)", example = "2025-08-07")
            @RequestParam String date
    ) {
        try {
            // 백그라운드에서 실행
            new Thread(() -> {
                newsService.collectHistoricalNews(date);
            }).start();

            return ResponseEntity.ok(Map.of(
                    "message", "역사적 뉴스 수집을 시작했습니다.",
                    "date", date,
                    "type", "HISTORICAL",
                    "status", "STARTED"
            ));

        } catch (Exception e) {
            log.error("역사적 뉴스 수집 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "역사적 뉴스 수집에 실패했습니다",
                            "message", e.getMessage()
                    ));
        }
    }

    @GetMapping("/stats")
    @Operation(
            summary = "뉴스 감성 통계",
            description = "전체 또는 종목별 감성 분석 통계를 조회합니다."
    )
    public ResponseEntity<Map<String, Object>> getNewsSentimentStats(
            @Parameter(description = "종목 코드 (선택)", example = "005930")
            @RequestParam(required = false) String stockCode
    ) {
        try {
            Map<String, Object> stats = newsService.getNewsSentimentStats(stockCode);
            log.info("뉴스 감성 통계 조회 완료: {}", stats);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("뉴스 감성 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/collection/status")
    @Operation(
            summary = "뉴스 수집 상태 조회",
            description = "마지막 수집 시간과 다음 수집 예정 시간을 조회합니다."
    )
    public ResponseEntity<Map<String, Object>> getCollectionStatus() {
        try {
            Map<String, Object> status = newsService.getCollectionStatus();

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("수집 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/cleanup")
    @Operation(
            summary = "오래된 뉴스 정리",
            description = "30일 이상 된 뉴스를 삭제합니다. (자동: 매일 새벽 3시)"
    )
    public ResponseEntity<Map<String, Object>> cleanupOldNews() {
        try {
            int deletedCount = newsService.cleanupOldNews();

            return ResponseEntity.ok(Map.of(
                    "message", String.format("오래된 뉴스 %d개를 정리했습니다.", deletedCount),
                    "deletedCount", deletedCount
            ));

        } catch (Exception e) {
            log.error("뉴스 정리 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "뉴스 정리에 실패했습니다",
                            "message", e.getMessage()
                    ));
        }
    }
}