package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 웹소켓 실시간 데이터 DTO
 * 
 * 실시간 차트 데이터 형식:
 * { 
 *   "symbol": "005930", 
 *   "timeframe": "D", 
 *   "o": 74200, 
 *   "h": 74800, 
 *   "l": 73900, 
 *   "c": 74500, 
 *   "v": 12345678, 
 *   "timestamp": 1703123400000 
 * }
 */

// 웹소켓 메시지 기본 구조
data class WebSocketMessage(
    @SerializedName("type") val type: String, // "candlestick", "tick", "order_book"
    @SerializedName("data") val data: Any
)

// 실시간 캔들스틱 데이터
data class RealtimeCandlestickDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("timeframe") val timeframe: String,
    @SerializedName("o") val open: Float,
    @SerializedName("h") val high: Float,
    @SerializedName("l") val low: Float,
    @SerializedName("c") val close: Float,
    @SerializedName("v") val volume: Long,
    @SerializedName("timestamp") val timestamp: Long
)

// 실시간 틱 데이터 (가격 변동)
data class RealtimeTickDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("price") val price: Float,
    @SerializedName("volume") val volume: Long,
    @SerializedName("change") val change: Float,
    @SerializedName("change_percent") val changePercent: Float,
    @SerializedName("timestamp") val timestamp: Long
)

// 웹소켓 구독 메시지
data class SubscribeMessage(
    @SerializedName("action") val action: String, // "subscribe" or "unsubscribe"
    @SerializedName("channel") val channel: String, // "candlestick", "tick"
    @SerializedName("symbol") val symbol: String,
    @SerializedName("timeframe") val timeframe: String? = null
)

// 웹소켓 응답 메시지
data class WebSocketResponse(
    @SerializedName("type") val type: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("status") val status: String?, // "subscribed", "unsubscribed", "error"
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: Any?
)

// 웹소켓 연결 상태
enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

// 웹소켓 이벤트
sealed class WebSocketEvent {
    data class ConnectionStateChanged(val state: WebSocketConnectionState) : WebSocketEvent()
    data class MessageReceived(val message: WebSocketResponse) : WebSocketEvent()
    data class CandlestickReceived(val data: RealtimeCandlestickDto) : WebSocketEvent()
    data class TickReceived(val data: RealtimeTickDto) : WebSocketEvent()
    data class Error(val throwable: Throwable) : WebSocketEvent()
}