package com.lago.app.presentation.ui.chart.v5

import android.webkit.WebView
import android.webkit.JavascriptInterface
import org.json.JSONObject
import com.google.gson.Gson
import java.util.ArrayDeque

// 기존 호환성을 위한 원래 데이터 구조 유지
data class Candle(val time: Long, val open: Int, var high: Int, var low: Int, var close: Int)
data class VolumeBar(val time: Long, val value: Long)

// TradingView 차트 전용 데이터 구조 (새로 추가)
data class CandleData(val time: Long, val open: Float, val high: Float, val low: Float, val close: Float)
data class VolumeData(val time: Long, val value: Long, val color: String? = null)

// 패턴 분석 리스너 (새 방식)
interface PatternListener {
    fun onPatternVisibleRange(fromEpochSec: Long, toEpochSec: Long)
    fun onPatternVisibleRangeError(msg: String)
}

// 무한 히스토리 데이터 요청 리스너
interface HistoricalDataRequestListener {
    fun onRequestHistoricalData(barsToLoad: Int)
}

// 차트 로딩 완료 리스너
interface ChartLoadingListener {
    fun onChartLoadingCompleted()
    fun onChartReady()
    fun onLoadingProgress(progress: Int)
}

class JsBridge(
    private val webView: WebView,
    private val gson: Gson = Gson(),
    private val historicalDataListener: HistoricalDataRequestListener? = null,
    private val patternListener: PatternListener? = null,
    private val chartLoadingListener: ChartLoadingListener? = null
) {
    private val queue = ArrayDeque<String>()
    private var ready = false
    private var version: Long = 0
    
    // 🔥 순차적 로딩을 위한 상태 추적
    private var loadingProgress = 0
    private var loadingCompleted = false
    private var pendingIndicators = mutableListOf<Pair<String, Boolean>>()
    
    // 🔥 강화된 대기열 시스템
    private val pendingInitialData = mutableListOf<Pair<List<CandleData>, List<VolumeData>>>()
    private val pendingRealTimeUpdates = mutableListOf<Triple<CandleData, String, String>>() // CandleData, timeFrame, type
    private val pendingVolumeUpdates = mutableListOf<Triple<VolumeData, String, String>>()

    fun markReady() {
        ready = true
        version = System.currentTimeMillis()
        android.util.Log.d("JsBridge", "🔥 markReady() called - version: $version, queue size: ${queue.size}")
        
        // 기존 JS 명령어 처리
        while (queue.isNotEmpty()) {
            eval(queue.removeFirst())
        }
        
        // 🔥 대기 중인 초기 데이터 처리
        processPendingInitialData()
        
        // 🔥 대기 중인 실시간 업데이트 처리  
        processPendingRealTimeUpdates()
        
        // 🔥 대기 중인 거래량 업데이트 처리
        processPendingVolumeUpdates()
        
        // HTML에서 차트 준비 완료 상태 확인
        updateLoadingProgress(70)
        
        android.util.Log.d("JsBridge", "✅ markReady() 완료 - 모든 대기 데이터 처리됨")
    }
    
    // 🔥 순차적 로딩을 위한 새로운 메서드들
    private fun updateLoadingProgress(progress: Int) {
        loadingProgress = progress
        android.util.Log.d("JsBridge", "📊 Loading progress: $progress%")
    }
    
    fun setIndicatorWithQueue(type: String, enabled: Boolean) {
        android.util.Log.d("JsBridge", "📈 Indicator 설정 요청: $type = $enabled")
        pendingIndicators.add(type to enabled)
        
        if (ready && loadingProgress >= 80) {
            processPendingIndicators()
        }
    }
    
    private fun processPendingIndicators() {
        if (pendingIndicators.isNotEmpty()) {
            android.util.Log.d("JsBridge", "🔄 처리할 Indicator: ${pendingIndicators.size}개")
            pendingIndicators.forEach { (type, enabled) ->
                val jsCommand = "window.setIndicatorEnabled('$type', $enabled, null)"
                enqueueOrEval(jsCommand)
            }
            pendingIndicators.clear()
            updateLoadingProgress(95)
            
            // 모든 지표 처리 완료 시 로딩 완료
            if (!loadingCompleted) {
                loadingCompleted = true
                updateLoadingProgress(100)
                android.util.Log.d("JsBridge", "🎉 모든 차트 로딩 완료!")
            }
        }
    }

    /**
     * 차트 초기 데이터 설정 (TradingView 권장: series.setData())
     * ChartTimeManager를 사용하여 시간 정규화
     */
    fun setInitialData(candles: List<CandleData>, volumes: List<VolumeData> = emptyList()) {
        android.util.Log.d("JsBridge", "🔥 setInitialData 호출: ${candles.size}개 캔들, ${volumes.size}개 거래량")
        android.util.Log.d("JsBridge", "🔥 ready 상태: $ready")

        // 🔥 데이터 검증 및 정제
        val validatedCandles = validateAndCleanCandleData(candles)
        val validatedVolumes = validateAndCleanVolumeData(volumes, validatedCandles)
        
        android.util.Log.d("JsBridge", "🔍 데이터 검증 완료: 유효한 캔들 ${validatedCandles.size}개, 거래량 ${validatedVolumes.size}개")

        // ChartTimeManager를 사용하여 시간 정규화
        val normalizedCandles = validatedCandles.map { candle ->
            val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(candle.time)
            ChartTimeManager.debugTimeInfo("Initial Candle", candle.time)
            candle.copy(time = normalizedTime)
        }

        val normalizedVolumes = validatedVolumes.map { volume ->
            val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(volume.time)
            volume.copy(time = normalizedTime)
        }
        
        // 🔥 HTML이 준비되지 않은 경우 대기열에 추가
        if (!ready) {
            android.util.Log.w("JsBridge", "⚠️ HTML이 준비되지 않음 - 초기 데이터를 대기열에 추가")
            pendingInitialData.add(Pair(normalizedCandles, normalizedVolumes))
            return
        }

        if (normalizedCandles.isNotEmpty()) {
            android.util.Log.d("JsBridge", "🔥 첫 캔들(정규화): time=${normalizedCandles.first().time}, close=${normalizedCandles.first().close}")
            android.util.Log.d("JsBridge", "🔥 마지막 캔들(정규화): time=${normalizedCandles.last().time}, close=${normalizedCandles.last().close}")
        }

        val candlesJson = gson.toJson(normalizedCandles)
        val volumesJson = gson.toJson(normalizedVolumes)

        android.util.Log.d("JsBridge", "🔥 JSON 변환 완료 - 캔들 JSON 길이: ${candlesJson.length}, 거래량 JSON 길이: ${volumesJson.length}")

        val jsCommand = """window.setInitialData(${candlesJson.quote()}, ${volumesJson.quote()})"""
        enqueueOrEval(jsCommand)
        
        // 🔥 데이터 로딩 완료 후 진행률 업데이트
        updateLoadingProgress(80)
        
        // 대기 중인 지표가 있으면 처리
        if (pendingIndicators.isNotEmpty()) {
            processPendingIndicators()
        }
    }

    /**
     * 실시간 데이터 업데이트 (TradingView 권장: series.update())
     * 동일 time = 현재 바 덮어쓰기, 새로운 time = 새 바 추가
     * ChartTimeManager를 사용하여 시간 정규화
     */
    fun updateRealTimeBar(bar: CandleData, timeFrame: String) {
        val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(bar.time)
        android.util.Log.d("JsBridge", "🔥 updateRealTimeBar 호출 - time: ${bar.time} → $normalizedTime, close: ${bar.close}, timeFrame: $timeFrame")

        ChartTimeManager.debugTimeInfo("RealTime Bar", bar.time)

        val normalizedBar = bar.copy(time = normalizedTime)
        
        // 🔥 HTML이 준비되지 않은 경우 대기열에 추가
        if (!ready) {
            android.util.Log.w("JsBridge", "⚠️ HTML이 준비되지 않음 - 실시간 캔들 데이터를 대기열에 추가")
            pendingRealTimeUpdates.add(Triple(normalizedBar, timeFrame, "candle"))
            return
        }
        
        val barJson = gson.toJson(normalizedBar)
        enqueueOrEval("""window.updateRealTimeBar(${barJson.quote()})""")
        
        android.util.Log.d("JsBridge", "✅ updateRealTimeBar 완료 - 차트에 반영됨")
    }

    fun updateRealTimeVolume(vol: VolumeData, timeFrame: String) {
        val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(vol.time)
        android.util.Log.d("JsBridge", "🔥 updateRealTimeVolume 호출 - time: ${vol.time} → $normalizedTime, value: ${vol.value}, timeFrame: $timeFrame")

        val normalizedVolume = vol.copy(time = normalizedTime)
        
        // 🔥 HTML이 준비되지 않은 경우 대기열에 추가
        if (!ready) {
            android.util.Log.w("JsBridge", "⚠️ HTML이 준비되지 않음 - 실시간 거래량 데이터를 대기열에 추가")
            pendingVolumeUpdates.add(Triple(normalizedVolume, timeFrame, "volume"))
            return
        }
        
        val volJson = gson.toJson(normalizedVolume)
        enqueueOrEval("""window.updateRealTimeVolume(${volJson.quote()})""")
        
        android.util.Log.d("JsBridge", "✅ updateRealTimeVolume 완료 - 거래량 차트에 반영됨")
    }

    // 기존 호환성을 위한 래퍼 메서드들 (ChartTimeManager 적용)
    fun setLegacyInitialData(candles: List<Candle>, volumes: List<VolumeBar> = emptyList()) {
        val convertedCandles = candles.map { candle ->
            val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(candle.time)
            CandleData(normalizedTime, candle.open.toFloat(), candle.high.toFloat(), candle.low.toFloat(), candle.close.toFloat())
        }
        val convertedVolumes = volumes.map { volume ->
            val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(volume.time)
            VolumeData(normalizedTime, volume.value)
        }
        setInitialData(convertedCandles, convertedVolumes)
    }

    fun updateBar(bar: Candle, timeFrame: String = "D") {
        val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(bar.time)
        android.util.Log.d("JsBridge", "🔥 updateBar 호출 - time: ${bar.time} → $normalizedTime, close: ${bar.close}, timeFrame: $timeFrame")

        val candleData = CandleData(normalizedTime, bar.open.toFloat(), bar.high.toFloat(), bar.low.toFloat(), bar.close.toFloat())
        updateRealTimeBar(candleData, timeFrame)
        
        android.util.Log.d("JsBridge", "✅ updateBar 완료 - 차트에 반영됨")
    }

    fun updateVolume(vol: VolumeBar, timeFrame: String = "D") {
        val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(vol.time)
        android.util.Log.d("JsBridge", "🔥 updateVolume 호출 - time: ${vol.time} → $normalizedTime, value: ${vol.value}, timeFrame: $timeFrame")

        // 🔥 현재 캔들 데이터에서 색상 결정
        val volumeColor = try {
            if (ready) {
                // HTML에서 현재 캔들 데이터를 확인하여 색상 결정
                "#26a69a" // 기본 상승 색상 (추후 캔들 데이터와 연동)
            } else {
                "#26a69a" // 기본 색상
            }
        } catch (e: Exception) {
            android.util.Log.w("JsBridge", "색상 결정 중 오류: ${e.message}")
            "#26a69a" // 기본 색상
        }

        // 🔥 거래량 색상을 추가하여 VolumeData 생성
        val volumeData = VolumeData(
            time = normalizedTime, 
            value = vol.value,
            color = volumeColor
        )
        updateRealTimeVolume(volumeData, timeFrame)
        
        android.util.Log.d("JsBridge", "✅ updateVolume 완료 - 거래량: ${vol.value}, 색상: $volumeColor, 차트에 반영됨")
    }

    fun updateSymbolName(symbolName: String) {
        enqueueOrEval("""window.updateSymbolName('${symbolName.replace("'", "\\'")}')""")
    }

    fun setTradeMarkers(markersJson: String) {
        val escapedJson = markersJson.replace("'", "\\'").replace("\"", "\\\"")
        enqueueOrEval("""window.setTradeMarkers('$escapedJson')""")
    }

    fun clearTradeMarkers() {
        enqueueOrEval("""window.clearTradeMarkers()""")
    }

    // 메인 패널 오버레이 지표 동적 업데이트
    fun updateSMA5(data: List<ChartData>) {
        val normalizedData = data.map { item ->
            item.copy(time = ChartTimeManager.normalizeToEpochSeconds(item.time))
        }
        val json = if (normalizedData.isEmpty()) "[]" else gson.toJson(normalizedData)
        enqueueOrEval("""window.seriesMap.sma5?.setData($json)""")
    }

    fun updateSMA20(data: List<ChartData>) {
        val normalizedData = data.map { item ->
            item.copy(time = ChartTimeManager.normalizeToEpochSeconds(item.time))
        }
        val json = if (normalizedData.isEmpty()) "[]" else gson.toJson(normalizedData)
        enqueueOrEval("""window.seriesMap.sma20?.setData($json)""")
    }

    fun updateBollingerBands(upperBand: List<ChartData>, middleBand: List<ChartData>, lowerBand: List<ChartData>) {
        val normalizedUpper = upperBand.map { item ->
            item.copy(time = ChartTimeManager.normalizeToEpochSeconds(item.time))
        }
        val normalizedMiddle = middleBand.map { item ->
            item.copy(time = ChartTimeManager.normalizeToEpochSeconds(item.time))
        }
        val normalizedLower = lowerBand.map { item ->
            item.copy(time = ChartTimeManager.normalizeToEpochSeconds(item.time))
        }

        val upperJson = if (normalizedUpper.isEmpty()) "[]" else gson.toJson(normalizedUpper)
        val middleJson = if (normalizedMiddle.isEmpty()) "[]" else gson.toJson(normalizedMiddle)
        val lowerJson = if (normalizedLower.isEmpty()) "[]" else gson.toJson(normalizedLower)

        enqueueOrEval("""
            window.seriesMap.bb_upper?.setData($upperJson);
            window.seriesMap.bb_middle?.setData($middleJson);
            window.seriesMap.bb_lower?.setData($lowerJson);
        """.trimIndent())
    }

    // 무한 히스토리 관련 메서드들
    @JavascriptInterface
    fun requestHistoricalData(barsToLoad: Int) {
        historicalDataListener?.onRequestHistoricalData(barsToLoad)
    }

    // ===== 패턴 분석 관련 메서드들 =====

    interface PatternAnalysisListener {
        fun onAnalyzePatternInRange(fromTime: String, toTime: String)
        fun onPatternAnalysisError(message: String)
        fun onPatternAnalysisComplete(patternName: String, description: String)
    }

    private var patternAnalysisListener: PatternAnalysisListener? = null

    fun setPatternAnalysisListener(listener: PatternAnalysisListener?) {
        this.patternAnalysisListener = listener
    }

    @JavascriptInterface
    fun analyzePatternInRange(fromTime: String, toTime: String) {
        android.util.Log.d("JsBridge", "📊 패턴 분석 요청: $fromTime ~ $toTime")
        patternAnalysisListener?.onAnalyzePatternInRange(fromTime, toTime)
    }

    @JavascriptInterface
    fun onPatternAnalysisError(message: String) {
        android.util.Log.w("JsBridge", "📊 패턴 분석 에러: $message")
        patternAnalysisListener?.onPatternAnalysisError(message)
    }

    @JavascriptInterface
    fun onPatternAnalysisComplete(patternName: String, description: String) {
        android.util.Log.d("JsBridge", "📊 패턴 분석 완료: $patternName - $description")
        patternAnalysisListener?.onPatternAnalysisComplete(patternName, description)
    }

    /**
     * 차트에서 보이는 영역의 시간 범위를 요청하는 개선된 메서드
     * ChartTimeManager를 사용하여 시간 처리 통일화
     */
    fun requestVisibleRange(callback: (fromTime: String?, toTime: String?) -> Unit) {
        val jsCommand = """
            (function() {
                try {
                    const range = window.getVisibleRange();
                    if (range && range.from && range.to) {
                        return JSON.stringify(range);
                    } else {
                        return null;
                    }
                } catch (error) {
                    console.error('getVisibleRange 실패:', error);
                    return null;
                }
            })();
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(jsCommand) { result ->
                try {
                    if (result != null && result != "null" && result.startsWith("{")) {
                        val cleanResult = result.replace("\"", "")
                        val jsonObject = org.json.JSONObject(cleanResult)
                        val fromTime = jsonObject.optString("from")
                        val toTime = jsonObject.optString("to")

                        if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                            // ChartTimeManager를 사용하여 시간 검증
                            try {
                                val fromEpoch = ChartTimeManager.normalizeToEpochSeconds(fromTime.toLong())
                                val toEpoch = ChartTimeManager.normalizeToEpochSeconds(toTime.toLong())
                                ChartTimeManager.debugTimeInfo("Visible Range From", fromEpoch)
                                ChartTimeManager.debugTimeInfo("Visible Range To", toEpoch)
                                callback(fromEpoch.toString(), toEpoch.toString())
                            } catch (e: NumberFormatException) {
                                android.util.Log.e("JsBridge", "시간 파싱 실패: $fromTime, $toTime", e)
                                callback(null, null)
                            }
                        } else {
                            callback(null, null)
                        }
                    } else {
                        callback(null, null)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("JsBridge", "보이는 영역 파싱 실패", e)
                    callback(null, null)
                }
            }
        }
    }

    fun triggerPatternAnalysis() {
        enqueueOrEval("window.requestPatternAnalysis()")
    }

    /**
     * 차트의 보이는 영역에서 패턴 분석을 실행 (개선된 버전)
     */
    fun analyzePatternInVisibleRange() {
        android.util.Log.d("JsBridge", "analyzePatternInVisibleRange() called")
        android.util.Log.d("JsBridge", "ready state: $ready")

        val jsFunction = """
            (function() {
                try {
                    console.log('LAGO: getVisibleTimeRange 시작');
                    var c = window.lightweightChart || window.chart; 
                    if (!c || !c.timeScale) {
                        console.log('LAGO: 차트 또는 timeScale이 없음');
                        return null;
                    }
                    var r = c.timeScale().getVisibleTimeRange();
                    if (!r || !r.from || !r.to) {
                        console.log('LAGO: visible range가 없음');
                        return null;
                    }
                    console.log('LAGO: visible range found:', r.from, r.to);
                    // 안드로이드 콜백 호출
                    if (window.ChartBridge && window.ChartBridge.onPatternVisibleRange) {
                        window.ChartBridge.onPatternVisibleRange(r.from.toString(), r.to.toString());
                    }
                    return [r.from, r.to];
                } catch(e) { 
                    console.error('LAGO: getVisibleTimeRange 에러:', e);
                    if (window.ChartBridge && window.ChartBridge.onPatternVisibleRangeError) {
                        window.ChartBridge.onPatternVisibleRangeError('JavaScript 에러: ' + e.message);
                    }
                    return null; 
                }
            })();
        """.trimIndent()

        enqueueOrEval(jsFunction)
    }

    @JavascriptInterface
    fun onPatternVisibleRange(fromTime: String, toTime: String) {
        android.util.Log.d("JsBridge", "onPatternVisibleRange callback: $fromTime ~ $toTime")
        try {
            val fromEpoch = ChartTimeManager.normalizeToEpochSeconds(fromTime.toLong())
            val toEpoch = ChartTimeManager.normalizeToEpochSeconds(toTime.toLong())

            ChartTimeManager.debugTimeInfo("Pattern Analysis From", fromEpoch)
            ChartTimeManager.debugTimeInfo("Pattern Analysis To", toEpoch)

            patternListener?.onPatternVisibleRange(fromEpoch, toEpoch)
        } catch (e: Exception) {
            android.util.Log.e("JsBridge", "시간 파싱 에러", e)
            patternListener?.onPatternVisibleRangeError("시간 파싱 에러: ${e.message}")
        }
    }

    @JavascriptInterface
    fun onPatternVisibleRangeError(message: String) {
        android.util.Log.w("JsBridge", "onPatternVisibleRangeError: $message")
        patternListener?.onPatternVisibleRangeError(message)
    }

    @JavascriptInterface
    fun onVisibleRangeAnalysis(fromTime: String, toTime: String) {
        android.util.Log.d("JsBridge", "📊 [4단계] onVisibleRangeAnalysis 콜백 진입: $fromTime ~ $toTime")

        patternAnalysisListener?.let { listener ->
            android.util.Log.d("JsBridge", "📊 [4단계] listener 존재 - onAnalyzePatternInRange 호출")
            listener.onAnalyzePatternInRange(fromTime, toTime)
        } ?: run {
            android.util.Log.w("JsBridge", "📊 [4단계] patternAnalysisListener가 null - 분석 진행 불가")
        }
    }

    // 🔥 차트 로딩 상태 관련 콜백 메서드들
    @JavascriptInterface
    fun onChartLoadingCompleted() {
        android.util.Log.d("JsBridge", "🎉 차트 로딩 완료 콜백 수신")
        chartLoadingListener?.onChartLoadingCompleted()
    }
    
    @JavascriptInterface
    fun onChartReady() {
        android.util.Log.d("JsBridge", "📊 차트 준비 완료 콜백 수신")
        chartLoadingListener?.onChartReady()
    }
    
    @JavascriptInterface
    fun onLoadingProgress(progress: Int) {
        android.util.Log.d("JsBridge", "📈 로딩 진행률: $progress%")
        chartLoadingListener?.onLoadingProgress(progress)
    }

    fun displayPatternResult(result: com.lago.app.domain.entity.PatternAnalysisResult) {
        val resultJson = gson.toJson(mapOf(
            "stockCode" to result.stockCode,
            "patterns" to result.patterns,
            "analysisTime" to result.analysisTime,
            "chartMode" to result.chartMode,
            "timeFrame" to result.timeFrame
        ))

        enqueueOrEval("window.displayPatternResult && window.displayPatternResult(${resultJson.quote()})")
    }

    /**
     * 무한 히스토리 데이터 추가 (개선된 버전)
     * ChartTimeManager를 사용하여 시간 정규화
     */
    fun prependHistoricalData(historicalCandles: List<CandleData>, historicalVolumes: List<VolumeData> = emptyList()) {
        val normalizedCandles = historicalCandles.map { candle ->
            val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(candle.time)
            candle.copy(time = normalizedTime)
        }

        val normalizedVolumes = historicalVolumes.map { volume ->
            val normalizedTime = ChartTimeManager.normalizeToEpochSeconds(volume.time)
            volume.copy(time = normalizedTime)
        }

        val candlesJson = gson.toJson(normalizedCandles)
        val volumesJson = gson.toJson(normalizedVolumes)
        enqueueOrEval("""window.prependHistoricalData(${candlesJson.quote()}, ${volumesJson.quote()})""")
    }

    /**
     * 🔥 실시간 스크롤 기능 - 차트를 최신 시간으로 자동 스크롤
     * TradingView 예제와 동일한 방식으로 구현
     */
    fun scrollToRealTime() {
        android.util.Log.d("JsBridge", "📊 scrollToRealTime 호출 - 차트를 최신 시간으로 스크롤")
        
        val jsCommand = """
            (function() {
                try {
                    var chart = window.__chart || window.chart || window.lightweightChart;
                    if (chart && chart.timeScale) {
                        chart.timeScale().scrollToRealTime();
                        console.log('LAGO: scrollToRealTime 실행 완료');
                        return true;
                    } else {
                        console.warn('LAGO: 차트 또는 timeScale이 없음');
                        return false;
                    }
                } catch(e) {
                    console.error('LAGO: scrollToRealTime 에러:', e);
                    return false;
                }
            })();
        """.trimIndent()
        
        enqueueOrEval(jsCommand)
        android.util.Log.d("JsBridge", "✅ scrollToRealTime JavaScript 명령 전송 완료")
    }

    @Deprecated("Use prependHistoricalData instead")
    fun addHistoricalData(historicalDataJson: String) {
        val escapedJson = historicalDataJson.replace("'", "\\'").replace("\"", "\\\"")
        enqueueOrEval("""window.addHistoricalData('$escapedJson')""")
    }

    /**
     * JS 실행 전 ready 상태 확인 및 큐 관리
     */
    private fun enqueueOrEval(script: String) {
        if (!ready) {
            queue.addLast(script)
            android.util.Log.d("JsBridge", "큐에 저장: ${script.take(50)}... (큐 크기: ${queue.size})")
            return
        }
        eval(script)
    }

    private fun eval(script: String) {
        android.util.Log.v("JsBridge", "JS 실행: ${script.take(100)}...")
        webView.post { webView.evaluateJavascript(script, null) }
    }

    private fun String.quote(): String = JSONObject.quote(this)

    // 🔥 대기 중인 초기 데이터 처리
    private fun processPendingInitialData() {
        if (pendingInitialData.isNotEmpty()) {
            android.util.Log.d("JsBridge", "🔄 대기 중인 초기 데이터 처리: ${pendingInitialData.size}개")
            val dataItem = pendingInitialData.removeFirst()
            setInitialData(dataItem.first, dataItem.second)
        }
    }
    
    // 🔥 대기 중인 실시간 업데이트 처리
    private fun processPendingRealTimeUpdates() {
        while (pendingRealTimeUpdates.isNotEmpty()) {
            val updateItem = pendingRealTimeUpdates.removeFirst()
            val (candleData, timeFrame, type) = updateItem
            
            android.util.Log.d("JsBridge", "🔄 대기 중인 실시간 캔들 처리: time=${candleData.time}, close=${candleData.close}")
            
            val barJson = gson.toJson(candleData)
            enqueueOrEval("""window.updateRealTimeBar(${barJson.quote()})""")
        }
        android.util.Log.d("JsBridge", "✅ 모든 대기 중인 실시간 캔들 처리 완료")
    }
    
    // 🔥 대기 중인 거래량 업데이트 처리
    private fun processPendingVolumeUpdates() {
        while (pendingVolumeUpdates.isNotEmpty()) {
            val updateItem = pendingVolumeUpdates.removeFirst()
            val (volumeData, timeFrame, type) = updateItem
            
            android.util.Log.d("JsBridge", "🔄 대기 중인 실시간 거래량 처리: time=${volumeData.time}, value=${volumeData.value}")
            
            val volJson = gson.toJson(volumeData)
            enqueueOrEval("""window.updateRealTimeVolume(${volJson.quote()})""")
        }
        android.util.Log.d("JsBridge", "✅ 모든 대기 중인 실시간 거래량 처리 완료")
    }

    companion object {
        /**
         * TradingView 차트 시간 포맷 변환 유틸리티 (Deprecated)
         * ChartTimeManager 사용 권장
         */
        @Deprecated("Use ChartTimeManager instead")
        object TimeUtils {
            fun formatDailyTime(unixTimestamp: Long): String {
                return ChartTimeManager.formatForDisplay(
                    ChartTimeManager.normalizeToEpochSeconds(unixTimestamp),
                    false
                )
            }

            fun formatIntraTime(unixMillis: Long): Long {
                return ChartTimeManager.normalizeToEpochSeconds(unixMillis)
            }

            fun formatTimeForChart(timestamp: Long, timeFrame: String): Any {
                val normalized = ChartTimeManager.normalizeToEpochSeconds(timestamp)
                return if (ChartTimeManager.isDailyOrAbove(timeFrame)) {
                    ChartTimeManager.formatForDisplay(normalized, false)
                } else {
                    normalized
                }
            }
        }
    }
    
    /**
     * 🔥 캔들 데이터 검증 및 정제
     */
    private fun validateAndCleanCandleData(candleData: List<CandleData>): List<CandleData> {
        return candleData.filter { candle ->
            // 유효한 가격 데이터 확인
            candle.open > 0 && candle.high > 0 && candle.low > 0 && candle.close > 0 &&
            candle.high >= candle.low && 
            candle.high >= candle.open && candle.high >= candle.close &&
            candle.low <= candle.open && candle.low <= candle.close
        }.sortedBy { it.time } // 시간순 정렬
    }
    
    /**
     * 🔥 거래량 데이터 검증 및 정제
     */
    private fun validateAndCleanVolumeData(volumeData: List<VolumeData>, candleData: List<CandleData>): List<VolumeData> {
        val candleTimeSet = candleData.map { it.time }.toSet()
        
        // 캔들 데이터와 매칭되는 거래량만 유지
        val matchingVolumeData = volumeData.filter { volume ->
            candleTimeSet.contains(volume.time) && volume.value >= 0
        }
        
        // 거래량 데이터가 부족한 경우 기본값 생성
        if (matchingVolumeData.size < candleData.size * 0.5) {  // 임계값을 0.5로 낮춤
            android.util.Log.w("JsBridge", "⚠️ 거래량 데이터 부족 (${matchingVolumeData.size}/${candleData.size}) - 기본값 생성")
            return candleData.map { candle ->
                val existingVolume = matchingVolumeData.find { it.time == candle.time }
                existingVolume ?: VolumeData(
                    time = candle.time,
                    value = kotlin.math.max(10000, (candle.close * 100).toLong()), // 가격 기반 기본 거래량
                    color = if (candle.close >= candle.open) "#26a69a" else "#ef5350"
                )
            }
        } else {
            android.util.Log.d("JsBridge", "✅ 거래량 데이터 충분함 (${matchingVolumeData.size}/${candleData.size})")
        }
        
        return matchingVolumeData.sortedBy { it.time }
    }
}
