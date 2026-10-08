package com.lago.app.domain.usecase

import com.lago.app.domain.entity.DailyQuizResult
import com.lago.app.domain.repository.StudyRepository
import javax.inject.Inject

class SolveDailyQuizUseCase @Inject constructor(
    private val repository: StudyRepository
) {
    suspend operator fun invoke(userId: Int, quizId: Int, userAnswer: Boolean, solvedTimeSeconds: Int): Result<DailyQuizResult> = 
        repository.solveDailyQuiz(userId, quizId, userAnswer, solvedTimeSeconds)
}