package com.lago.app.domain.entity

import java.util.*

/**
 * 매수/매도 신호 데이터 엔티티
 */
data class TradingSignal(
    val id: String,
    val stockCode: String,
    val signalType: SignalType,
    val signalSource: SignalSource,
    val timestamp: Date,
    val price: Double,
    val message: String? = null
)

/**
 * 신호 타입 (매수/매도)
 */
enum class SignalType {
    BUY,    // 매수
    SELL    // 매도
}

/**
 * 신호 소스 (사용자/AI)
 */
enum class SignalSource(
    val displayName: String
) {
    USER("사용자"),
    AI_BLUE("AI 파랑"),
    AI_GREEN("AI 초록"),
    AI_RED("AI 빨강"),
    AI_YELLOW("AI 노랑")
}