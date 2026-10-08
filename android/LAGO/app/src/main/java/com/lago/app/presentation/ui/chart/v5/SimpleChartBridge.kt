package com.lago.app.presentation.ui.chart.v5

import android.webkit.WebView
import android.webkit.JavascriptInterface
import com.google.gson.Gson

/**
 * LightWeight Charts v5.0.8 기준 간단하고 직관적인 차트 브릿지
 * 복잡한 큐 시스템 없이 간단한 상태 관리
 */
class SimpleChartBridge(
    private val webView: WebView,
    private val gson: Gson = Gson()
) {
    private var isChartReady = false
    private var pendingData: ChartInitialData? = null
    
    // 차트 초기 데이터 구조
    data class ChartInitialData(
        val candles: List<CandleData>,
        val volume: List<VolumeData>,
        val indicators: Map<String, Any> = emptyMap()
    )
    
    // v5.0.8 호환 데이터 구조
    data class CandleData(val time: Long, val open: Double, val high: Double, val low: Double, val close: Double)
    data class VolumeData(val time: Long, val value: Long, val color: String = "#26a69a")
    data class LineData(val time: Long, val value: Double)
    
    /**
     * 차트 준비 완료 시 호출 (HTML에서)
     */
    @JavascriptInterface
    fun onChartReady() {
        android.util.Log.d("SimpleChartBridge", "✅ 차트 준비 완료")
        isChartReady = true
        
        // 대기 중인 데이터가 있으면 즉시 로딩
        pendingData?.let { data ->
            loadChartData(data)
            pendingData = null
        }
    }
    
    /**
     * 차트 데이터 설정 (메인 진입점)
     */
    fun setChartData(
        candles: List<CandleData>,
        volume: List<VolumeData> = emptyList(),
        sma5: List<LineData> = emptyList(),
        sma20: List<LineData> = emptyList(),
        rsi: List<LineData> = emptyList()
    ) {
        val chartData = ChartInitialData(
            candles = candles,
            volume = volume,
            indicators = mapOf(
                "sma5" to sma5,
                "sma20" to sma20,
                "rsi" to rsi
            )
        )
        
        if (isChartReady) {
            loadChartData(chartData)
        } else {
            android.util.Log.d("SimpleChartBridge", "⏳ 차트 준비 중 - 데이터 대기")
            pendingData = chartData
        }
    }
    
    /**
     * 실제 차트에 데이터 로딩
     */
    private fun loadChartData(data: ChartInitialData) {
        android.util.Log.d("SimpleChartBridge", "🚀 차트 데이터 로딩: 캔들=${data.candles.size}, 거래량=${data.volume.size}")
        
        // 1. 메인 캔들스틱 데이터
        val candlesJson = gson.toJson(data.candles)
        executeJS("window.setMainData($candlesJson);")
        
        // 2. 거래량 데이터
        if (data.volume.isNotEmpty()) {
            val volumeJson = gson.toJson(data.volume)
            executeJS("window.setVolumeData($volumeJson);")
        }
        
        // 3. 지표 데이터
        data.indicators.forEach { (indicator, lineData) ->
            if (lineData is List<*> && lineData.isNotEmpty()) {
                val indicatorJson = gson.toJson(lineData)
                executeJS("window.setIndicatorData('$indicator', $indicatorJson);")
            }
        }
        
        android.util.Log.d("SimpleChartBridge", "✅ 모든 데이터 로딩 완료")
    }
    
    /**
     * 실시간 데이터 업데이트
     */
    fun updateRealTime(candle: CandleData, volume: VolumeData? = null) {
        if (!isChartReady) return
        
        val candleJson = gson.toJson(candle)
        executeJS("window.updateCandle($candleJson);")
        
        volume?.let {
            val volumeJson = gson.toJson(it)
            executeJS("window.updateVolume($volumeJson);")
        }
    }
    
    /**
     * 지표 토글
     */
    fun toggleIndicator(indicator: String, enabled: Boolean) {
        if (!isChartReady) return
        executeJS("window.toggleIndicator('$indicator', $enabled);")
    }
    
    /**
     * JavaScript 실행
     */
    private fun executeJS(script: String) {
        webView.post {
            webView.evaluateJavascript(script) { result ->
                if (result?.contains("error", ignoreCase = true) == true) {
                    android.util.Log.w("SimpleChartBridge", "JS 실행 결과: $result")
                }
            }
        }
    }
}