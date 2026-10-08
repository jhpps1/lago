package com.lago.app.domain.entity

data class DailyQuizResult(
    val correct: Boolean,
    val score: Int,
    val ranking: Int,
    val bonusAmount: Int,
    val explanation: String
)