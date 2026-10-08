package com.lago.app.data.repository

import com.lago.app.data.remote.api.HistoryChallengeApiService
import com.lago.app.data.remote.dto.HistoryChallengeStockDto
import com.lago.app.domain.entity.HistoryChallengeStock
import com.lago.app.domain.repository.HistoryChallengeRepository
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryChallengeRepositoryImpl @Inject constructor(
    private val apiService: HistoryChallengeApiService
) : HistoryChallengeRepository {
    
    override suspend fun getHistoryChallengeStocks(): Flow<Resource<List<HistoryChallengeStock>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getHistoryChallengeStocks()
            if (response.isSuccessful) {
                val stocks = response.body()?.map { it.toEntity() } ?: emptyList()
                emit(Resource.Success(stocks))
            } else {
                emit(Resource.Error("서버 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("네트워크 오류: ${e.message}"))
        }
    }
    
    override suspend fun getHistoryChallengeStocksByChallenge(challengeId: Int): Flow<Resource<List<HistoryChallengeStock>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getHistoryChallengeStocksByChallenge(challengeId)
            if (response.isSuccessful) {
                val stocks = response.body()?.map { it.toEntity() } ?: emptyList()
                emit(Resource.Success(stocks))
            } else {
                emit(Resource.Error("서버 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("네트워크 오류: ${e.message}"))
        }
    }
    
    override suspend fun getHistoryChallenge(): Flow<Resource<com.lago.app.data.remote.dto.HistoryChallengeResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getHistoryChallenge()
            if (response.isSuccessful) {
                val historyChallenge = response.body()
                if (historyChallenge != null) {
                    emit(Resource.Success(historyChallenge))
                } else {
                    emit(Resource.Error("빈 응답"))
                }
            } else {
                emit(Resource.Error("서버 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("네트워크 오류: ${e.message}"))
        }
    }
}

private fun HistoryChallengeStockDto.toEntity(): HistoryChallengeStock {
    return HistoryChallengeStock(
        challengeId = challengeId,
        stockCode = stockCode,
        stockName = stockName,
        currentPrice = currentPrice,
        openPrice = openPrice,
        highPrice = highPrice,
        lowPrice = lowPrice,
        closePrice = closePrice,
        fluctuationRate = fluctuationRate,
        tradingVolume = tradingVolume,
        marketCap = marketCap,
        profitRate = profitRate
    )
}