package com.lago.app.domain.repository

import com.lago.app.data.remote.dto.CalculatedRankingUser
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface RankingRepository {
    suspend fun getRanking(): Flow<Resource<List<CalculatedRankingUser>>>
}