package com.lago.app.presentation.ui.chart

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lago.app.presentation.ui.chart.v5.SimpleChartBridge

/**
 * 간단한 차트 테스트 화면
 * LightWeight Charts v5.0.8 기준으로 최적화된 차트 시스템 테스트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSimpleChartScreen() {
    var chartBridge by remember { mutableStateOf<SimpleChartBridge?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 제목
        Text(
            text = "Simple Chart v5.0.8 Test",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 테스트 버튼들
        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { loadSampleData(chartBridge) }
            ) {
                Text("샘플 데이터")
            }
            
            Button(
                onClick = { addIndicators(chartBridge) }
            ) {
                Text("지표 추가")
            }
            
            Button(
                onClick = { updateRealTime(chartBridge) }
            ) {
                Text("실시간 업데이트")
            }
        }
        
        // 차트 영역
        SimpleChartWebView(
            chartData = SimpleChartData(), // 초기에는 빈 데이터
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onChartReady = { bridge ->
                chartBridge = bridge
                android.util.Log.d("TestChart", "✅ 차트 준비 완료")
            }
        )
    }
}

/**
 * 샘플 데이터 로딩
 */
private fun loadSampleData(bridge: SimpleChartBridge?) {
    android.util.Log.d("TestChart", "🔄 샘플 데이터 로딩")
    
    bridge?.let {
        // 샘플 캔들 데이터 (최근 30일)
        val baseTime = System.currentTimeMillis() / 1000 - (30 * 24 * 60 * 60) // 30일 전
        val candles = (0..29).map { day ->
            val time = baseTime + (day * 24 * 60 * 60)
            val open = 50000.0 + (Math.random() * 10000)
            val close = open + (Math.random() - 0.5) * 2000
            val high = Math.max(open, close) + (Math.random() * 1000)
            val low = Math.min(open, close) - (Math.random() * 1000)
            
            SimpleChartBridge.CandleData(
                time = time,
                open = open,
                high = high,
                low = low,
                close = close
            )
        }
        
        // 샘플 거래량 데이터
        val volumes = candles.map { candle ->
            SimpleChartBridge.VolumeData(
                time = candle.time,
                value = (Math.random() * 1000000).toLong(),
                color = if (candle.close > candle.open) "#26a69a" else "#ef5350"
            )
        }
        
        it.setChartData(candles = candles, volume = volumes)
        android.util.Log.d("TestChart", "✅ 샘플 데이터 로딩 완료: ${candles.size}개")
    }
}

/**
 * 지표 추가
 */
private fun addIndicators(bridge: SimpleChartBridge?) {
    android.util.Log.d("TestChart", "🔄 지표 추가")
    
    bridge?.let {
        // SMA5, SMA20 샘플 데이터
        val baseTime = System.currentTimeMillis() / 1000 - (30 * 24 * 60 * 60)
        
        val sma5 = (0..29).map { day ->
            SimpleChartBridge.LineData(
                time = baseTime + (day * 24 * 60 * 60),
                value = 50000.0 + (Math.random() * 5000)
            )
        }
        
        val sma20 = (0..29).map { day ->
            SimpleChartBridge.LineData(
                time = baseTime + (day * 24 * 60 * 60),
                value = 51000.0 + (Math.random() * 4000)
            )
        }
        
        val rsi = (0..29).map { day ->
            SimpleChartBridge.LineData(
                time = baseTime + (day * 24 * 60 * 60),
                value = 30.0 + (Math.random() * 40) // RSI 30-70 범위
            )
        }
        
        it.setChartData(
            candles = emptyList(), // 기존 캔들은 그대로
            volume = emptyList(),  // 기존 거래량도 그대로
            sma5 = sma5,
            sma20 = sma20,
            rsi = rsi
        )
        
        android.util.Log.d("TestChart", "✅ 지표 추가 완료")
    }
}

/**
 * 실시간 업데이트 테스트
 */
private fun updateRealTime(bridge: SimpleChartBridge?) {
    android.util.Log.d("TestChart", "🔄 실시간 업데이트")
    
    bridge?.let {
        val currentTime = System.currentTimeMillis() / 1000
        val open = 52000.0
        val close = open + (Math.random() - 0.5) * 1000
        val high = Math.max(open, close) + (Math.random() * 500)
        val low = Math.min(open, close) - (Math.random() * 500)
        
        val newCandle = SimpleChartBridge.CandleData(
            time = currentTime,
            open = open,
            high = high,
            low = low,
            close = close
        )
        
        val newVolume = SimpleChartBridge.VolumeData(
            time = currentTime,
            value = (Math.random() * 1000000).toLong(),
            color = if (close > open) "#26a69a" else "#ef5350"
        )
        
        it.updateRealTime(newCandle, newVolume)
        android.util.Log.d("TestChart", "✅ 실시간 업데이트 완료")
    }
}