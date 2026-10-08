package com.lago.app.domain.entity

data class HistoryChallengeNews(
    val challengeNewsId: Int,
    val challengeId: Int,
    val title: String,
    val content: String,
    val publishedAt: String,
    val imageUrl: String = "" // 이미지 URL 필드 추가
)