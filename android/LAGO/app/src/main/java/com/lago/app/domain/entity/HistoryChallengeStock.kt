package com.lago.app.domain.entity

data class HistoryChallengeStock(
    val challengeId: Int,
    val stockCode: String,
    val stockName: String,
    val currentPrice: Float,
    val openPrice: Float,
    val highPrice: Float,
    val lowPrice: Float,
    val closePrice: Float,
    val fluctuationRate: Float,         // 등락률 (%)
    val changePrice: Float = 0f,        // 전일대비 가격차이 (원)
    val tradingVolume: Long,
    val marketCap: Long? = null,
    val profitRate: Float? = null
)