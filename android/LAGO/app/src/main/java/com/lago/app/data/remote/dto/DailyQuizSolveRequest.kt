package com.lago.app.data.remote.dto

data class DailyQuizSolveRequest(
    val userId: Int,
    val quizId: Int,
    val userAnswer: Boolean,
    val solvedTimeSeconds: Int
)