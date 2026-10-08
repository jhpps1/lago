package com.lago.app.data.repository

import com.lago.app.data.remote.api.PortfolioApiService
import com.lago.app.data.remote.dto.*
import com.lago.app.domain.repository.PortfolioRepository
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 포트폴리오 Repository 구현체
 * 백엔드 PortfolioController API와 연동
 */
@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val portfolioApiService: PortfolioApiService
) : PortfolioRepository {

    override suspend fun getUserPortfolio(userId: Long): Flow<Resource<List<StockHoldingResponse>>> = flow {
        try {
            emit(Resource.Loading())
            val response = portfolioApiService.getUserPortfolio(userId)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("포트폴리오 조회 실패: ${e.message}"))
        }
    }

    override suspend fun getAccountHoldings(
        accountId: Long,
        userId: Long
    ): Flow<Resource<List<StockHoldingResponse>>> = flow {
        try {
            emit(Resource.Loading())
            val response = portfolioApiService.getAccountHoldings(accountId, userId)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("계좌 보유주식 조회 실패: ${e.message}"))
        }
    }

    override suspend fun getStockHolding(
        accountId: Long,
        stockCode: String,
        userId: Long
    ): Flow<Resource<StockHoldingResponse>> = flow {
        try {
            emit(Resource.Loading())
            val response = portfolioApiService.getStockHolding(accountId, stockCode, userId)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("종목 보유정보 조회 실패: ${e.message}"))
        }
    }

    override suspend fun getUserCurrentStatus(
        userId: Long,
        accountType: Int
    ): Flow<Resource<AccountCurrentStatusResponse>> = flow {
        try {
            emit(Resource.Loading())
            val response = portfolioApiService.getUserCurrentStatus(userId, accountType)
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("계좌 현재상황 조회 실패: ${e.message}"))
        }
    }

    override suspend fun getTransactionHistory(
        userId: Long,
        accountType: Int,
        stockCode: String?
    ): Flow<Resource<List<TransactionHistoryResponse>>> = flow {
        try {
            emit(Resource.Loading())
            val response = when (accountType) {
                1 -> {
                    // 역사챌린지 거래내역
                    portfolioApiService.getHistoricalChallengeTransactionHistory(userId)
                }
                0 -> {
                    // 실시간모의투자 거래내역
                    if (stockCode != null) {
                        portfolioApiService.getTransactionHistoryByStock(userId, stockCode)
                    } else {
                        portfolioApiService.getTransactionHistory(userId)
                    }
                }
                else -> {
                    // 기본값은 실시간모의투자
                    if (stockCode != null) {
                        portfolioApiService.getTransactionHistoryByStock(userId, stockCode)
                    } else {
                        portfolioApiService.getTransactionHistory(userId)
                    }
                }
            }
            emit(Resource.Success(response))
        } catch (e: Exception) {
            emit(Resource.Error("거래내역 조회 실패: ${e.message}"))
        }
    }
}