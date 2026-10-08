package com.lago.app.domain.repository

import com.lago.app.domain.entity.HistoryChallengeStock
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface HistoryChallengeRepository {
    
    suspend fun getHistoryChallengeStocks(): Flow<Resource<List<HistoryChallengeStock>>>
    
    suspend fun getHistoryChallengeStocksByChallenge(challengeId: Int): Flow<Resource<List<HistoryChallengeStock>>>
    
    /**
     * 역사챌린지 조회 (API: GET /api/history-challenge)
     */
    suspend fun getHistoryChallenge(): Flow<Resource<com.lago.app.data.remote.dto.HistoryChallengeResponse>>
}