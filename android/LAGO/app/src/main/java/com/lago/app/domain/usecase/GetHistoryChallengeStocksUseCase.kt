package com.lago.app.domain.usecase

import com.lago.app.domain.entity.HistoryChallengeStock
import com.lago.app.domain.repository.HistoryChallengeRepository
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryChallengeStocksUseCase @Inject constructor(
    private val repository: HistoryChallengeRepository
) {
    suspend operator fun invoke(): Flow<Resource<List<HistoryChallengeStock>>> {
        return repository.getHistoryChallengeStocks()
    }
    
    suspend operator fun invoke(challengeId: Int): Flow<Resource<List<HistoryChallengeStock>>> {
        return repository.getHistoryChallengeStocksByChallenge(challengeId)
    }
}