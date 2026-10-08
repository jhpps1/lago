package com.lago.app.data.remote.dto

data class RandomQuizSolveRequest(
    val userId: Int,
    val quizId: Int,
    val userAnswer: Boolean
)