package com.lago.app.domain.usecase

import com.lago.app.domain.entity.HistoryChallengeNews
import com.lago.app.domain.repository.NewsRepository
import javax.inject.Inject

class GetHistoryChallengeNewsDetailUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(challengeId: Int, challengeNewsId: Int): Result<HistoryChallengeNews> {
        return newsRepository.getHistoryChallengeNewsDetail(challengeId, challengeNewsId)
    }
}