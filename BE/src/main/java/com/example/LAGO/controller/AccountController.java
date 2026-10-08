package com.example.LAGO.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.LAGO.domain.Account;
import com.example.LAGO.domain.MockTrade;
import com.example.LAGO.dto.AccountDto;
import com.example.LAGO.dto.response.TransactionHistoryResponse;
import com.example.LAGO.repository.AccountRepository;
import com.example.LAGO.repository.MockTradeRepository;
import com.example.LAGO.service.AccountService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 계좌 조회 API
 */
@Tag(name = "Account", description = "계좌 조회 API")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final MockTradeRepository mockTradeRepository;

    /**
     * GET /api/accounts/{accountId} - 계좌 단건 조회
     */
    @Operation(summary = "계좌 단건 조회", description = "accountId로 계좌 정보를 조회합니다.")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    /**
     * GET /api/accounts/{userId}/transactions - 사용자 전체 거래 내역 조회
     */
    @Operation(
        summary = "사용자 전체 거래 내역 조회", 
        description = "userId로 해당 사용자의 모의투자 계좌(type=0) 전체 거래 내역을 조회합니다."
    )
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>> getTransactionHistory(@PathVariable Long userId) {
        List<TransactionHistoryResponse> transactions = accountService.getTransactionHistoryByUserId(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/accounts/{userId}/transactions/{stockCode} - 사용자 종목별 거래 내역 조회
     */
    @Operation(
        summary = "사용자 종목별 거래 내역 조회", 
        description = "userId와 stockCode로 해당 사용자의 특정 종목 거래 내역을 조회합니다. 모의투자 계좌(type=0)만 대상입니다."
    )
    @GetMapping("/{userId}/transactions/{stockCode}")
    public ResponseEntity<List<TransactionHistoryResponse>> getTransactionHistoryByStock(
            @PathVariable Long userId, 
            @PathVariable String stockCode) {
        List<TransactionHistoryResponse> transactions = accountService.getTransactionHistoryByUserIdAndStockCode(userId, stockCode);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/accounts/ai/{aiId}/transactions - AI 매매봇 전체 거래 내역 조회
     */
    @Operation(
        summary = "AI 매매봇 전체 거래 내역 조회", 
        description = "aiId로 해당 AI 봇의 AI 봇 계좌(type=2) 전체 거래 내역을 조회합니다."
    )
    @GetMapping("/ai/{aiId}/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>> getAiTransactionHistory(@PathVariable Integer aiId) {
        List<TransactionHistoryResponse> transactions = accountService.getTransactionHistoryByAiId(aiId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/accounts/ai/{aiId}/transactions/{stockCode} - AI 매매봇 종목별 거래 내역 조회
     */
    @Operation(
        summary = "AI 매매봇 종목별 거래 내역 조회", 
        description = "aiId와 stockCode로 해당 AI 봇의 특정 종목 거래 내역을 조회합니다. AI 봇 계좌(type=2)만 대상입니다."
    )
    @GetMapping("/ai/{aiId}/transactions/{stockCode}")
    public ResponseEntity<List<TransactionHistoryResponse>> getAiTransactionHistoryByStock(
            @PathVariable Integer aiId, 
            @PathVariable String stockCode) {
        List<TransactionHistoryResponse> transactions = accountService.getTransactionHistoryByAiIdAndStockCode(aiId, stockCode);
        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/accounts/{userId}/history - 역사챌린지 거래 내역 조회
     */
    @Operation(
        summary = "역사챌린지 거래 내역 조회", 
        description = "userId로 해당 사용자의 역사챌린지 계좌(type=1) 거래 내역을 조회합니다. 종목코드는 068270으로 고정됩니다."
    )
    @GetMapping("/{userId}/history")
    public ResponseEntity<List<TransactionHistoryResponse>> getHistoricalChallengeTransactionHistory(@PathVariable Long userId) {
        List<TransactionHistoryResponse> transactions = accountService.getHistoricalChallengeTransactionHistoryByUserId(userId);
        return ResponseEntity.ok(transactions);
    }
}