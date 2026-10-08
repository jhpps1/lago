package com.lago.app.domain.entity

data class QuizResult(
    val correct: Boolean,
    val score: Int,
    val explanation: String
)