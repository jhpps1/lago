package com.lago.app.domain.repository

import com.lago.app.data.remote.dto.*
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * 포트폴리오 관련 Repository 인터페이스
 * 백엔드 PortfolioController와 연동하는 계좌/보유주식 관리
 */
interface PortfolioRepository {

    /**
     * 사용자 포트폴리오 조회 (모든 계좌)
     * @param userId 사용자 ID
     * @return 보유주식 목록
     */
    suspend fun getUserPortfolio(userId: Long): Flow<Resource<List<StockHoldingResponse>>>

    /**
     * 특정 계좌의 보유주식 조회
     * @param accountId 계좌 ID  
     * @param userId 사용자 ID
     * @return 보유주식 목록
     */
    suspend fun getAccountHoldings(
        accountId: Long,
        userId: Long
    ): Flow<Resource<List<StockHoldingResponse>>>

    /**
     * 특정 종목의 보유 정보 조회
     * @param accountId 계좌 ID
     * @param stockCode 종목 코드
     * @param userId 사용자 ID
     * @return 보유주식 정보
     */
    suspend fun getStockHolding(
        accountId: Long,
        stockCode: String,
        userId: Long
    ): Flow<Resource<StockHoldingResponse>>

    /**
     * 사용자 계좌 현재 상황 조회 (프론트 실시간 계산용)
     * @param userId 사용자 ID
     * @param accountType 계좌 타입 (0=실시간모의투자, 1=역사챌린지)
     * @return 계좌 현재 상황
     */
    suspend fun getUserCurrentStatus(
        userId: Long,
        accountType: Int = 0
    ): Flow<Resource<AccountCurrentStatusResponse>>

    /**
     * 거래 내역 조회 (계좌 타입별)
     * @param userId 사용자 ID
     * @param accountType 계좌 타입 (0=실시간모의투자, 1=역사챌린지)
     * @param stockCode 종목 코드 (선택사항)
     * @return 거래 내역 목록
     */
    suspend fun getTransactionHistory(
        userId: Long,
        accountType: Int = 0,
        stockCode: String? = null
    ): Flow<Resource<List<TransactionHistoryResponse>>>
}