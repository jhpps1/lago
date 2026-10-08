package com.lago.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lago.app.domain.entity.CandlestickData

@Entity(tableName = "cached_chart_data")
data class CachedChartData(
    @PrimaryKey 
    val id: String, // stockCode + timeFrame 조합 (예: "005930_D")
    val stockCode: String,
    val timeFrame: String,
    val data: List<CandlestickData>,
    val lastUpdated: Long, // 캐시된 시간 (System.currentTimeMillis())
    val expiryTime: Long = lastUpdated + (5 * 60 * 1000) // 5분 후 만료
) {
    companion object {
        fun createId(stockCode: String, timeFrame: String) = "${stockCode}_${timeFrame}"
    }
}

@Entity(tableName = "cached_stock_info")
data class CachedStockInfo(
    @PrimaryKey
    val stockCode: String,
    val name: String,
    val currentPrice: Float,
    val priceChange: Float,
    val priceChangePercent: Float,
    val previousDay: Int?,
    val lastUpdated: Long,
    val expiryTime: Long = lastUpdated + (2 * 60 * 1000) // 2분 후 만료
)

// TypeConverters are defined in Converters.kt