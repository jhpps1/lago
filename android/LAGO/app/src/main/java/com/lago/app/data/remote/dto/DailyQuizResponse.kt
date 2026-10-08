package com.lago.app.data.remote.dto

data class DailyQuizResponse(
    val alreadySolved: Boolean,
    val quiz: QuizDto?,
    val solvedAt: String?,
    val score: Int?,
    val ranking: Int?
)