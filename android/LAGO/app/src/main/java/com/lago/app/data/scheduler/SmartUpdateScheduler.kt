package com.lago.app.data.scheduler

import android.util.Log
import com.lago.app.domain.entity.StockRealTimeData
import com.lago.app.domain.entity.ScreenType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartUpdateScheduler @Inject constructor() {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 화면별 업데이트 큐 - 실시간 UI 파이프라인 최적화
    private val updateQueues = mapOf(
        ScreenType.CHART to UpdateQueue(100f, "차트"),           // 10fps (차트 캔들 업데이트)  
        ScreenType.STOCK_LIST to UpdateQueue(250f, "종목리스트"),  // 4fps (종목카드 갱신, 250ms 스로틀)
        ScreenType.PORTFOLIO to UpdateQueue(500f, "포트폴리오"),   // 2fps
        ScreenType.SUMMARY to UpdateQueue(1000f, "요약"),        // 1fps
        ScreenType.NEWS to UpdateQueue(5000f, "뉴스")           // 0.2fps
    )
    
    // 화면별 업데이트 스트림 - replay=1로 설정하여 구독 시점 문제 해결
    private val _chartUpdates = MutableSharedFlow<Map<String, StockRealTimeData>>(replay = 1)
    val chartUpdates: SharedFlow<Map<String, StockRealTimeData>> = _chartUpdates.asSharedFlow()
    
    private val _stockListUpdates = MutableSharedFlow<Map<String, StockRealTimeData>>(replay = 1)
    val stockListUpdates: SharedFlow<Map<String, StockRealTimeData>> = _stockListUpdates.asSharedFlow()
    
    private val _portfolioUpdates = MutableSharedFlow<Map<String, StockRealTimeData>>()
    val portfolioUpdates: SharedFlow<Map<String, StockRealTimeData>> = _portfolioUpdates.asSharedFlow()
    
    private val _summaryUpdates = MutableSharedFlow<Map<String, StockRealTimeData>>()
    val summaryUpdates: SharedFlow<Map<String, StockRealTimeData>> = _summaryUpdates.asSharedFlow()
    
    companion object {
        private const val TAG = "SmartUpdateScheduler"
    }
    
    init {
        startUpdateQueues()
    }
    
    fun scheduleUpdate(screenType: ScreenType, stockCode: String, data: StockRealTimeData) {
        Log.d(TAG, "scheduleUpdate - screenType: $screenType, stockCode: $stockCode, price: ${data.price}")
        scope.launch {
            val queue = updateQueues[screenType]
            if (queue != null) {
                queue.add(stockCode, data)
                Log.d(TAG, "데이터 큐에 추가됨 - $screenType: $stockCode")
            } else {
                Log.w(TAG, "큐를 찾을 수 없음 - screenType: $screenType")
            }
        }
    }
    
    fun scheduleMultipleUpdates(screenType: ScreenType, updates: Map<String, StockRealTimeData>) {
        scope.launch {
            updateQueues[screenType]?.addMultiple(updates)
        }
    }
    
    // 특정 화면의 업데이트 주기 동적 변경
    fun adjustUpdateFrequency(screenType: ScreenType, newIntervalMs: Float) {
        updateQueues[screenType]?.updateInterval(newIntervalMs)
        Log.d(TAG, "$screenType 업데이트 주기 변경: ${newIntervalMs}ms")
    }
    
    // 화면이 포커스를 잃었을 때 업데이트 주기 줄이기
    fun pauseScreen(screenType: ScreenType) {
        updateQueues[screenType]?.pause()
        Log.d(TAG, "$screenType 업데이트 일시정지")
    }
    
    // 화면이 포커스를 얻었을 때 업데이트 재개
    fun resumeScreen(screenType: ScreenType) {
        updateQueues[screenType]?.resume()
        Log.d(TAG, "$screenType 업데이트 재개")
    }
    
    private fun startUpdateQueues() {
        updateQueues.forEach { (screenType, queue) ->
            scope.launch {
                queue.startProcessing { updates ->
                    val emitResult = when (screenType) {
                        ScreenType.CHART -> _chartUpdates.tryEmit(updates)
                        ScreenType.STOCK_LIST -> _stockListUpdates.tryEmit(updates)
                        ScreenType.PORTFOLIO -> _portfolioUpdates.tryEmit(updates)
                        ScreenType.SUMMARY -> _summaryUpdates.tryEmit(updates)
                        ScreenType.NEWS -> { /* 뉴스는 업데이트 스트림 불필요 */ true }
                    }
                    Log.d(TAG, "tryEmit 결과 - $screenType: $emitResult, 업데이트 수: ${updates.size}")
                }
            }
        }
    }
    
    private inner class UpdateQueue(
        private var intervalMs: Float,
        private val name: String
    ) {
        private val pendingUpdates = mutableMapOf<String, StockRealTimeData>()
        private var lastUpdate = 0L
        private var isPaused = false
        private val lock = Mutex()
        
        suspend fun add(stockCode: String, data: StockRealTimeData) {
            if (isPaused) {
                Log.w(TAG, "큐가 일시정지됨 - $name: $stockCode")
                return
            }
            
            lock.withLock {
                val isFirstUpdate = lastUpdate == 0L
                pendingUpdates[stockCode] = data
                Log.d(TAG, "큐에 데이터 추가 - $name: $stockCode, price: ${data.price}, pending: ${pendingUpdates.size}")
                
                val now = System.currentTimeMillis()
                val timeSinceLastUpdate = now - lastUpdate
                
                // 실시간 UI 파이프라인: 스로틀링 + 배치 처리
                val shouldFlush = isFirstUpdate || 
                                 timeSinceLastUpdate >= intervalMs ||
                                 pendingUpdates.size >= 3  // 배치 크기를 3으로 낮춤
                
                if (shouldFlush) {
                    Log.d(TAG, "🚀 flush 실행 - $name: ${pendingUpdates.size}개 배치 (${if (isFirstUpdate) "최초" else "${timeSinceLastUpdate}ms"})")
                    flushUpdates()
                    lastUpdate = now
                } else {
                    Log.v(TAG, "📦 배치 대기 - $name: ${pendingUpdates.size}개 pending (${timeSinceLastUpdate}ms < ${intervalMs}ms)")
                }
            }
        }
        
        suspend fun addMultiple(updates: Map<String, StockRealTimeData>) {
            if (isPaused) return
            
            lock.withLock {
                pendingUpdates.putAll(updates)
                
                val now = System.currentTimeMillis()
                if (now - lastUpdate >= intervalMs) {
                    flushUpdates()
                    lastUpdate = now
                }
            }
        }
        
        suspend fun startProcessing(onFlush: suspend (Map<String, StockRealTimeData>) -> Unit) {
            while (true) {
                delay(100L) // 100ms마다 체크 (더 빠른 반응성)
                
                if (isPaused) continue
                
                lock.withLock {
                    if (pendingUpdates.isNotEmpty()) {
                        val now = System.currentTimeMillis()
                        val timeSinceLastUpdate = now - lastUpdate
                        
                        // 주기적 배치 처리: 시간 경과 또는 배치 크기 초과 시
                        val shouldProcessBatch = timeSinceLastUpdate >= intervalMs || 
                                               pendingUpdates.size >= 3 // 3개만 쌓여도 처리
                        
                        if (shouldProcessBatch) {
                            val updates = pendingUpdates.toMap()
                            pendingUpdates.clear()
                            lastUpdate = now
                            
                            Log.d(TAG, "⏰ $name 주기 배치: ${updates.size}개 (${timeSinceLastUpdate}ms)")
                            
                            // UI 반영 (메인 스레드)
                            onFlush(updates)
                        }
                    }
                }
            }
        }
        
        private suspend fun flushUpdates() {
            if (pendingUpdates.isNotEmpty()) {
                val updates = pendingUpdates.toMap()
                pendingUpdates.clear()
                
                Log.d(TAG, "$name: 즉시 flush - ${updates.size}개 종목 업데이트 처리")
                updates.forEach { (code, data) ->
                    Log.d(TAG, "$name 즉시 업데이트: $code = ${data.price}")
                }
                
                // 즉시 업데이트 전달
                val emitResult = when (this@SmartUpdateScheduler.getScreenTypeForQueue(this@UpdateQueue)) {
                    ScreenType.CHART -> this@SmartUpdateScheduler._chartUpdates.tryEmit(updates)
                    ScreenType.STOCK_LIST -> this@SmartUpdateScheduler._stockListUpdates.tryEmit(updates)
                    ScreenType.PORTFOLIO -> this@SmartUpdateScheduler._portfolioUpdates.tryEmit(updates)
                    ScreenType.SUMMARY -> this@SmartUpdateScheduler._summaryUpdates.tryEmit(updates)
                    ScreenType.NEWS -> true
                }
                Log.d(TAG, "$name: 즉시 tryEmit 결과 - $emitResult")
            }
        }
        
        fun updateInterval(newInterval: Float) {
            intervalMs = newInterval
        }
        
        fun pause() {
            isPaused = true
        }
        
        fun resume() {
            isPaused = false
        }
        
        fun getStats(): String {
            return "$name: ${pendingUpdates.size}개 대기, ${intervalMs}ms 주기, 일시정지: $isPaused"
        }
    }
    
    fun getSchedulerStats(): String {
        return updateQueues.entries.joinToString("\n") { (screenType, queue) ->
            queue.getStats()
        }
    }
    
    private fun getScreenTypeForQueue(queue: UpdateQueue): ScreenType {
        return updateQueues.entries.first { it.value == queue }.key
    }
    
    fun cleanup() {
        scope.cancel()
    }
}