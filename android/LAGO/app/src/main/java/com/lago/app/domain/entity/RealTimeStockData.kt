package com.lago.app.domain.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.google.gson.annotations.SerializedName

@Serializable
data class StockRealTimeData(
    @SerialName("code")
    @SerializedName("code")
    val stockCode: String,

    @SerialName("closePrice")
    @SerializedName("closePrice")
    val closePrice: Long? = null,

    @SerialName("openPrice")
    @SerializedName("openPrice")
    val openPrice: Long? = null,

    @SerialName("highPrice")
    @SerializedName("highPrice")
    val highPrice: Long? = null,

    @SerialName("lowPrice")
    @SerializedName("lowPrice")
    val lowPrice: Long? = null,

    @SerialName("volume")
    @SerializedName("volume")
    val volume: Long? = null,

    @SerialName("minuteKey")
    @SerializedName("minuteKey")
    val minuteKey: String? = null,

    @SerialName("parsedDateTime")
    @SerializedName("parsedDateTime")
    val parsedDateTime: String? = null,

    // 호환용 (다른 소스에서 올 수도 있음)
    @SerialName("tradePrice")
    @SerializedName("tradePrice")
    val tradePrice: Long? = null,

    @SerialName("currentPrice")
    @SerializedName("currentPrice")
    val currentPrice: Long? = null,

    // 가격 변동 정보
    @SerialName("changePrice")
    @SerializedName("changePrice")
    val changePrice: Long? = null,

    @SerialName("changeRate")
    @SerializedName("changeRate")
    val changeRate: Double? = null,

    @SerialName("change")
    @SerializedName("change")
    val change: Long? = null,

    @SerialName("rate")
    @SerializedName("rate")
    val rate: Double? = null,

    // 웹소켓에서 오는 등락률 (fluctuationRate)
    @SerialName("fluctuationRate")
    @SerializedName("fluctuationRate")
    val fluctuationRate: Double? = null,

    // 웹소켓에서 오는 시간 (date: "102821" = 10시 28분 21초)
    @SerialName("date")
    @SerializedName("date")
    val date: String? = null,

    // 역사챌린지용 실제 데이터 시간 (예: "2021-06-30T14:10:00")
    @SerialName("originDateTime")
    @SerializedName("originDateTime")
    val originDateTime: String? = null,

    // 웹소켓 수신 시간 (예: "2025-08-16T03:26:55")
    @SerialName("eventDateTime")
    @SerializedName("eventDateTime")
    val eventDateTime: String? = null,

    // 전일 대비 가격 차이 (previousDay: +160 또는 -160, ± 포함)
    @SerialName("previousDay")
    @SerializedName("previousDay")
    val previousDay: Int? = null,

    val timestamp: Long = System.currentTimeMillis()
) {
    // 실제 가격 계산 (우선순위: tradePrice > currentPrice > closePrice)
    val price: Double
        get() = (tradePrice ?: currentPrice ?: closePrice ?: 0L).toDouble()

    // 호환성을 위한 기존 필드들
    val priceChange: Double
        get() = (changePrice ?: change ?: 0L).toDouble()

    val priceChangePercent: Double
        get() = fluctuationRate ?: changeRate ?: rate ?: 0.0
    
    /**
     * 웹소켓 데이터를 올바른 KST timestamp로 변환
     * 1. originDateTime 우선 사용 (역사챌린지용 실제 시간)
     * 2. parsedDateTime 사용 (다른 역사 데이터)
     * 3. date: "102821" (10시 28분 21초) -> 오늘 날짜의 해당 시간으로 변환
     * 4. fallback: timestamp 사용
     */
    fun getKstTimestamp(): Long {
        // 1. originDateTime 우선 사용 (역사챌린지 - 실제 과거 시간)
        if (!originDateTime.isNullOrBlank()) {
            try {
                // "2021-06-30T14:10:00" 형태를 KST timestamp로 변환
                val instant = java.time.LocalDateTime.parse(originDateTime.substring(0, 19))
                return instant.toEpochSecond(java.time.ZoneOffset.of("+09:00")) * 1000L
            } catch (e: Exception) {
                android.util.Log.w("StockRealTimeData", "Failed to parse originDateTime: $originDateTime", e)
            }
        }
        
        // 2. parsedDateTime 사용 (다른 역사 데이터)
        if (!parsedDateTime.isNullOrBlank()) {
            try {
                val instant = java.time.LocalDateTime.parse(parsedDateTime.substring(0, 19))
                return instant.toEpochSecond(java.time.ZoneOffset.of("+09:00")) * 1000L
            } catch (e: Exception) {
                android.util.Log.w("StockRealTimeData", "Failed to parse parsedDateTime: $parsedDateTime", e)
            }
        }
        
        // 3. date 필드 사용 (일반적인 실시간 데이터 - 오늘 날짜 기준)
        if (!date.isNullOrBlank() && date.length >= 6) {
            try {
                val hh = date.substring(0, 2).toInt()
                val mm = date.substring(2, 4).toInt() 
                val ss = date.substring(4, 6).toInt()
                
                // 오늘 날짜의 해당 시간으로 KST timestamp 생성
                val kstZone = java.time.ZoneId.of("Asia/Seoul")
                val today = java.time.LocalDate.now(kstZone)
                val dateTime = java.time.LocalDateTime.of(today, java.time.LocalTime.of(hh, mm, ss))
                
                return dateTime.toEpochSecond(java.time.ZoneOffset.of("+09:00")) * 1000L
            } catch (e: Exception) {
                android.util.Log.w("StockRealTimeData", "Failed to parse date: $date", e)
            }
        }
        
        // 4. fallback: 기본 timestamp 사용
        android.util.Log.d("StockRealTimeData", "Using fallback timestamp: $timestamp")
        return timestamp
    }
    
    /**
     * 역사챌린지 모드인지 확인
     */
    fun isHistoricalMode(): Boolean {
        return !originDateTime.isNullOrBlank() || !parsedDateTime.isNullOrBlank()
    }
}

data class PortfolioReturn(
    val stockCode: String,
    val stockName: String,
    val quantity: Int,
    val buyPrice: Double,
    val currentPrice: Double,
    val profit: Double,
    val returnRate: Double,
    val totalValue: Double
)

data class TotalPortfolioSummary(
    val totalInvestment: Double,
    val totalCurrentValue: Double,
    val totalProfit: Double,
    val totalReturnRate: Double,
    val stockReturns: List<PortfolioReturn>
)

enum class StockPriority {
    HOT,    // 현재 보고있는 종목 (차트 화면)
    WARM,   // 포트폴리오 + 관심종목
    COLD    // 전체 목록에서 가끔 보는 종목
}

enum class ScreenType {
    CHART,
    STOCK_LIST,
    PORTFOLIO,
    SUMMARY,
    NEWS
}