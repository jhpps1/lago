package com.example.LAGO.controller;

import com.example.LAGO.domain.*;
import com.example.LAGO.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 테스트 데이터 생성 컨트롤러
 * 자동매매봇 테스트를 위한 더미 데이터 생성
 */
@RestController
@RequestMapping("/api/test/data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "테스트 데이터", description = "테스트용 더미 데이터 생성 API")
public class TestDataController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    /**
     * 테스트용 AI 봇 생성
     */
    @PostMapping("/create-ai-bot")
    @Operation(summary = "테스트용 AI 봇 생성", description = "자동매매 테스트를 위한 AI 봇을 생성합니다.")
    public ResponseEntity<Map<String, Object>> createTestAiBot(
            @RequestParam(defaultValue = "테스트봇") String nickname,
            @RequestParam(defaultValue = "1000000") Integer initialBalance) {
        
        try {
            // 1. AI 봇 유저 생성
            User aiBot = User.builder()
                    .email("testbot@test.com")
                    .nickname(nickname)
                    .isAi(true)  // AI 봇으로 설정
                    .personality("균형이")  // 기본 전략
                    .createdAt(LocalDateTime.now())
                    .build();
            
            User savedBot = userRepository.save(aiBot);
            log.info("AI 봇 생성: userId={}, nickname={}", savedBot.getUserId(), savedBot.getNickname());

            // 2. AI 봇 계좌 생성  
            Account botAccount = Account.builder()
                    .userId(savedBot.getUserId())
                    .type(Account.TYPE_AI_BOT)
                    .balance(initialBalance)
                    .totalAsset(initialBalance)
                    .profit(0)
                    .profitRate(0.0)
                    .build();
            
            Account savedAccount = accountRepository.save(botAccount);
            log.info("AI 봇 계좌 생성: accountId={}, balance={}", savedAccount.getAccountId(), savedAccount.getBalance());

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "botUserId", savedBot.getUserId(),
                "accountId", savedAccount.getAccountId(), 
                "nickname", savedBot.getNickname(),
                "balance", savedAccount.getBalance(),
                "message", "AI 봇이 성공적으로 생성되었습니다."
            ));

        } catch (Exception e) {
            log.error("AI 봇 생성 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "AI 봇 생성 중 오류: " + e.getMessage()
            ));
        }
    }

    /**
     * 기존 AI 봇 목록 조회
     */
    @GetMapping("/ai-bots")
    @Operation(summary = "AI 봇 목록 조회", description = "현재 등록된 AI 봇들을 조회합니다.")
    public ResponseEntity<?> getAiBots() {
        try {
            var aiBots = userRepository.findByIsAiTrueOrderByAiIdAsc();
            return ResponseEntity.ok(Map.of(
                "aiBots", aiBots,
                "count", aiBots.size()
            ));
        } catch (Exception e) {
            log.error("AI 봇 조회 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "AI 봇 조회 중 오류: " + e.getMessage()
            ));
        }
    }

    /**
     * AI 봇들에게 ai_bot 타입 계좌 생성
     */
    @PostMapping("/create-ai-accounts")
    @Operation(summary = "AI 봇 계좌 생성", description = "모든 AI 봇에게 ai_bot 타입의 계좌를 생성합니다.")
    public ResponseEntity<Map<String, Object>> createAiAccounts(
            @RequestParam(defaultValue = "1000000") Integer initialBalance) {
        
        try {
            var aiBots = userRepository.findByIsAiTrueOrderByAiIdAsc();
            int createdCount = 0;
            int skippedCount = 0;
            
            for (var aiBot : aiBots) {
                // 이미 ai_bot 계좌가 있는지 확인
                var existingAccount = accountRepository.findByUserIdAndType(aiBot.getUserId(), Account.TYPE_AI_BOT);
                
                if (existingAccount.isEmpty()) {
                    // ai_bot 계좌 생성 (ID는 자동 생성)
                    Account botAccount = Account.builder()
                            .userId(aiBot.getUserId())
                            .type(Account.TYPE_AI_BOT)
                            .balance(initialBalance)
                            .totalAsset(initialBalance)
                            .profit(0)
                            .profitRate(0.0)
                            .build();
                    
                    Account savedAccount = accountRepository.save(botAccount);
                    log.info("AI 봇 {} (userId={})에게 계좌 생성: accountId={}, balance={}", 
                            aiBot.getNickname(), aiBot.getUserId(), savedAccount.getAccountId(), savedAccount.getBalance());
                    createdCount++;
                } else {
                    log.info("AI 봇 {} (userId={})는 이미 ai_bot 계좌가 있음: accountId={}", 
                            aiBot.getNickname(), aiBot.getUserId(), existingAccount.get().getAccountId());
                    skippedCount++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "AI 봇 계좌 생성 완료",
                "totalBots", aiBots.size(),
                "createdAccounts", createdCount,
                "skippedAccounts", skippedCount,
                "initialBalance", initialBalance
            ));

        } catch (Exception e) {
            log.error("AI 봇 계좌 생성 중 오류", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "AI 봇 계좌 생성 중 오류: " + e.getMessage()
            ));
        }
    }
}