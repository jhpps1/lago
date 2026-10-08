package com.lago.app.presentation.ui.chart.v5

import android.webkit.WebView
import android.webkit.JavascriptInterface
import com.google.gson.Gson

/**
 * LightWeight Charts v5 네이티브 API 직접 사용하는 간단한 브릿지
 * 복잡한 큐나 래퍼 없이 setData 직접 호출
 */
class NativeChartBridge(
    private val webView: WebView,
    private val gson: Gson = Gson()
) {
    private var isChartReady = false
    private var pendingActions = mutableListOf<() -> Unit>()
    
    @JavascriptInterface
    fun onChartReady() {
        android.util.Log.d("NativeChartBridge", "✅ 차트 준비 완료")
        isChartReady = true
        
        // 대기 중인 액션들 실행
        pendingActions.forEach { it() }
        pendingActions.clear()
    }
    
    /**
     * 초기 차트 데이터 설정 - v5 네이티브 setData 사용
     */
    fun setInitialData(
        candles: List<CandleData>,
        volumes: List<VolumeData> = emptyList(),
        sma5: List<LineData> = emptyList(),
        sma20: List<LineData> = emptyList(),
        rsi: List<LineData> = emptyList()
    ) {
        android.util.Log.d("NativeChartBridge", "🚀 초기 데이터 설정: 캔들=${candles.size}, 거래량=${volumes.size}")
        
        executeWhenReady {
            // 1. 캔들 데이터 (필수)
            if (candles.isNotEmpty()) {
                val candlesJson = gson.toJson(candles)
                executeJS("window.setCandleData($candlesJson);")
            }
            
            // 2. 거래량 데이터
            if (volumes.isNotEmpty()) {
                val volumesJson = gson.toJson(volumes)
                executeJS("window.setVolumeData($volumesJson);")
            }
            
            // 3. SMA5 데이터
            if (sma5.isNotEmpty()) {
                val sma5Json = gson.toJson(sma5)
                executeJS("window.setSMA5Data($sma5Json);")
            }
            
            // 4. SMA20 데이터
            if (sma20.isNotEmpty()) {
                val sma20Json = gson.toJson(sma20)
                executeJS("window.setSMA20Data($sma20Json);")
            }
            
            // 5. RSI 데이터 (별도 패널)
            if (rsi.isNotEmpty()) {
                val rsiJson = gson.toJson(rsi)
                executeJS("window.setRSIData($rsiJson);")
            }
        }
    }
    
    /**
     * 개별 데이터 설정
     */
    fun setCandleData(candles: List<CandleData>) {
        executeWhenReady {
            val candlesJson = gson.toJson(candles)
            executeJS("window.setCandleData($candlesJson);")
        }
    }
    
    fun setVolumeData(volumes: List<VolumeData>) {
        executeWhenReady {
            val volumesJson = gson.toJson(volumes)
            executeJS("window.setVolumeData($volumesJson);")
        }
    }
    
    fun setSMA5Data(sma5: List<LineData>) {
        executeWhenReady {
            val sma5Json = gson.toJson(sma5)
            executeJS("window.setSMA5Data($sma5Json);")
        }
    }
    
    fun setSMA20Data(sma20: List<LineData>) {
        executeWhenReady {
            val sma20Json = gson.toJson(sma20)
            executeJS("window.setSMA20Data($sma20Json);")
        }
    }
    
    fun setRSIData(rsi: List<LineData>) {
        executeWhenReady {
            val rsiJson = gson.toJson(rsi)
            executeJS("window.setRSIData($rsiJson);")
        }
    }
    
    /**
     * 실시간 업데이트
     */
    fun updateCandle(candle: CandleData) {
        executeWhenReady {
            val candleJson = gson.toJson(candle)
            executeJS("window.updateCandle($candleJson);")
        }
    }
    
    fun updateVolume(volume: VolumeData) {
        executeWhenReady {
            val volumeJson = gson.toJson(volume)
            executeJS("window.updateVolume($volumeJson);")
        }
    }
    
    /**
     * 지표 토글
     */
    fun toggleSMA5(visible: Boolean) {
        executeWhenReady {
            executeJS("window.toggleSMA5($visible);")
        }
    }
    
    fun toggleSMA20(visible: Boolean) {
        executeWhenReady {
            executeJS("window.toggleSMA20($visible);")
        }
    }
    
    fun toggleVolume(visible: Boolean) {
        executeWhenReady {
            executeJS("window.toggleVolume($visible);")
        }
    }
    
    fun toggleRSI(visible: Boolean) {
        executeWhenReady {
            executeJS("window.toggleRSI($visible);")
        }
    }
    
    /**
     * 차트 준비 상태 확인 후 실행
     */
    private fun executeWhenReady(action: () -> Unit) {
        if (isChartReady) {
            action()
        } else {
            pendingActions.add(action)
        }
    }
    
    /**
     * JavaScript 실행
     */
    private fun executeJS(script: String) {
        webView.post {
            webView.evaluateJavascript(script) { result ->
                if (result?.contains("error", ignoreCase = true) == true) {
                    android.util.Log.w("NativeChartBridge", "JS 결과: $result")
                }
            }
        }
    }
    
    // v5 호환 데이터 구조 (UTCTimestamp 초 단위)
    data class CandleData(
        val time: Long, // UTCTimestamp 초 단위
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double
    )
    
    data class VolumeData(
        val time: Long, // UTCTimestamp 초 단위
        val value: Long,
        val color: String = "#26a69a"
    )
    
    data class LineData(
        val time: Long, // UTCTimestamp 초 단위
        val value: Double
    )
}