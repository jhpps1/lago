package com.example.LAGO.controller;

import com.example.LAGO.domain.Quiz;
import com.example.LAGO.dto.QuizDto;
import com.example.LAGO.service.DailyQuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/study/daily-quiz")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "차트학습/퀴즈", description = "투자 학습 및 퀴즈 관련 API")
public class DailyQuizController {

    private final DailyQuizService dailyQuizService;

    @GetMapping
    @Operation(summary = "오늘의 데일리 퀴즈 조회", description = "오늘 예정된 데일리 퀴즈를 조회합니다. 이미 풀었으면 알려줍니다.")
    public ResponseEntity<DailyQuizResponse> getTodayQuiz(@RequestParam Integer userId) {
        try {
            DailyQuizService.DailyQuizResult result = dailyQuizService.getTodayQuizForUser(userId);
            
            DailyQuizResponse response = DailyQuizResponse.builder()
                    .alreadySolved(result.alreadySolved)
                    .quiz(result.alreadySolved ? null : new QuizDto(result.quiz))
                    .solvedAt(result.solvedAt)
                    .score(result.score)
                    .ranking(result.ranking)
                    .build();
                    
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting today's quiz: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/solve")
    @Operation(summary = "데일리 퀴즈 정답 제출", description = "데일리 퀴즈 정답을 제출하고 결과를 받습니다.")
    public ResponseEntity<SolveResponse> solveDailyQuiz(@RequestBody SolveRequest request) {
        try {
            DailyQuizService.SolveResult result = dailyQuizService.solveDailyQuiz(
                    request.userId, 
                    request.quizId, 
                    request.userAnswer, 
                    request.solvedTimeSeconds
            );

            SolveResponse response = SolveResponse.builder()
                    .correct(result.correct)
                    .score(result.score)
                    .ranking(result.ranking)
                    .bonusAmount(result.bonusAmount)
                    .streak(result.streak)
                    .explanation(result.explanation)
                    .build();

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error solving daily quiz: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/streak")
    @Operation(summary = "사용자 스트릭 조회", description = "사용자의 현재 연속 도전 일수를 조회합니다.")
    public ResponseEntity<StreakResponse> getUserStreak(@RequestParam Integer userId) {
        try {
            int streak = dailyQuizService.getCurrentStreak(userId);
            
            StreakResponse response = new StreakResponse();
            response.userId = userId;
            response.streak = streak;
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error getting user streak: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Schema(description = "스트릭 조회 응답")
    public static class StreakResponse {
        @Schema(description = "사용자 ID", example = "1")
        public Integer userId;
        
        @Schema(description = "연속 도전 일수", example = "5")
        public int streak;
    }

    @Schema(description = "퀴즈 풀이 요청")
    public static class SolveRequest {
        @Schema(description = "사용자 ID", example = "1")
        public Integer userId;
        
        @Schema(description = "퀴즈 ID", example = "1")
        public Integer quizId;
        
        @Schema(description = "사용자 답변", example = "true")
        public Boolean userAnswer;
        
        @Schema(description = "풀이 소요 시간(초)", example = "45")
        public Integer solvedTimeSeconds;
    }

    @Schema(description = "데일리 퀴즈 조회 응답")
    public static class DailyQuizResponse {
        @Schema(description = "이미 풀었는지 여부", example = "false")
        public boolean alreadySolved;
        
        @Schema(description = "퀴즈 정보 (안 풀었을 때만)")
        public QuizDto quiz;
        
        @Schema(description = "풀이 날짜 (이미 풀었을 때)")
        public String solvedAt;
        
        @Schema(description = "점수 (이미 풀었을 때)")
        public Integer score;
        
        @Schema(description = "순위 (이미 풀었을 때)")
        public Integer ranking;

        public static DailyQuizResponseBuilder builder() {
            return new DailyQuizResponseBuilder();
        }

        public static class DailyQuizResponseBuilder {
            private boolean alreadySolved;
            private QuizDto quiz;
            private String solvedAt;
            private Integer score;
            private Integer ranking;

            public DailyQuizResponseBuilder alreadySolved(boolean alreadySolved) {
                this.alreadySolved = alreadySolved;
                return this;
            }

            public DailyQuizResponseBuilder quiz(QuizDto quiz) {
                this.quiz = quiz;
                return this;
            }

            public DailyQuizResponseBuilder solvedAt(String solvedAt) {
                this.solvedAt = solvedAt;
                return this;
            }

            public DailyQuizResponseBuilder score(Integer score) {
                this.score = score;
                return this;
            }

            public DailyQuizResponseBuilder ranking(Integer ranking) {
                this.ranking = ranking;
                return this;
            }

            public DailyQuizResponse build() {
                DailyQuizResponse response = new DailyQuizResponse();
                response.alreadySolved = this.alreadySolved;
                response.quiz = this.quiz;
                response.solvedAt = this.solvedAt;
                response.score = this.score;
                response.ranking = this.ranking;
                return response;
            }
        }
    }

    @Schema(description = "퀴즈 풀이 결과")
    public static class SolveResponse {
        @Schema(description = "정답 여부", example = "true")
        public boolean correct;
        
        @Schema(description = "획득 점수", example = "100")
        public int score;
        
        @Schema(description = "순위", example = "1")
        public int ranking;
        
        @Schema(description = "보너스 투자금", example = "100000")
        public int bonusAmount;
        
        @Schema(description = "연속 도전 일수", example = "5")
        public int streak;
        
        @Schema(description = "해설", example = "PER이 높다는 것은...")
        public String explanation;

        public static SolveResponseBuilder builder() {
            return new SolveResponseBuilder();
        }

        public static class SolveResponseBuilder {
            private boolean correct;
            private int score;
            private int ranking;
            private int bonusAmount;
            private int streak;
            private String explanation;

            public SolveResponseBuilder correct(boolean correct) {
                this.correct = correct;
                return this;
            }

            public SolveResponseBuilder score(int score) {
                this.score = score;
                return this;
            }

            public SolveResponseBuilder ranking(int ranking) {
                this.ranking = ranking;
                return this;
            }

            public SolveResponseBuilder bonusAmount(int bonusAmount) {
                this.bonusAmount = bonusAmount;
                return this;
            }

            public SolveResponseBuilder streak(int streak) {
                this.streak = streak;
                return this;
            }

            public SolveResponseBuilder explanation(String explanation) {
                this.explanation = explanation;
                return this;
            }

            public SolveResponse build() {
                SolveResponse response = new SolveResponse();
                response.correct = this.correct;
                response.score = this.score;
                response.ranking = this.ranking;
                response.bonusAmount = this.bonusAmount;
                response.streak = this.streak;
                response.explanation = this.explanation;
                return response;
            }
        }
    }
}