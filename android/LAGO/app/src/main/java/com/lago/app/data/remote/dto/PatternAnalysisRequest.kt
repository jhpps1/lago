package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 차트 패턴 분석 요청 DTO
 */
data class PatternAnalysisRequest(
    @SerializedName("stockCode")
    val stockCode: String,
    
    @SerializedName("chartMode")
    val chartMode: String,      // "mock" | "challenge"
    
    @SerializedName("interval")
    val interval: String,       // "MINUTE1", "MINUTE5", etc.
    
    @SerializedName("fromDateTime")
    val fromDateTime: String,   // "2025-08-17T09:30:00"
    
    @SerializedName("toDateTime")
    val toDateTime: String      // "2025-08-17T15:30:00"
)