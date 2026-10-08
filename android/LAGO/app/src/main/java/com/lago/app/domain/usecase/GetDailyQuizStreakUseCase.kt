package com.lago.app.domain.usecase

import com.lago.app.domain.entity.DailyQuizStreak
import com.lago.app.domain.repository.StudyRepository
import javax.inject.Inject

class GetDailyQuizStreakUseCase @Inject constructor(
    private val repository: StudyRepository
) {
    suspend operator fun invoke(userId: Int): Result<DailyQuizStreak> = 
        repository.getDailyQuizStreak(userId)
}