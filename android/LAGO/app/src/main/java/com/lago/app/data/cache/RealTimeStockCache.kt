package com.lago.app.data.cache

import android.util.LruCache
import com.lago.app.domain.entity.StockRealTimeData
import com.lago.app.domain.entity.StockPriority
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealTimeStockCache @Inject constructor() {
    
    // 핵심: 단일 StateFlow로 모든 종목 데이터 관리
    private val _quotes = MutableStateFlow<Map<String, StockRealTimeData>>(emptyMap())
    val quotes: StateFlow<Map<String, StockRealTimeData>> = _quotes.asStateFlow()
    
    // L1: 즉시 접근 (현재 화면) - 50개
    private val hotCache = LruCache<String, StockRealTimeData>(50)
    
    // L2: 자주 사용 (포트폴리오, 관심종목) - 200개
    private val warmCache = LruCache<String, StockRealTimeData>(200)
    
    // L3: 가끔 사용 (전체 목록) - 1000개
    private val coldCache = LruCache<String, StockRealTimeData>(1000)
    
    // 종목별 우선순위 추적
    private val stockPriorities = mutableMapOf<String, StockPriority>()
    
    fun updateStock(stockCode: String, data: StockRealTimeData) {
        // NPE 방지 검증
        require(stockCode.isNotBlank()) { "stockCode cannot be null or blank" }
        require(data.stockCode.isNotBlank()) { "data.stockCode cannot be null or blank" }
        
        android.util.Log.v("RealTimeStockCache", "📥 캐시 업데이트 요청: $stockCode = ${data.price.toInt()}원")
        
        // StateFlow 업데이트 (UI 자동 갱신의 핵심!)
        _quotes.update { oldMap ->
            val currentData = oldMap[stockCode]
            // 값이 동일하면 스킵 (불필요한 리렌더링 방지)
            if (currentData?.closePrice == data.closePrice && 
                currentData?.volume == data.volume &&
                currentData?.price == data.price) {
                android.util.Log.v("RealTimeStockCache", "⏭️ $stockCode: 동일한 데이터 스킵")
                return@update oldMap
            }
            
            android.util.Log.d("RealTimeStockCache", "✅ 캐시 업데이트: $stockCode = ${data.price.toInt()}원 (캐시 크기: ${oldMap.size + 1})")
            
            // 새 맵 생성하여 반환 (immutability 유지)
            oldMap.toMutableMap().apply { 
                put(stockCode, data) 
            }
        }
        
        // 우선순위별 캐시 저장은 유지
        val priority = stockPriorities[stockCode] ?: StockPriority.COLD
        when (priority) {
            StockPriority.HOT -> hotCache.put(stockCode, data)
            StockPriority.WARM -> warmCache.put(stockCode, data)
            StockPriority.COLD -> coldCache.put(stockCode, data)
        }
    }
    
    fun getStockData(stockCode: String): StockRealTimeData? {
        // StateFlow에서 먼저 확인 (최신 데이터)
        return _quotes.value[stockCode]
            ?: hotCache.get(stockCode)
            ?: warmCache.get(stockCode)
            ?: coldCache.get(stockCode)
    }
    
    // 특정 종목의 Flow 반환 (차트용)
    fun symbolFlow(stockCode: String): Flow<StockRealTimeData> =
        quotes.mapNotNull { it[stockCode] }
            .distinctUntilChanged { old, new ->
                old.closePrice == new.closePrice && 
                old.volume == new.volume &&
                old.price == new.price
            }
    
    fun setStockPriority(stockCode: String, priority: StockPriority) {
        stockPriorities[stockCode] = priority
        
        // 우선순위 변경 시 캐시 이동
        moveStockToAppropriateCache(stockCode, priority)
    }
    
    fun setMultipleStockPriorities(stocks: Map<String, StockPriority>) {
        stockPriorities.putAll(stocks)
        stocks.forEach { (stockCode, priority) ->
            moveStockToAppropriateCache(stockCode, priority)
        }
    }
    
    private fun moveStockToAppropriateCache(stockCode: String, priority: StockPriority) {
        // 기존 캐시에서 데이터 찾기
        val data = getStockData(stockCode) ?: return
        
        // 모든 캐시에서 제거
        hotCache.remove(stockCode)
        warmCache.remove(stockCode)
        coldCache.remove(stockCode)
        
        // 새로운 우선순위 캐시에 저장
        when (priority) {
            StockPriority.HOT -> hotCache.put(stockCode, data)
            StockPriority.WARM -> warmCache.put(stockCode, data)
            StockPriority.COLD -> coldCache.put(stockCode, data)
        }
    }
    
    fun clearStockPriority(stockCode: String) {
        stockPriorities.remove(stockCode)
        
        // HOT/WARM에서 COLD로 이동
        val data = getStockData(stockCode)
        if (data != null) {
            hotCache.remove(stockCode)
            warmCache.remove(stockCode)
            coldCache.put(stockCode, data)
        }
    }
    
    fun getCacheStats(): String {
        return """
            Hot Cache: ${hotCache.size()}/50
            Warm Cache: ${warmCache.size()}/200  
            Cold Cache: ${coldCache.size()}/1000
            Priorities: ${stockPriorities.size}
        """.trimIndent()
    }
}