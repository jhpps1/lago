package com.lago.app.data.remote.dto

data class DailyQuizSolveResponse(
    val correct: Boolean,
    val score: Int,
    val ranking: Int,
    val bonusAmount: Int,
    val explanation: String
)