package com.lago.app.data.remote.dto

data class RandomQuizSolveResponse(
    val correct: Boolean,
    val score: Int,
    val explanation: String
)