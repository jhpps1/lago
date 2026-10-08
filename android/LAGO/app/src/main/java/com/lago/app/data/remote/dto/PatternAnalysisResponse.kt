package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 차트 패턴 분석 응답 DTO
 */
data class PatternAnalysisResponse(
    @SerializedName("name")
    val name: String,           // "더블 탑 패턴"
    
    @SerializedName("reason")
    val reason: String          // "고점이 두 번 형성되며 상승 추세가 멈추고 하락 반전될 가능성을 나타내는 패턴입니다."
)