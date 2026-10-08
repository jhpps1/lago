package com.lago.app.data.remote.api

import com.lago.app.data.remote.dto.*
import retrofit2.http.*

/**
 * 포트폴리오 관련 API 서비스
 * 백엔드 PortfolioController와 1:1 매핑
 */
interface PortfolioApiService {

    /**
     * 사용자 포트폴리오 조회 (모든 계좌)
     * GET /api/users/me/portfolio
     */
    @GET("api/users/me/portfolio")
    suspend fun getUserPortfolio(
        @Header("User-Id") userId: Long
    ): List<StockHoldingResponse>

    /**
     * 특정 계좌의 보유주식 조회
     * GET /api/accounts/{accountId}/holdings
     */
    @GET("api/accounts/{accountId}/holdings")
    suspend fun getAccountHoldings(
        @Path("accountId") accountId: Long,
        @Header("User-Id") userId: Long
    ): List<StockHoldingResponse>

    /**
     * 특정 종목의 보유 정보 조회
     * GET /api/accounts/{accountId}/holdings/{stockCode}
     */
    @GET("api/accounts/{accountId}/holdings/{stockCode}")
    suspend fun getStockHolding(
        @Path("accountId") accountId: Long,
        @Path("stockCode") stockCode: String,
        @Header("User-Id") userId: Long
    ): StockHoldingResponse

    /**
     * 사용자 계좌 현재 상황 조회 (프론트 실시간 계산용)
     * GET /api/users/{userId}/current-status?type={accountType}
     */
    @GET("api/users/{userId}/current-status")
    suspend fun getUserCurrentStatus(
        @Path("userId") userId: Long,
        @Query("type") accountType: Int = 0
    ): AccountCurrentStatusResponse

    /**
     * 사용자 전체 거래 내역 조회 (모의투자 계좌)
     * GET /api/accounts/{userId}/transactions
     */
    @GET("api/accounts/{userId}/transactions")
    suspend fun getTransactionHistory(
        @Path("userId") userId: Long
    ): List<TransactionHistoryResponse>

    /**
     * 사용자 종목별 거래 내역 조회 (모의투자 계좌)
     * GET /api/accounts/{userId}/transactions/{stockCode}
     */
    @GET("api/accounts/{userId}/transactions/{stockCode}")
    suspend fun getTransactionHistoryByStock(
        @Path("userId") userId: Long,
        @Path("stockCode") stockCode: String
    ): List<TransactionHistoryResponse>

    /**
     * 역사챌린지 거래 내역 조회
     * GET /api/accounts/{userId}/history
     */
    @GET("api/accounts/{userId}/history")
    suspend fun getHistoricalChallengeTransactionHistory(
        @Path("userId") userId: Long
    ): List<TransactionHistoryResponse>
}