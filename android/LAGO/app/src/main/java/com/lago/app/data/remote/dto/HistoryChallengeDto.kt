package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HistoryChallengeStockDto(
    @SerializedName("challenge_id") val challengeId: Int,
    @SerializedName("stock_code") val stockCode: String,
    @SerializedName("stock_name") val stockName: String,
    @SerializedName("current_price") val currentPrice: Float,
    @SerializedName("open_price") val openPrice: Float,
    @SerializedName("high_price") val highPrice: Float,
    @SerializedName("low_price") val lowPrice: Float,
    @SerializedName("close_price") val closePrice: Float,
    @SerializedName("fluctuation_rate") val fluctuationRate: Float,
    @SerializedName("trading_volume") val tradingVolume: Long,
    @SerializedName("market_cap") val marketCap: Long,
    @SerializedName("profit_rate") val profitRate: Float?
)

/**
 * 역사챌린지 응답 (백엔드 HistoryChallengeResponse에 맞춤)
 */
data class HistoryChallengeResponse(
    @SerializedName("challengeId") val challengeId: Int,
    @SerializedName("theme") val theme: String,
    @SerializedName("stockName") val stockName: String,
    @SerializedName("stockCode") val stockCode: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("originDate") val originDate: String,
    @SerializedName("currentPrice") val currentPrice: Int,
    @SerializedName("fluctuationPrice") val fluctuationPrice: Int,
    @SerializedName("fluctuationRate") val fluctuationRate: Float
)

/**
 * 역사챌린지 차트 데이터 응답 (실제 API 응답 형식에 맞춤)
 */
data class HistoryChallengeDataResponse(
    @SerializedName("rowId") val rowId: Int,
    @SerializedName("eventDateTime") val eventDateTime: String,
    @SerializedName("originDateTime") val originDateTime: String,
    @SerializedName("openPrice") val openPrice: Int,
    @SerializedName("highPrice") val highPrice: Int,
    @SerializedName("lowPrice") val lowPrice: Int,
    @SerializedName("closePrice") val closePrice: Int,
    @SerializedName("volume") val volume: Int,
    @SerializedName("fluctuation_price") val fluctuationPrice: Int,
    @SerializedName("fluctuation_rate") val fluctuationRate: Float
)

/**
 * 역사챌린지 뉴스 응답 (백엔드 HistoryChallengeNewsResponse에 맞춤)
 */
data class HistoryChallengeNewsResponse(
    @SerializedName("challengeNewsId") val challengeNewsId: Int,
    @SerializedName("challengeId") val challengeId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String?,
    @SerializedName("publishedAt") val publishedAt: String
)

/**
 * 역사챌린지 웹소켓 데이터 (실제 WebSocket JSON 형식에 맞춤)
 */
data class HistoryChallengeWebSocketData(
    @SerializedName("rowId") val rowId: Int?,
    @SerializedName("eventDateTime") val eventDateTime: String,
    @SerializedName("originDateTime") val originDateTime: String,
    @SerializedName("openPrice") val openPrice: Int,
    @SerializedName("highPrice") val highPrice: Int,
    @SerializedName("lowPrice") val lowPrice: Int,
    @SerializedName("closePrice") val closePrice: Int,
    @SerializedName("volume") val volume: Int,
    @SerializedName("fluctuationPrice") val fluctuationPrice: Int, // WebSocket에서는 camelCase
    @SerializedName("fluctuationRate") val fluctuationRate: Float  // WebSocket에서는 camelCase
)

/**
 * 주식 일봉 데이터 DTO
 */
data class StockDayCandleDto(
    @SerializedName("date") val date: String,
    @SerializedName("open") val open: Float,
    @SerializedName("high") val high: Float,
    @SerializedName("low") val low: Float,
    @SerializedName("close") val close: Float,
    @SerializedName("volume") val volume: Long
)