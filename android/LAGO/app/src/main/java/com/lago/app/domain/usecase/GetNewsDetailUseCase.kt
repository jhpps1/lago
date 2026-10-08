package com.lago.app.domain.usecase

import com.lago.app.domain.entity.News
import com.lago.app.domain.repository.NewsRepository
import javax.inject.Inject

class GetNewsDetailUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(newsId: Int): Result<News> {
        return newsRepository.getNewsDetail(newsId)
    }
}