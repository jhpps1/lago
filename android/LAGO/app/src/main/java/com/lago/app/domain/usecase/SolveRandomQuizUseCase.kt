package com.lago.app.domain.usecase

import com.lago.app.domain.entity.QuizResult
import com.lago.app.domain.repository.StudyRepository
import javax.inject.Inject

class SolveRandomQuizUseCase @Inject constructor(
    private val repository: StudyRepository
) {
    suspend operator fun invoke(userId: Int, quizId: Int, userAnswer: Boolean): Result<QuizResult> = 
        repository.solveRandomQuiz(userId, quizId, userAnswer)
}