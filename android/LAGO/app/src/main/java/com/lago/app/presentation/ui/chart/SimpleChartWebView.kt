package com.lago.app.presentation.ui.chart

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.lago.app.presentation.ui.chart.v5.SimpleChartBridge

/**
 * LightWeight Charts v5.0.8 기준 간단한 차트 WebView
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleChartWebView(
    chartData: SimpleChartData,
    modifier: Modifier = Modifier,
    onChartReady: (SimpleChartBridge) -> Unit = {}
) {
    val context = LocalContext.current
    var chartBridge by remember { mutableStateOf<SimpleChartBridge?>(null) }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                
                // 브릿지 설정
                val bridge = SimpleChartBridge(this)
                chartBridge = bridge
                addJavascriptInterface(bridge, "SimpleChartBridge")
                
                // HTML 로딩
                loadUrl("file:///android_asset/simple_chart_v5.html")
                
                onChartReady(bridge)
            }
        },
        update = { webView ->
            // 차트 데이터 업데이트
            chartBridge?.let { bridge ->
                bridge.setChartData(
                    candles = chartData.candles,
                    volume = chartData.volume,
                    sma5 = chartData.sma5,
                    sma20 = chartData.sma20,
                    rsi = chartData.rsi
                )
            }
        }
    )
}

/**
 * 간단한 차트 데이터 구조
 */
data class SimpleChartData(
    val candles: List<SimpleChartBridge.CandleData> = emptyList(),
    val volume: List<SimpleChartBridge.VolumeData> = emptyList(),
    val sma5: List<SimpleChartBridge.LineData> = emptyList(),
    val sma20: List<SimpleChartBridge.LineData> = emptyList(),
    val rsi: List<SimpleChartBridge.LineData> = emptyList()
)