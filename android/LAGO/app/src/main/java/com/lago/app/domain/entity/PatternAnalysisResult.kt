package com.lago.app.domain.entity

import com.lago.app.data.remote.dto.PatternAnalysisResponse

/**
 * 차트 패턴 분석 결과 (로컬 저장용)
 */
data class PatternAnalysisResult(
    val stockCode: String,
    val patterns: List<PatternAnalysisResponse>,
    val analysisTime: String,   // 분석 시간 (현재 시간)
    val chartMode: String,      // "mock" | "challenge"
    val timeFrame: String       // "10", "30", "60", etc.
)