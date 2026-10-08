package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistoryChallengeNewsDto(
    @SerializedName("challengeNewsId")
    val challengeNewsId: Int,
    
    @SerializedName("challengeId")
    val challengeId: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("publishedAt")
    val publishedAt: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String? = null // 이미지 URL 필드 추가 (nullable)
)