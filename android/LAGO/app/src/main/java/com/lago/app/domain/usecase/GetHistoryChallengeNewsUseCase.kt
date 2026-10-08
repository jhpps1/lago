package com.lago.app.domain.usecase

import com.lago.app.domain.entity.HistoryChallengeNews
import com.lago.app.domain.repository.NewsRepository
import javax.inject.Inject

class GetHistoryChallengeNewsUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(challengeId: Int, pastDateTime: String): Result<List<HistoryChallengeNews>> {
        return newsRepository.getHistoryChallengeNews(challengeId, pastDateTime)
    }
}