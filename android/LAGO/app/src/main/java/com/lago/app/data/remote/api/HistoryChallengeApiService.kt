package com.lago.app.data.remote.api

import com.lago.app.data.remote.dto.HistoryChallengeStockDto
import retrofit2.Response
import retrofit2.http.*

interface HistoryChallengeApiService {
    
    @GET("history-challenges/stocks")
    suspend fun getHistoryChallengeStocks(): Response<List<HistoryChallengeStockDto>>
    
    @GET("history-challenges/{challengeId}/stocks")
    suspend fun getHistoryChallengeStocksByChallenge(
        @Path("challengeId") challengeId: Int
    ): Response<List<HistoryChallengeStockDto>>
    
    @GET("api/history-challenge")
    suspend fun getHistoryChallenge(): Response<com.lago.app.data.remote.dto.HistoryChallengeResponse>
}