package com.lago.app.domain.entity

data class DailyQuizStatus(
    val alreadySolved: Boolean,
    val quiz: Quiz?,
    val solvedAt: String?,
    val score: Int?,
    val ranking: Int?
)