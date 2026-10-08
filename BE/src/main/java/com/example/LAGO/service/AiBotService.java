package com.example.LAGO.service;

import com.example.LAGO.domain.User;
import com.example.LAGO.domain.Account;
import com.example.LAGO.dto.response.AiBotAccountResponse;
import com.example.LAGO.dto.response.AiBotListResponse;
import com.example.LAGO.repository.UserRepository;
import com.example.LAGO.repository.AccountRepository;
import com.example.LAGO.repository.MockTradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.LAGO.constants.TradingConstants.*;

/**
 * AI 매매봇 서비스
 * 지침서 명세: AI 봇은 user 테이블의 is_ai 컬럼으로 구분, AI 거래/전략은 별도 테이블
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBotService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final MockTradeRepository mockTradeRepository;

    /**
     * AI 매매봇 계좌 조회
     * 명세서 기준: is_ai=true인 사용자와 연결된 계좌 정보 조회
     * 
     * @param aiId AI 봇 식별자 (ai_id 컬럼값)
     * @return AI 봇 계좌 정보
     * @throws RuntimeException AI 봇 또는 계좌를 찾을 수 없는 경우
     */
    public AiBotAccountResponse getAiBotAccount(Integer aiId) {
        log.info("AI 매매봇 계좌 조회 시작: aiId={}", aiId);

        try {
            // 지침서 명세: ai_id + is_ai 기준으로 AI 유저 조회
            User aiUser = userRepository.findByAiIdAndIsAi(aiId, true)
                    .orElseThrow(() -> {
                        log.error("AI 봇 사용자를 찾을 수 없습니다. aiId={}", aiId);
                        return new RuntimeException("해당 aiId의 AI 봇 사용자를 찾을 수 없습니다.");
                    });

            // 지침서 명세: 해당 user_id 기준으로 봇 전용 계좌 조회
            Account aiAccount = accountRepository.findByUserIdAndType(aiUser.getUserId(), ACCOUNT_TYPE_MOCK_TRADING)
                    .orElseThrow(() -> {
                        log.error("AI 봇 계좌를 찾을 수 없습니다. userId={}", aiUser.getUserId());
                        return new RuntimeException("AI 봇 계좌를 찾을 수 없습니다.");
                    });

            // 거래 횟수 및 평균값 조회
            Long tradeCount = mockTradeRepository.countByAccountId(aiAccount.getAccountId());
            Double avgTradeValue = mockTradeRepository.findAvgTradeValue(aiAccount.getAccountId());

            // TODO: WARN - 추후 AI_STRATEGY 테이블에서 전략 정보 조회 필요
            String strategy = "기본 전략"; // 임시값

            // TODO: WARN - 마지막 거래일 조회 로직 추가 필요 (MOCK_TRADE 테이블에서)
            LocalDateTime lastTradeAt = null; // 임시값

            // 지침서 명세에 따른 응답 구성
            AiBotAccountResponse response = AiBotAccountResponse.builder()
                    .aiId(aiId)
                    .nickname(aiUser.getNickname())
                    .accountId(aiAccount.getAccountId())
                    .balance(aiAccount.getBalance())
                    .totalAsset(aiAccount.getTotalAsset())
                    .profit(aiAccount.getProfit())
                    .profitRate(aiAccount.getProfitRate())
                    .type(aiAccount.getType())
                    .tradeCount(tradeCount)
                    .avgTradeValue(avgTradeValue != null ? avgTradeValue : 0.0)
                    .lastTradeAt(lastTradeAt)
                    .strategy(strategy)
                    .responseTime(LocalDateTime.now())
                    .build();

            log.info("AI 매매봇 계좌 조회 완료: aiId={}, nickname={}, balance={}", 
                    aiId, aiUser.getNickname(), aiAccount.getBalance());

            return response;

        } catch (Exception e) {
            log.error("AI 매매봇 계좌 조회 중 오류 발생: aiId={}, error={}", aiId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * AI 매매봇 상태 조회 (기존 메서드 - 호환성 유지)
     * 
     * @param aiId AI 봇 식별자
     * @return AI 봇 상태 정보
     * @deprecated getAiBotAccount() 사용 권장
     */
    @Deprecated
    public Map<String, Object> getAiBotStatus(Integer aiId) {
        log.warn("Deprecated 메서드 호출: getAiBotStatus(). getAiBotAccount() 사용을 권장합니다.");

        //ai_id + is_ai 기준으로 AI 유저 조회
        User aiUser = userRepository.findByAiIdAndIsAi(aiId, true)
                .orElseThrow(() -> new RuntimeException("해당 aiId의 AI 봇 사용자를 찾을 수 없습니다."));

        //해당 user_id 기준으로 봇 전용 계좌 조회
        Account aiAccount = accountRepository.findByUserIdAndType(aiUser.getUserId(), ACCOUNT_TYPE_MOCK_TRADING)
                .orElseThrow(() -> new RuntimeException("AI 봇 계좌를 찾을 수 없습니다."));

        //거래 횟수 및 평균값 조회
        Long tradeCount = mockTradeRepository.countByAccountId(aiAccount.getAccountId());
        Double avgTradeValue = mockTradeRepository.findAvgTradeValue(aiAccount.getAccountId());

        //결과 구성
        Map<String, Object> result = new HashMap<>();
        result.put("nickname", aiUser.getNickname());
        result.put("balance", aiAccount.getBalance());
        result.put("profitRate", aiAccount.getProfitRate());
        result.put("tradeCount", tradeCount);
        result.put("avgTradeValue", avgTradeValue);

        return result;
    }

    /**
     * 모든 AI 봇 목록 조회
     * @return AI 봇 목록
     */
    public List<AiBotListResponse> getAllAiBots() {
        log.info("모든 AI 봇 목록 조회 시작");

        List<User> aiBots = userRepository.findByIsAiTrueOrderByAiIdAsc();
        
        return aiBots.stream()
                .map(aiBot -> {
                    // 각 AI 봇의 계좌 정보 조회 (계좌는 무조건 존재)
                    Account account = accountRepository.findByUserIdAndType(
                            aiBot.getUserId(), ACCOUNT_TYPE_MOCK_TRADING
                    ).orElseThrow(() -> new RuntimeException("AI 봇 계좌를 찾을 수 없습니다: " + aiBot.getUserId()));
                    
                    log.info("AI 봇 조회: userId={}, aiId={}, nickname={}, personality={}, totalAsset={}, profit={}, profitRate={}%", 
                            aiBot.getUserId(), aiBot.getAiId(), aiBot.getNickname(), aiBot.getPersonality(),
                            account.getTotalAsset(), account.getProfit(), account.getProfitRate());
                    
                    return AiBotListResponse.builder()
                            .userId(aiBot.getUserId())
                            .aiId(aiBot.getAiId())
                            .nickname(aiBot.getNickname())
                            .personality(aiBot.getPersonality())
                            .totalAsset(account.getTotalAsset())
                            .profit(account.getProfit())
                            .profitRate(account.getProfitRate())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
