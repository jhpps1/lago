package com.lago.app.data.cache

import android.util.LruCache
import com.lago.app.domain.entity.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChartMemoryCache @Inject constructor() {
    
    companion object {
        // 캐시 크기 설정 (종목 수 기준)
        private const val STOCK_INFO_CACHE_SIZE = 100      // 100개 종목
        private const val CANDLESTICK_CACHE_SIZE = 50      // 50개 차트 데이터셋
        private const val VOLUME_CACHE_SIZE = 50           // 50개 거래량 데이터셋
        private const val INDICATOR_CACHE_SIZE = 30        // 30개 기술지표 데이터셋
        
        // 차트 데이터 포인트 제한
        private const val MAX_CHART_POINTS = 2000          // 차트당 최대 2000개 포인트
        
        private const val TAG = "ChartMemoryCache"
    }
    
    // 주식 기본 정보 캐시
    private val stockInfoCache = LruCache<String, CachedChartStockInfo>(STOCK_INFO_CACHE_SIZE)
    
    // 캔들스틱 데이터 캐시 (stockCode_timeFrame 키)
    private val candlestickCache = LruCache<String, CachedCandlestickData>(CANDLESTICK_CACHE_SIZE)
    
    // 거래량 데이터 캐시
    private val volumeCache = LruCache<String, CachedVolumeData>(VOLUME_CACHE_SIZE)
    
    // 기술지표 데이터 캐시
    private val indicatorCache = LruCache<String, CachedIndicatorData>(INDICATOR_CACHE_SIZE)
    
    // ===== STOCK INFO CACHE =====
    
    fun getChartStockInfo(stockCode: String): ChartStockInfo? {
        val cached = stockInfoCache.get(stockCode)
        return if (cached != null && !cached.isExpired()) {
            cached.data
        } else {
            stockInfoCache.remove(stockCode)
            null
        }
    }
    
    fun putChartStockInfo(stockCode: String, stockInfo: ChartStockInfo, ttlMinutes: Int = 5) {
        val cached = CachedChartStockInfo(
            data = stockInfo,
            cachedAt = System.currentTimeMillis(),
            ttlMillis = ttlMinutes * 60 * 1000L
        )
        stockInfoCache.put(stockCode, cached)
    }
    
    // ===== CANDLESTICK DATA CACHE =====
    
    fun getCandlestickData(stockCode: String, timeFrame: String): List<CandlestickData>? {
        val key = "${stockCode}_${timeFrame}"
        val cached = candlestickCache.get(key)
        return if (cached != null && !cached.isExpired()) {
            cached.data.takeLast(MAX_CHART_POINTS) // 최대 포인트 수 제한
        } else {
            candlestickCache.remove(key)
            null
        }
    }
    
    fun putCandlestickData(
        stockCode: String, 
        timeFrame: String, 
        data: List<CandlestickData>, 
        ttlMinutes: Int = 15
    ) {
        val key = "${stockCode}_${timeFrame}"
        val cached = CachedCandlestickData(
            data = data.takeLast(MAX_CHART_POINTS), // 포인트 수 제한
            cachedAt = System.currentTimeMillis(),
            ttlMillis = ttlMinutes * 60 * 1000L
        )
        candlestickCache.put(key, cached)
    }
    
    /**
     * 실시간 데이터 추가 (기존 캐시에 새 포인트 추가)
     */
    fun appendCandlestickData(stockCode: String, timeFrame: String, newData: CandlestickData) {
        val key = "${stockCode}_${timeFrame}"
        val cached = candlestickCache.get(key)
        
        if (cached != null && !cached.isExpired()) {
            val updatedData = cached.data.toMutableList().apply {
                // 같은 시간대면 마지막 데이터 교체, 다르면 추가
                if (isNotEmpty() && isSameTimeframe(last().time, newData.time, timeFrame)) {
                    set(size - 1, newData)
                } else {
                    add(newData)
                }
            }.takeLast(MAX_CHART_POINTS)
            
            val updatedCached = CachedCandlestickData(
                data = updatedData,
                cachedAt = cached.cachedAt, // 원래 캐시 시간 유지
                ttlMillis = cached.ttlMillis
            )
            candlestickCache.put(key, updatedCached)
        }
    }
    
    // ===== VOLUME DATA CACHE =====
    
    fun getVolumeData(stockCode: String, timeFrame: String): List<VolumeData>? {
        val key = "${stockCode}_${timeFrame}"
        val cached = volumeCache.get(key)
        return if (cached != null && !cached.isExpired()) {
            cached.data.takeLast(MAX_CHART_POINTS)
        } else {
            volumeCache.remove(key)
            null
        }
    }
    
    fun putVolumeData(
        stockCode: String, 
        timeFrame: String, 
        data: List<VolumeData>, 
        ttlMinutes: Int = 15
    ) {
        val key = "${stockCode}_${timeFrame}"
        val cached = CachedVolumeData(
            data = data.takeLast(MAX_CHART_POINTS),
            cachedAt = System.currentTimeMillis(),
            ttlMillis = ttlMinutes * 60 * 1000L
        )
        volumeCache.put(key, cached)
    }
    
    // ===== INDICATOR DATA CACHE =====
    
    fun getIndicatorData(stockCode: String, timeFrame: String, indicators: List<String>): ChartIndicatorData? {
        val indicatorKey = indicators.sorted().joinToString(",")
        val key = "${stockCode}_${timeFrame}_${indicatorKey}"
        val cached = indicatorCache.get(key)
        return if (cached != null && !cached.isExpired()) {
            cached.data
        } else {
            indicatorCache.remove(key)
            null
        }
    }
    
    fun putIndicatorData(
        stockCode: String, 
        timeFrame: String, 
        indicators: List<String>,
        data: ChartIndicatorData, 
        ttlMinutes: Int = 30
    ) {
        val indicatorKey = indicators.sorted().joinToString(",")
        val key = "${stockCode}_${timeFrame}_${indicatorKey}"
        val cached = CachedIndicatorData(
            data = data,
            cachedAt = System.currentTimeMillis(),
            ttlMillis = ttlMinutes * 60 * 1000L
        )
        indicatorCache.put(key, cached)
    }
    
    // ===== CACHE MANAGEMENT =====
    
    /**
     * 특정 종목의 모든 캐시 무효화
     */
    fun invalidateStock(stockCode: String) {
        stockInfoCache.remove(stockCode)
        
        // 캔들스틱 캐시에서 해당 종목 제거
        val candlestickKeys = mutableListOf<String>()
        candlestickCache.snapshot().keys.forEach { key ->
            if (key.startsWith("${stockCode}_")) {
                candlestickKeys.add(key)
            }
        }
        candlestickKeys.forEach { candlestickCache.remove(it) }
        
        // 거래량 캐시에서 해당 종목 제거
        val volumeKeys = mutableListOf<String>()
        volumeCache.snapshot().keys.forEach { key ->
            if (key.startsWith("${stockCode}_")) {
                volumeKeys.add(key)
            }
        }
        volumeKeys.forEach { volumeCache.remove(it) }
        
        // 기술지표 캐시에서 해당 종목 제거
        val indicatorKeys = mutableListOf<String>()
        indicatorCache.snapshot().keys.forEach { key ->
            if (key.startsWith("${stockCode}_")) {
                indicatorKeys.add(key)
            }
        }
        indicatorKeys.forEach { indicatorCache.remove(it) }
    }
    
    /**
     * 만료된 캐시 정리
     */
    fun clearExpired() {
        // Stock Info 만료 캐시 정리
        val expiredStockKeys = mutableListOf<String>()
        stockInfoCache.snapshot().forEach { (key, cached) ->
            if (cached.isExpired()) {
                expiredStockKeys.add(key)
            }
        }
        expiredStockKeys.forEach { stockInfoCache.remove(it) }
        
        // Candlestick 만료 캐시 정리
        val expiredCandlestickKeys = mutableListOf<String>()
        candlestickCache.snapshot().forEach { (key, cached) ->
            if (cached.isExpired()) {
                expiredCandlestickKeys.add(key)
            }
        }
        expiredCandlestickKeys.forEach { candlestickCache.remove(it) }
        
        // Volume 만료 캐시 정리
        val expiredVolumeKeys = mutableListOf<String>()
        volumeCache.snapshot().forEach { (key, cached) ->
            if (cached.isExpired()) {
                expiredVolumeKeys.add(key)
            }
        }
        expiredVolumeKeys.forEach { volumeCache.remove(it) }
        
        // Indicator 만료 캐시 정리
        val expiredIndicatorKeys = mutableListOf<String>()
        indicatorCache.snapshot().forEach { (key, cached) ->
            if (cached.isExpired()) {
                expiredIndicatorKeys.add(key)
            }
        }
        expiredIndicatorKeys.forEach { indicatorCache.remove(it) }
    }
    
    /**
     * 모든 캐시 초기화
     */
    fun clearAll() {
        stockInfoCache.evictAll()
        candlestickCache.evictAll()
        volumeCache.evictAll()
        indicatorCache.evictAll()
    }
    
    /**
     * 캐시 통계 정보
     */
    fun getCacheStats(): String {
        return """
            Cache Statistics:
            - Stock Info: ${stockInfoCache.size()}/${STOCK_INFO_CACHE_SIZE}
            - Candlestick: ${candlestickCache.size()}/${CANDLESTICK_CACHE_SIZE}
            - Volume: ${volumeCache.size()}/${VOLUME_CACHE_SIZE}
            - Indicators: ${indicatorCache.size()}/${INDICATOR_CACHE_SIZE}
        """.trimIndent()
    }
    
    // ===== HELPER METHODS =====
    
    private fun isSameTimeframe(time1: Long, time2: Long, timeframe: String): Boolean {
        val diff = kotlin.math.abs(time1 - time2)
        
        return when (timeframe) {
            "1" -> diff < 60 * 1000L // 1분
            "3" -> diff < 3 * 60 * 1000L // 3분
            "5" -> diff < 5 * 60 * 1000L // 5분
            "15" -> diff < 15 * 60 * 1000L // 15분
            "30" -> diff < 30 * 60 * 1000L // 30분
            "60" -> diff < 60 * 60 * 1000L // 1시간
            "D" -> {
                val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = time1 }
                val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = time2 }
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
                cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
            }
            else -> false
        }
    }
}

// ===== CACHED DATA MODELS =====

data class CachedChartStockInfo(
    val data: ChartStockInfo,
    val cachedAt: Long,
    val ttlMillis: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > cachedAt + ttlMillis
}

data class CachedCandlestickData(
    val data: List<CandlestickData>,
    val cachedAt: Long,
    val ttlMillis: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > cachedAt + ttlMillis
}

data class CachedVolumeData(
    val data: List<VolumeData>,
    val cachedAt: Long,
    val ttlMillis: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > cachedAt + ttlMillis
}

data class CachedIndicatorData(
    val data: ChartIndicatorData,
    val cachedAt: Long,
    val ttlMillis: Long
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > cachedAt + ttlMillis
}