package com.lago.app.domain.usecase

import com.lago.app.data.remote.dto.PatternAnalysisRequest
import com.lago.app.data.remote.dto.PatternAnalysisResponse
import com.lago.app.domain.repository.ChartRepository
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AnalyzeChartPatternUseCase @Inject constructor(
    private val chartRepository: ChartRepository
) {
    suspend operator fun invoke(
        request: PatternAnalysisRequest
    ): Flow<Resource<List<PatternAnalysisResponse>>> {
        return chartRepository.analyzeChartPattern(request)
    }
}