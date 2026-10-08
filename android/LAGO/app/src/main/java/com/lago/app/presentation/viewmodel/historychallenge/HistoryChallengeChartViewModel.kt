package com.lago.app.presentation.viewmodel.historychallenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.domain.entity.*
import com.lago.app.domain.repository.ChartRepository
import com.lago.app.domain.usecase.AnalyzeChartPatternUseCase
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject
import com.lago.app.presentation.ui.chart.v5.HistoricalDataRequestListener
import com.lago.app.presentation.viewmodel.chart.ChartUiEvent
import com.lago.app.presentation.viewmodel.chart.ChartUiState
import com.lago.app.presentation.viewmodel.chart.ChartLoadingStage
import com.lago.app.presentation.viewmodel.chart.HoldingItem
import com.lago.app.presentation.viewmodel.chart.TradingItem
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration.Companion.milliseconds

// MinuteBucket 클래스 제거됨 - 웹소켓에서 완전한 OHLCV 데이터를 받음


/**
 * 역사챌린지 전용 차트 ViewModel
 * 역사챌린지 데이터 구조에 맞춘 별도 구현
 */
@HiltViewModel
class HistoryChallengeChartViewModel @Inject constructor(
    private val chartRepository: ChartRepository,
    private val analyzeChartPatternUseCase: AnalyzeChartPatternUseCase,
    private val userPreferences: UserPreferences,
    private val realTimeCache: com.lago.app.data.cache.RealTimeStockCache,
    private val mockTradeRepository: com.lago.app.domain.repository.MockTradeRepository
) : ViewModel(), HistoricalDataRequestListener, com.lago.app.presentation.ui.chart.v5.JsBridge.PatternAnalysisListener {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ChartUiEvent>()

    // 안전 타임아웃을 위한 Job
    private var chartLoadingTimeoutJob: Job? = null

    // 실시간 차트 업데이트를 위한 JsBridge
    var jsBridge: com.lago.app.presentation.ui.chart.v5.JsBridge? = null

    // 무한 히스토리 관련 상태 변수들
    private var currentEarliestTime: Long? = null
    private var isLoadingHistory = false
    private val gson = Gson()

    // 역사챌린지 웹소켓 연결 상태
    private var historyChallengeWebSocket: Job? = null

    // 현재 활성 역사챌린지 정보
    private var currentChallengeId: Int? = null

    init {
        loadHistoryChallengeList()

        // 실시간 캐시 데이터 모니터링 시작
        observeRealTimeData()
    }

    /**
     * 실시간 캐시 데이터 모니터링
     */
    private fun observeRealTimeData() {
        viewModelScope.launch {
            realTimeCache.quotes
                .sample(500) // 500ms마다 샘플링
                .collect { quotesMap ->
                    android.util.Log.d("HistoryChallengeChartViewModel", "🔥 실시간 데이터 수신: ${quotesMap.size}개 종목")

                    // 현재 차트에 표시된 종목의 실시간 데이터 업데이트 (역사챌린지 전용 키 사용)
                    val currentStockCode = _uiState.value.currentStock.code
                    val historyChallengeKey = "HISTORY_CHALLENGE_$currentStockCode"
                    if (quotesMap.containsKey(historyChallengeKey)) {
                        val realTimeData = quotesMap[historyChallengeKey]!!
                        updateChartWithRealTimeData(realTimeData)
                        android.util.Log.d("HistoryChallengeChartViewModel", "🔥 ${currentStockCode} (키: $historyChallengeKey) 차트 실시간 업데이트: ${realTimeData.closePrice}원")
                    }
                }
        }
    }

    /**
     * 차트에 실시간 데이터 반영 (TradingView 권장 방식: series.update())
     */
    private fun updateChartWithRealTimeData(realTimeData: com.lago.app.domain.entity.StockRealTimeData) {
        _uiState.update { currentState ->
            val updatedStock = currentState.currentStock.copy(
                currentPrice = realTimeData.closePrice?.toFloat() ?: currentState.currentStock.currentPrice,
                priceChange = realTimeData.priceChange.toFloat(), // WebSocket의 실제 전일대비 가격차이
                priceChangePercent = realTimeData.priceChangePercent.toFloat() // calculated property 사용
            )

            currentState.copy(currentStock = updatedStock)
        }

        // 🔥 역사챌린지 차트 실시간 업데이트 활성화
        chartBridge?.let { bridge ->
            // 실시간 데이터를 차트 캔들로 변환 (현재 시간 기준)
            val currentTime = System.currentTimeMillis() / 1000 // epoch seconds
            val normalizedTime = com.lago.app.presentation.ui.chart.v5.ChartTimeManager.normalizeToEpochSeconds(currentTime)
            
            val realtimeCandle = com.lago.app.presentation.ui.chart.v5.CandleData(
                time = normalizedTime,
                open = realTimeData.openPrice?.toFloat() ?: realTimeData.closePrice?.toFloat() ?: 0f,
                high = realTimeData.highPrice?.toFloat() ?: realTimeData.closePrice?.toFloat() ?: 0f,
                low = realTimeData.lowPrice?.toFloat() ?: realTimeData.closePrice?.toFloat() ?: 0f,
                close = realTimeData.closePrice?.toFloat() ?: 0f
            )
            
            // 실시간 거래량 데이터 (있는 경우)
            val realtimeVolume = com.lago.app.presentation.ui.chart.v5.VolumeData(
                time = normalizedTime,
                value = realTimeData.volume?.toLong() ?: 0L,
                color = if (realtimeCandle.close >= realtimeCandle.open) "#26a69a" else "#ef5350"
            )
            
            // 차트에 실시간 업데이트 적용
            bridge.updateRealTimeBar(realtimeCandle, getCurrentTimeFrame())
            bridge.updateRealTimeVolume(realtimeVolume, getCurrentTimeFrame())
            
            android.util.Log.d("HistoryChallengeChart", "🔥 역사챌린지 차트 실시간 업데이트 완료: ${realtimeCandle.close}원")
        }
    }

    /**
     * TradingView 권장 방식으로 차트 초기 데이터 설정 (series.setData)
     */
    private fun setInitialChartData(candlestickData: List<CandlestickData>, interval: String) {
        chartBridge?.let { bridge ->
            // 역사챌린지에서는 항상 epoch seconds로 시간 변환
            val chartCandles = candlestickData.map { candle ->
                // ChartTimeManager로 시간 정규화
                val epochSeconds = com.lago.app.presentation.ui.chart.v5.ChartTimeManager.normalizeToEpochSeconds(candle.time)
                com.lago.app.presentation.ui.chart.v5.CandleData(
                    time = epochSeconds,
                    open = candle.open,
                    high = candle.high,
                    low = candle.low,
                    close = candle.close
                )
            }

            // 거래량 데이터 변환 (있는 경우)
            val volumeData = candlestickData.map { candle ->
                // ChartTimeManager로 시간 정규화
                val epochSeconds = com.lago.app.presentation.ui.chart.v5.ChartTimeManager.normalizeToEpochSeconds(candle.time)
                com.lago.app.presentation.ui.chart.v5.VolumeData(
                    time = epochSeconds,
                    value = candle.volume,
                    color = if (candle.close >= candle.open) "#26a69a" else "#ef5350" // 상승/하락 색상
                )
            }

            // 차트에 초기 데이터 설정
            bridge.setInitialData(chartCandles, volumeData)
            android.util.Log.d("HistoryChallengeChart", "🔥 차트 초기 데이터 설정 완료: ${chartCandles.size}개 캔들")
            
            // 🔥 역사챌린지 전용 보조지표 자동 활성화
            applyHistoryChallengeDefaultIndicators(bridge)
            android.util.Log.d("HistoryChallengeChart", "📊 역사챌린지 전용 보조지표 자동 활성화 완료")
        }
    }

    /**
     * 현재 선택된 타임프레임 반환
     */
    private fun getCurrentTimeFrame(): String {
        return _uiState.value.config.timeFrame
    }

    /**
     * 역사챌린지 전용 기본 보조지표 자동 활성화
     * 초기 진입 시 유용한 지표들을 자동으로 활성화하여 차트 분석 편의성 증대
     */
    private fun applyHistoryChallengeDefaultIndicators(bridge: com.lago.app.presentation.ui.chart.v5.JsBridge) {
        // 거래량 (필수): 주식 거래 분석의 기본
        bridge.setIndicatorWithQueue("volume", true)
        
        // 볼린저 밴드: 변동성과 추세 파악에 유용
        bridge.setIndicatorWithQueue("bollingerBands", true)
        
        // SMA5: 단기 이동평균선으로 추세 확인에 유용
        bridge.setIndicatorWithQueue("sma5", true)
        
        // SMA20: 중기 이동평균선으로 주가 지지/저항 확인
        bridge.setIndicatorWithQueue("sma20", true)
        
        // UI 상태도 동기화 (사용자가 설정 화면에서 확인할 수 있도록)
        _uiState.update { state ->
            state.copy(
                config = state.config.copy(
                    indicators = state.config.indicators.copy(
                        volume = true,
                        bollingerBands = true,
                        sma5 = true,
                        sma20 = true
                    )
                )
            )
        }
        
        android.util.Log.d("HistoryChallengeChart", "📊 기본 지표 활성화: 거래량, 볼린저밴드, SMA5, SMA20")
    }

    /**
     * TradingView 권장 방식으로 과거 데이터를 차트 앞쪽에 추가
     */
    private fun prependHistoricalDataToChart(historicalData: List<CandlestickData>) {
        chartBridge?.let { bridge ->
            // 역사챌린지에서는 항상 epoch seconds로 시간 변환
            val historicalCandles = historicalData.map { candle ->
                // ChartTimeManager로 시간 정규화
                val epochSeconds = com.lago.app.presentation.ui.chart.v5.ChartTimeManager.normalizeToEpochSeconds(candle.time)
                com.lago.app.presentation.ui.chart.v5.CandleData(
                    time = epochSeconds,
                    open = candle.open,
                    high = candle.high,
                    low = candle.low,
                    close = candle.close
                )
            }

            // 과거 거래량 데이터 변환
            val historicalVolumes = historicalData.map { candle ->
                // ChartTimeManager로 시간 정규화
                val epochSeconds = com.lago.app.presentation.ui.chart.v5.ChartTimeManager.normalizeToEpochSeconds(candle.time)
                com.lago.app.presentation.ui.chart.v5.VolumeData(
                    time = epochSeconds,
                    value = candle.volume,
                    color = if (candle.close >= candle.open) "#26a69a" else "#ef5350"
                )
            }

            // TradingView 권장 방식: 기존 데이터와 병합 후 setData 호출
            bridge.prependHistoricalData(historicalCandles, historicalVolumes)
            android.util.Log.d("HistoryChallengeChart", "🔥 무한 히스토리 데이터 차트에 추가 완료: ${historicalCandles.size}개 캔들")
        }
    }

    /**
     * 역사챌린지 데이터를 CandlestickData로 변환 (타임프레임별 버킷 시작 시각으로 스냅)
     */
    private fun convertHistoryChallengeData(data: Map<String, Any>, timeFrame: String = "1"): CandlestickData {
        val originDateTime = data["originDateTime"] as String
        val openPrice = (data["openPrice"] as Number).toFloat()
        val highPrice = (data["highPrice"] as Number).toFloat()
        val lowPrice = (data["lowPrice"] as Number).toFloat()
        val closePrice = (data["closePrice"] as Number).toFloat()
        val volume = (data["volume"] as Number).toLong()

        // originDateTime을 정규화된 시간으로 변환
        val normalizedTime = parseHistoryChallengeDateTime(originDateTime)

        return CandlestickData(
            time = normalizedTime,
            open = openPrice,
            high = highPrice,
            low = lowPrice,
            close = closePrice,
            volume = volume
        )
    }

    /**
     * 역사챌린지 과거 데이터 로드
     */
    private fun loadHistoryChallengeData(challengeId: Int, interval: String, pastMinutes: Int? = null, pastDays: Int? = null) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HistoryChallengeChart", "🔥 차트 데이터 로드 시작")
                android.util.Log.d("HistoryChallengeChart", "🔥 파라미터: challengeId=$challengeId, interval=$interval")
                android.util.Log.d("HistoryChallengeChart", "🔥 과거 기간: pastMinutes=$pastMinutes, pastDays=$pastDays")

                _uiState.update { it.copy(isLoading = true, chartLoadingStage = ChartLoadingStage.DATA_LOADING) }

                // 역사챌린지 전용 차트 API 호출 (현재 시간 기준)
                val response = chartRepository.getHistoryChallengeChart(challengeId, interval, pastMinutes, pastDays)

                response.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val data = resource.data ?: return@collect

                            android.util.Log.d("HistoryChallengeChart", "🔥 차트 데이터 로드 성공: ${data.size}개 캔들")
                            if (data.isNotEmpty()) {
                                val firstCandle = data.first()
                                val lastCandle = data.last()
                                android.util.Log.d("HistoryChallengeChart", "🔥 첫 캔들: ${java.util.Date(firstCandle.time)} - ${firstCandle.close}원")
                                android.util.Log.d("HistoryChallengeChart", "🔥 마지막 캔들: ${java.util.Date(lastCandle.time)} - ${lastCandle.close}원")
                            }

                            _uiState.update { state ->
                                state.copy(
                                    candlestickData = data,
                                    isLoading = false,
                                    chartLoadingStage = ChartLoadingStage.CHART_READY,
                                    errorMessage = null
                                )
                            }

                            // TradingView 권장 방식으로 차트 초기 데이터 설정
                            setInitialChartData(data, interval)

                            // 가장 오래된 시간 기록 (무한 히스토리용)
                            currentEarliestTime = data.minByOrNull { it.time }?.time
                            android.util.Log.d("HistoryChallengeChart", "🔥 가장 오래된 시간: ${currentEarliestTime?.let { java.util.Date(it) }}")
                        }
                        is Resource.Error -> {
                            android.util.Log.e("HistoryChallengeChart", "🚨 차트 데이터 로드 실패: ${resource.message}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = resource.message,
                                    chartLoadingStage = ChartLoadingStage.CHART_READY
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "데이터 로드 실패: ${e.message}",
                        chartLoadingStage = ChartLoadingStage.CHART_READY
                    )
                }
            }
        }
    }

    /**
     * 역사챌린지 실시간 웹소켓 연결
     */
    private fun connectHistoryChallengeWebSocket(challengeId: Int) {
        // 기존 연결 해제
        historyChallengeWebSocket?.cancel()

        historyChallengeWebSocket = viewModelScope.launch {
            try {
                android.util.Log.d("HistoryChallengeChart", "역사챌린지 웹소켓 연결 시작: challengeId=$challengeId")

                // STOMP WebSocket 연결 설정
                val stompClient = ua.naiksoftware.stomp.Stomp.over(
                    ua.naiksoftware.stomp.Stomp.ConnectionProvider.OKHTTP,
                    com.lago.app.util.Constants.WS_STOCK_URL
                )

                // 역사챌린지 토픽 구독: /topic/history-challenge
                val subscription = stompClient.topic("/topic/history-challenge")
                    .subscribe({ stompMessage ->
                        try {
                            android.util.Log.d("HistoryChallengeChart", "웹소켓 메시지 수신: ${stompMessage.payload}")

                            // JSON 파싱하여 HistoryChallengeWebSocketData로 변환
                            val webSocketData = gson.fromJson(
                                stompMessage.payload,
                                com.lago.app.data.remote.dto.HistoryChallengeWebSocketData::class.java
                            )

                            // 모든 데이터 처리 (rowId가 0이어도 유효한 데이터)
                            // 정규화된 시간으로 변환
                            val candleData = CandlestickData(
                                time = parseHistoryChallengeDateTime(webSocketData.originDateTime),
                                open = webSocketData.openPrice.toFloat(),
                                high = webSocketData.highPrice.toFloat(),
                                low = webSocketData.lowPrice.toFloat(),
                                close = webSocketData.closePrice.toFloat(),
                                volume = webSocketData.volume.toLong()
                            )

                            // UI 업데이트
                            updateRealTimeChart(candleData)
                        } catch (e: Exception) {
                            android.util.Log.e("HistoryChallengeChart", "웹소켓 데이터 파싱 실패", e)
                        }
                    }, { error ->
                        android.util.Log.e("HistoryChallengeChart", "웹소켓 구독 오류", error)
                    })

                // 연결 시작
                stompClient.connect()

                android.util.Log.d("HistoryChallengeChart", "역사챌린지 웹소켓 연결 완료")

            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "웹소켓 연결 실패", e)
            }
        }
    }

    /**
     * 역사챌린지 날짜시간 문자열을 정규화된 epoch seconds로 변환
     */
    private fun parseHistoryChallengeDateTime(dateTimeString: String): Long {
        return com.lago.app.presentation.ui.chart.v5.ChartTimeManager.parseHistoryChallengeDateTime(dateTimeString)
    }

    /**
     * 실시간 데이터 업데이트 (웹소켓 originDateTime 기반)
     * 웹소켓에서 받은 역사챌린지 데이터를 TradingView 차트에 반영
     */
    private fun updateRealTimeChart(candleData: CandlestickData) {
        // WebSocket에서 받은 데이터를 정규화
        val normalizedTime = com.lago.app.presentation.ui.chart.v5.ChartTimeManager.normalizeToEpochSeconds(candleData.time)
        val tickPrice = candleData.close
        val tickVolume = candleData.volume

        android.util.Log.d("HistoryChallengeChart", "📊 역사챌린지 데이터 수신: time=${Date(normalizedTime * 1000)}, price=$tickPrice, volume=$tickVolume")

        // 현재 상태의 캔들스틱 데이터 업데이트
        _uiState.update { state ->
            val updatedCandles = state.candlestickData.toMutableList()

            // 같은 시간대면 마지막 캔들 업데이트, 다르면 새 캔들 추가
            if (updatedCandles.isNotEmpty()) {
                val lastCandle = updatedCandles.last()

                if (normalizedTime == lastCandle.time) {
                    // 동일한 시간: 기존 캔들 업데이트
                    updatedCandles[updatedCandles.size - 1] = candleData
                    android.util.Log.d("HistoryChallengeChart", "📊 기존 캔들 업데이트: ${Date(normalizedTime * 1000)}")
                } else if (normalizedTime > lastCandle.time) {
                    // 새로운 시간: 새 캔들 추가
                    updatedCandles.add(candleData)
                    android.util.Log.d("HistoryChallengeChart", "📊 새 캔들 생성: ${Date(normalizedTime * 1000)}")
                } else {
                    // 과거 시간 (정상적이지 않음)
                    android.util.Log.w("HistoryChallengeChart", "📊 과거 시간 데이터 무시: ${Date(normalizedTime * 1000)} < ${Date(lastCandle.time * 1000)}")
                    return@update state
                }
            } else {
                updatedCandles.add(candleData)
                android.util.Log.d("HistoryChallengeChart", "📊 첫 캔들 생성: ${Date(normalizedTime * 1000)}")
            }

            state.copy(
                candlestickData = updatedCandles,
                currentStock = state.currentStock.copy(
                    currentPrice = candleData.close,
                    priceChange = if (updatedCandles.size > 1) {
                        candleData.close - updatedCandles[updatedCandles.size - 2].close
                    } else 0f,
                    priceChangePercent = if (updatedCandles.size > 1) {
                        val prevClose = updatedCandles[updatedCandles.size - 2].close
                        ((candleData.close - prevClose) / prevClose) * 100
                    } else 0f
                )
            )
        }

        // TradingView 권장 방식으로 실시간 차트 업데이트
        chartBridge?.let { bridge ->
            val realTimeCandle = com.lago.app.presentation.ui.chart.v5.CandleData(
                time = normalizedTime,
                open = candleData.open,
                high = candleData.high,
                low = candleData.low,
                close = candleData.close
            )

            // 🔥 실시간 캔들 업데이트 활성화: series.update() 방식
            // 동일 time = 기존 캔들 덮어쓰기, 새 time = 새 캔들 추가
            bridge.updateRealTimeBar(realTimeCandle, getCurrentTimeFrame())
            android.util.Log.d("HistoryChallengeChart", "📊 실시간 캔들 업데이트: ${Date(normalizedTime * 1000)} - ${candleData.close}원")

            // 🔥 실시간 거래량 업데이트 활성화
            val realTimeVolume = com.lago.app.presentation.ui.chart.v5.VolumeData(
                time = normalizedTime,
                value = candleData.volume,
                color = if (candleData.close >= candleData.open) "#26a69a" else "#ef5350"
            )
            bridge.updateRealTimeVolume(realTimeVolume, getCurrentTimeFrame())
            android.util.Log.d("HistoryChallengeChart", "📊 실시간 거래량 업데이트: ${candleData.volume}")

            // 🔥 새로운 캔들이 추가된 경우 실시간으로 스크롤 (예제와 동일한 방식)
            if (normalizedTime > (_uiState.value.candlestickData.lastOrNull()?.time ?: 0L)) {
                bridge.scrollToRealTime()
                android.util.Log.d("HistoryChallengeChart", "📊 새 캔들 추가로 실시간 스크롤 활성화")
            }
        }
    }

    // 기존 aggregateTickToMinuteBar 함수 제거됨
    // 웹소켓에서 완전한 OHLCV 데이터를 받으므로 별도 집계 불필요

    /**
     * 역사챌린지 목록 로드
     */
    private fun loadHistoryChallengeList() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, chartLoadingStage = ChartLoadingStage.DATA_LOADING) }

                val response = chartRepository.getHistoryChallenge()
                response.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val challenge = resource.data ?: return@collect
                            // 단일 챌린지를 기본으로 설정
                            currentChallengeId = challenge.challengeId

                            _uiState.update { state ->
                                state.copy(
                                    currentStock = ChartStockInfo(
                                        code = challenge.stockCode,
                                        name = challenge.stockName,
                                        currentPrice = challenge.currentPrice.toFloat(),
                                        priceChange = challenge.fluctuationPrice.toFloat(),
                                        priceChangePercent = challenge.fluctuationRate,
                                        previousDay = null
                                    ),
                                    config = ChartConfig(
                                        stockCode = challenge.stockCode,
                                        timeFrame = "1", // 역사챌린지는 1분봉
                                        indicators = ChartIndicators()
                                    ),
                                    isLoading = false,
                                    chartLoadingStage = ChartLoadingStage.CHART_READY
                                )
                            }

                            // 역사챌린지 차트 데이터 로드 (현재 시간 기준 과거 100일)
                            loadHistoryChallengeData(
                                challengeId = challenge.challengeId,
                                interval = "DAY",
                                pastDays = 100
                            )

                            // 웹소켓 연결
                            connectHistoryChallengeWebSocket(challenge.challengeId)
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = resource.message,
                                    chartLoadingStage = ChartLoadingStage.CHART_READY
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "역사챌린지 데이터 로드 실패: ${e.message}",
                        chartLoadingStage = ChartLoadingStage.CHART_READY
                    )
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 초기 상태 - 종목이 선택되기 전까지 빈 상태 유지
            _uiState.update { state ->
                state.copy(
                    currentStock = ChartStockInfo(
                        code = "",
                        name = "",
                        currentPrice = 0f,
                        priceChange = 0f,
                        priceChangePercent = 0f,
                        previousDay = null
                    ),
                    config = ChartConfig(
                        stockCode = "",
                        timeFrame = "1", // 역사챌린지는 1분봉
                        indicators = ChartIndicators()
                    )
                )
            }
        }
    }

    /**
     * 역사챌린지 변경
     */
    fun changeHistoryChallenge(challengeId: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, chartLoadingStage = ChartLoadingStage.DATA_LOADING) }

                // 챌린지 정보 로드 (단일 챌린지이므로 challengeId 검증 생략)
                val response = chartRepository.getHistoryChallenge()
                response.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val challenge = resource.data ?: return@collect

                            // 요청된 challengeId와 실제 challengeId가 다르면 경고 로그
                            if (challenge.challengeId != challengeId) {
                                android.util.Log.w("HistoryChallengeChart", "요청된 challengeId($challengeId)와 실제 challengeId(${challenge.challengeId})가 다릅니다.")
                            }

                            currentChallengeId = challenge.challengeId

                            _uiState.update { state ->
                                state.copy(
                                    currentStock = ChartStockInfo(
                                        code = challenge.stockCode,
                                        name = challenge.stockName,
                                        currentPrice = challenge.currentPrice.toFloat(),
                                        priceChange = challenge.fluctuationPrice.toFloat(),
                                        priceChangePercent = challenge.fluctuationRate,
                                        previousDay = null
                                    ),
                                    config = state.config.copy(stockCode = challenge.stockCode)
                                )
                            }

                            // 차트 데이터 로드 (현재 시간 기준 과거 기간)
                            val interval = convertTimeFrameToInterval(_uiState.value.config.timeFrame)
                            val (pastMinutes, pastDays) = getTimeFramePeriod(interval)
                            loadHistoryChallengeData(
                                challengeId = challenge.challengeId,
                                interval = interval,
                                pastMinutes = pastMinutes,
                                pastDays = pastDays
                            )

                            // 웹소켓 재연결
                            connectHistoryChallengeWebSocket(challenge.challengeId)
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = resource.message,
                                    chartLoadingStage = ChartLoadingStage.CHART_READY
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "챌린지 변경 실패: ${e.message}",
                        chartLoadingStage = ChartLoadingStage.CHART_READY
                    )
                }
            }
        }
    }

    fun onEvent(event: ChartUiEvent) {
        when (event) {
            is ChartUiEvent.ChangeStock -> {
                changeStock(event.stockCode)
            }
            is ChartUiEvent.ChangeStockWithInfo -> {
                changeStockWithInfo(event.stockCode, event.stockInfo)
            }
            is ChartUiEvent.ChangeTimeFrame -> {
                changeTimeFrame(event.timeFrame)
            }
            is ChartUiEvent.ToggleIndicator -> {
                toggleIndicator(event.indicatorType, event.enabled)
            }
            is ChartUiEvent.RefreshData -> {
                refreshData()
            }
            is ChartUiEvent.ClearError -> {
                clearError()
            }
            is ChartUiEvent.ClearTradeMessage -> {
                // 역사챌린지에서는 매매 메시지가 없으므로 빈 처리
            }
            is ChartUiEvent.SelectPattern -> {
                // 역사챌린지에서는 패턴 분석 기능이 제한적이므로 빈 처리
            }
            is ChartUiEvent.NextPatternStage -> {
                // 역사챌린지에서는 패턴 분석 기능이 제한적이므로 빈 처리
            }
            is ChartUiEvent.ResetPatternStage -> {
                // 역사챌린지에서는 패턴 분석 기능이 제한적이므로 빈 처리
            }
            is ChartUiEvent.ToggleFavorite -> {
                toggleFavorite()
            }
            is ChartUiEvent.ChangeBottomTab -> {
                changeBottomTab(event.tabIndex)
            }
            is ChartUiEvent.AnalyzePattern -> {
                requestPatternAnalysis()
            }
            is ChartUiEvent.BuyClicked -> {
                handleBuyClicked()
            }
            is ChartUiEvent.SellClicked -> {
                handleSellClicked()
            }
            is ChartUiEvent.ShowIndicatorSettings -> {
                _uiState.update { it.copy(showIndicatorSettings = true) }
            }
            is ChartUiEvent.HideIndicatorSettings -> {
                _uiState.update { it.copy(showIndicatorSettings = false) }
            }
            is ChartUiEvent.ToggleIndicatorSettings -> {
                _uiState.update { it.copy(showIndicatorSettings = !it.showIndicatorSettings) }
            }
            is ChartUiEvent.LoadTradingSignals -> {
                loadTradingSignals()
            }
            is ChartUiEvent.ToggleUserTradingSignals -> {
                //_uiState.update { it.copy(showUserTradingSignals = event.show) }
            }
            is ChartUiEvent.SelectAITradingSignals -> {
                _uiState.update { it.copy(selectedAI = event.aiSource) }
            }
            is ChartUiEvent.BackPressed -> {
                // 뒤로 가기 처리
            }
        }
    }

    private fun changeStock(stockCode: String) {
        viewModelScope.launch {
            // 기존 웹소켓 연결 해제
            historyChallengeWebSocket?.cancel()

            try {
                _uiState.update { it.copy(isLoading = true, chartLoadingStage = ChartLoadingStage.DATA_LOADING) }

                val stockInfoResult = chartRepository.getStockInfo(stockCode)
                stockInfoResult.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val stockInfo = resource.data ?: return@collect

                            _uiState.update { state ->
                                state.copy(
                                    currentStock = stockInfo,
                                    config = state.config.copy(stockCode = stockCode),
                                    chartLoadingStage = ChartLoadingStage.DATA_LOADING
                                )
                            }

                            // 차트 데이터 로드 (역사챌린지용)
                            currentChallengeId?.let { challengeId ->
                                val currentConfig = _uiState.value.config
                                val interval = convertTimeFrameToInterval(currentConfig.timeFrame)
                                val (pastMinutes, pastDays) = getTimeFramePeriod(interval)
                                loadHistoryChallengeData(
                                    challengeId = challengeId,
                                    interval = interval,
                                    pastMinutes = pastMinutes,
                                    pastDays = pastDays
                                )

                                // 실시간 웹소켓 연결
                                connectHistoryChallengeWebSocket(challengeId)
                            }

                            // 매매내역과 보유현황 로드
                            loadTradingHistory(stockCode)
                            loadHoldings()
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = resource.message,
                                    chartLoadingStage = ChartLoadingStage.CHART_READY
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "주식 정보 로드 실패: ${e.message}",
                        chartLoadingStage = ChartLoadingStage.CHART_READY
                    )
                }
            }
        }
    }

    private fun changeStockWithInfo(stockCode: String, stockInfo: ChartStockInfo) {
        _uiState.update { state ->
            state.copy(
                currentStock = stockInfo,
                config = state.config.copy(stockCode = stockCode)
            )
        }
        // 역사챌린지용 데이터 로드
        currentChallengeId?.let { challengeId ->
            val interval = convertTimeFrameToInterval(_uiState.value.config.timeFrame)
            val (pastMinutes, pastDays) = getTimeFramePeriod(interval)
            loadHistoryChallengeData(
                challengeId = challengeId,
                interval = interval,
                pastMinutes = pastMinutes,
                pastDays = pastDays
            )
            connectHistoryChallengeWebSocket(challengeId)
        }
        loadTradingHistory(stockCode)
        loadHoldings()
    }

    private fun changeTimeFrame(timeFrame: String) {
        _uiState.update { state ->
            state.copy(config = state.config.copy(timeFrame = timeFrame))
        }
        // 역사챌린지용 타임프레임 변경
        currentChallengeId?.let { challengeId ->
            val interval = convertTimeFrameToInterval(timeFrame)
            val (pastMinutes, pastDays) = getTimeFramePeriod(interval)
            loadHistoryChallengeData(
                challengeId = challengeId,
                interval = interval,
                pastMinutes = pastMinutes,
                pastDays = pastDays
            )
        }
    }

    /**
     * UI 타임프레임을 API interval로 변환 (ChartTimeManager 사용)
     */
    private fun convertTimeFrameToInterval(timeFrame: String): String {
        return com.lago.app.presentation.ui.chart.v5.ChartTimeManager.toApiTimeFrame(timeFrame)
    }

    /**
     * interval에 따른 적절한 과거 기간 반환
     */
    private fun getTimeFramePeriod(interval: String): Pair<Int?, Int?> {
        return when (interval) {
            "MINUTE", "MINUTE3", "MINUTE5" -> Pair(1440, null) // 24시간 (1440분)
            "MINUTE10", "MINUTE15" -> Pair(4320, null) // 3일 (4320분)
            "MINUTE30", "MINUTE60" -> Pair(10080, null) // 7일 (10080분)
            "DAY" -> Pair(null, 100) // 100일
            "WEEK" -> Pair(null, 365) // 52주 (365일)
            "MONTH" -> Pair(null, 730) // 24개월 (730일)
            "YEAR" -> Pair(null, 1825) // 5년 (1825일)
            else -> Pair(null, 100) // 기본 100일
        }
    }

    /**
     * 현재 시간을 API 형식으로 반환
     * 현재 시간으로 요청하면 서버에서 해당하는 과거 데이터를 반환
     */
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * 차트 표시를 위한 적절한 기간 계산
     * interval에 따라 fromDateTime을 과거로 설정하여 충분한 데이터 확보
     */
    private fun getChartDateRange(interval: String): Pair<String, String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val toDateTime = dateFormat.format(calendar.time) // 현재 시간

        // interval별로 적절한 과거 기간 설정
        when (interval) {
            "MINUTE", "MINUTE3", "MINUTE5" -> {
                calendar.add(Calendar.HOUR_OF_DAY, -24) // 24시간 전
            }
            "MINUTE10", "MINUTE15" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -3) // 3일 전
            }
            "MINUTE30", "MINUTE60" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -7) // 1주일 전
            }
            "DAY" -> {
                calendar.add(Calendar.DAY_OF_MONTH, -100) // 100일 전
            }
            "WEEK" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -52) // 52주 전
            }
            "MONTH" -> {
                calendar.add(Calendar.MONTH, -24) // 24개월 전
            }
            "YEAR" -> {
                calendar.add(Calendar.YEAR, -5) // 5년 전
            }
            else -> {
                calendar.add(Calendar.DAY_OF_MONTH, -100) // 기본 100일 전
            }
        }

        val fromDateTime = dateFormat.format(calendar.time)
        return Pair(fromDateTime, toDateTime)
    }

    private fun toggleIndicator(indicatorType: String, enabled: Boolean) {
        _uiState.update { state ->
            val indicators = when (indicatorType) {
                "sma5" -> state.config.indicators.copy(sma5 = enabled)
                "sma20" -> state.config.indicators.copy(sma20 = enabled)
                "sma60" -> state.config.indicators.copy(sma60 = enabled)
                "sma120" -> state.config.indicators.copy(sma120 = enabled)
                "rsi" -> state.config.indicators.copy(rsi = enabled)
                "macd" -> state.config.indicators.copy(macd = enabled)
                "bollingerBands" -> state.config.indicators.copy(bollingerBands = enabled)
                "volume" -> state.config.indicators.copy(volume = enabled)
                else -> state.config.indicators
            }
            state.copy(config = state.config.copy(indicators = indicators))
        }

        // 지표 변경시 데이터 다시 로드
        refreshData()
    }

    private fun refreshData() {
        val currentState = _uiState.value
        // 역사챌린지용 데이터 새로고침
        currentChallengeId?.let { challengeId ->
            val interval = convertTimeFrameToInterval(currentState.config.timeFrame)
            val (pastMinutes, pastDays) = getTimeFramePeriod(interval)
            loadHistoryChallengeData(
                challengeId = challengeId,
                interval = interval,
                pastMinutes = pastMinutes,
                pastDays = pastDays
            )
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    private fun changeBottomTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedBottomTab = tabIndex) }
    }

    /**
     * 차트 패턴 분석 (JavaScript 브릿지에서 호출)
     * @param fromTime 시작 시간 (JavaScript timestamp seconds)
     * @param toTime 종료 시간 (JavaScript timestamp seconds)
     */
    private fun analyzePatternInRange(fromTime: String, toTime: String) {
        val currentState = _uiState.value

        // 분석 횟수 체크
        if (currentState.patternAnalysisCount >= currentState.maxPatternAnalysisCount) {
            _uiState.update {
                it.copy(patternAnalysisError = "일일 패턴 분석 횟수를 초과했습니다.")
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isPatternAnalyzing = true,
                        patternAnalysisError = null
                    )
                }

                // 로컬 랜덤 패턴 생성
                delay(1500) // 분석하는 것처럼 지연시간 추가
                
                // 미리 정의된 패턴들 중 랜덤 선택
                val availablePatterns = listOf(
                    com.lago.app.data.remote.dto.PatternAnalysisResponse(
                        name = "더블 바텀 패턴",
                        reason = "2025-07-29와 2025-07-29에 저점이 반복 형성되었으며, 아직 넥라인 돌파는 발생하지 않았습니다."
                    ),
                    com.lago.app.data.remote.dto.PatternAnalysisResponse(
                        name = "더블 탑 패턴",
                        reason = "2025-07-23와 2025-07-23에 고점이 반복 형성되었으며, 아직 넥라인 돌파는 발생하지 않았습니다."
                    ),
                    com.lago.app.data.remote.dto.PatternAnalysisResponse(
                        name = "페넌트 패턴",
                        reason = "패턴이 감지되었으나, 상세 정보를 생성할 수 없습니다."
                    ),
                    com.lago.app.data.remote.dto.PatternAnalysisResponse(
                        name = "플래그 패턴",
                        reason = "패턴이 감지되었으나, 상세 정보를 생성할 수 없습니다."
                    ),
                    com.lago.app.data.remote.dto.PatternAnalysisResponse(
                        name = "대칭 삼각형",
                        reason = "수렴형 삼각형 패턴으로, 고점과 저점이 점점 좁아지고 있습니다. 변동성 확대가 예상됩니다. (2025-08-06, 2025-08-07 기준)"
                    )
                )

                // 랜덤으로 하나 선택
                val selectedPattern = availablePatterns.random()
                android.util.Log.d("HistoryChallengeChart", "📊 랜덤 패턴 선택: ${selectedPattern.name}")

                val patternResult = com.lago.app.domain.entity.PatternAnalysisResult(
                    stockCode = currentState.currentStock.code,
                    patterns = listOf(selectedPattern),
                    analysisTime = getCurrentTime(),
                    chartMode = "challenge",
                    timeFrame = currentState.config.timeFrame
                )

                _uiState.update { state ->
                    state.copy(
                        isPatternAnalyzing = false,
                        patternAnalysis = patternResult,
                        patternAnalysisCount = state.patternAnalysisCount + 1,
                        patternAnalysisError = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPatternAnalyzing = false,
                        patternAnalysisError = "패턴 분석 실패: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadTradingSignals() {
        viewModelScope.launch {
            try {
                // 역사챌린지에서는 매매신호가 제공되지 않음
                // AI 매매봇은 일반 모의투자에서만 사용
                _uiState.update { state ->
                    state.copy(tradingSignals = emptyList())
                }
                android.util.Log.d("HistoryChallengeChart", "역사챌린지: 매매신호 기능 비활성화")
            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "매매신호 로드 실패", e)
            }
        }
    }

    private fun loadTradingHistory(stockCode: String) {
        viewModelScope.launch {
            try {
                // 역사챌린지는 개별 계좌가 아닌 글로벌 챌린지이므로
                // 개인 매매내역은 제공되지 않음
                _uiState.update { state ->
                    state.copy(tradingHistory = emptyList())
                }
                android.util.Log.d("HistoryChallengeChart", "역사챌린지: 개인 매매내역 없음")
            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "매매내역 로드 실패", e)
            }
        }
    }


    // 차트 준비 완료 콜백
    fun onChartReady() {
        android.util.Log.d("HistoryChallengeChart", "🔥 차트 준비 완료")
        _uiState.update { it.copy(isLoading = false, chartLoadingStage = ChartLoadingStage.CHART_READY) }
    }

    // 🔥 순차적 로딩 완료 콜백
    fun onChartLoadingCompleted() {
        android.util.Log.d("HistoryChallengeChart", "🎉 모든 차트 로딩 완료!")
        cancelChartLoadingTimeout() // 타임아웃 취소
        _uiState.update { it.copy(isLoading = false, chartLoadingStage = ChartLoadingStage.COMPLETED) }
        
        // 🔥 자동 재시도 메커니즘: 역사챌린지 차트가 제대로 로드되지 않은 경우 재시도
        scheduleAutoRetryIfNeeded()
    }

    /**
     * 🔥 자동 재시도 메커니즘: 역사챌린지 데이터가 제대로 로드되지 않으면 자동으로 재시도
     */
    private fun scheduleAutoRetryIfNeeded() {
        viewModelScope.launch {
            delay(2000) // 2초 후 체크
            
            val currentState = _uiState.value
            val hasData = currentState.candlestickData.isNotEmpty()
            val hasVolumeData = currentState.volumeData.isNotEmpty()
            
            if (!hasData || !hasVolumeData) {
                android.util.Log.w("HistoryChallengeChart", "🔄 자동 재시도: 데이터 부족 감지 (캔들: $hasData, 거래량: $hasVolumeData)")
                
                // 현재 선택된 종목과 시간대로 데이터 재로드
                val stockCode = currentState.currentStock.code
                val timeFrame = currentState.config.timeFrame
                
                if (stockCode.isNotEmpty() && currentChallengeId != null) {
                    android.util.Log.d("HistoryChallengeChart", "🔄 자동 재시도 실행: $stockCode, $timeFrame, 챌린지ID: $currentChallengeId")
                    val interval = convertTimeFrameToInterval(timeFrame)
                    loadHistoryChallengeData(currentChallengeId!!, interval)
                }
            } else {
                android.util.Log.d("HistoryChallengeChart", "✅ 역사챌린지 차트 데이터 정상 확인: 캔들 ${currentState.candlestickData.size}개, 거래량 ${currentState.volumeData.size}개")
                
                // 🔥 데이터가 정상인 경우 주기적 건강상태 모니터링 시작
                startPeriodicHealthCheck()
            }
        }
    }

    /**
     * 🔥 주기적 역사챌린지 차트 건강상태 체크: 15초마다 데이터 상태 확인 및 필요시 새로고침
     */
    private fun startPeriodicHealthCheck() {
        viewModelScope.launch {
            while (true) {
                delay(15000) // 15초마다 체크
                
                val currentState = _uiState.value
                val hasData = currentState.candlestickData.isNotEmpty()
                val hasVolumeData = currentState.volumeData.isNotEmpty()
                val isLoadingStageComplete = currentState.chartLoadingStage == ChartLoadingStage.COMPLETED
                
                if (!hasData || !hasVolumeData || !isLoadingStageComplete) {
                    android.util.Log.w("HistoryChallengeChart", "🏥 건강상태 체크: 데이터 이상 감지 (캔들: $hasData, 거래량: $hasVolumeData, 완료상태: $isLoadingStageComplete)")
                    
                    val stockCode = currentState.currentStock.code
                    val timeFrame = currentState.config.timeFrame
                    
                    if (stockCode.isNotEmpty() && currentChallengeId != null) {
                        android.util.Log.d("HistoryChallengeChart", "🏥 건강상태 체크: 역사챌린지 데이터 새로고침 실행")
                        val interval = convertTimeFrameToInterval(timeFrame)
                        loadHistoryChallengeData(currentChallengeId!!, interval)
                        break // 새로고침 후 건강상태 체크 중단 (완료 후 다시 시작됨)
                    }
                } else {
                    android.util.Log.v("HistoryChallengeChart", "🏥 건강상태 체크: 정상 (캔들: ${currentState.candlestickData.size}개)")
                }
            }
        }
    }

    // 차트 로딩 상태 변경 콜백
    fun onChartLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
            _uiState.update { it.copy(chartLoadingStage = ChartLoadingStage.WEBVIEW_LOADING) }
        }
    }


    // HistoricalDataRequestListener 구현
    override fun onRequestHistoricalData(barsToLoad: Int) {
        if (isLoadingHistory) return

        isLoadingHistory = true
        android.util.Log.d("HistoryChallengeChart", "과거 데이터 요청: $barsToLoad bars")

        viewModelScope.launch {
            try {
                val earliestTime = currentEarliestTime ?: run {
                    android.util.Log.w("HistoryChallengeChart", "earliestTime이 설정되지 않음")
                    isLoadingHistory = false
                    return@launch
                }

                currentChallengeId?.let { challengeId ->
                    // 역사챌린지 과거 데이터 추가 로드
                    val currentConfig = _uiState.value.config
                    val interval = convertTimeFrameToInterval(currentConfig.timeFrame)

                    // earliestTime을 기준으로 이전 데이터 요청
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val beforeDateTime = dateFormat.format(Date(earliestTime))

                    android.util.Log.d("HistoryChallengeChart", "🔥 무한 히스토리 로드: $beforeDateTime 이전 ${barsToLoad}개 캔들")

                    // 무한 히스토리 API 호출
                    val response = chartRepository.getHistoryChallengeHistoricalData(
                        challengeId = challengeId,
                        interval = interval,
                        beforeDateTime = beforeDateTime,
                        limit = barsToLoad
                    )

                    response.collect { resource ->
                        when (resource) {
                            is Resource.Success -> {
                                val historicalData = resource.data ?: emptyList()

                                if (historicalData.isNotEmpty()) {
                                    android.util.Log.d("HistoryChallengeChart", "🔥 무한 히스토리 성공: ${historicalData.size}개 캔들 로드")

                                    // 기존 데이터 앞에 과거 데이터 추가 (TradingView 권장 방식)
                                    _uiState.update { state ->
                                        val combinedData = (historicalData + state.candlestickData).sortedBy { it.time }
                                        state.copy(candlestickData = combinedData)
                                    }

                                    // 가장 오래된 시간 업데이트
                                    currentEarliestTime = historicalData.minByOrNull { it.time }?.time
                                    android.util.Log.d("HistoryChallengeChart", "🔥 새로운 earliestTime: ${currentEarliestTime?.let { Date(it) }}")

                                    // TradingView 권장 방식으로 과거 데이터 추가
                                    prependHistoricalDataToChart(historicalData)
                                } else {
                                    android.util.Log.w("HistoryChallengeChart", "무한 히스토리: 더 이상 과거 데이터가 없음")
                                }
                            }
                            is Resource.Error -> {
                                android.util.Log.e("HistoryChallengeChart", "🚨 무한 히스토리 로드 실패: ${resource.message}")
                            }
                            is Resource.Loading -> {
                                android.util.Log.d("HistoryChallengeChart", "무한 히스토리 로딩 중...")
                            }
                        }
                    }
                } ?: android.util.Log.w("HistoryChallengeChart", "currentChallengeId가 설정되지 않음")

            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "과거 데이터 로드 실패", e)
            } finally {
                isLoadingHistory = false
            }
        }
    }

    // ===== 차트 브릿지 연결 관련 (패턴 분석용) =====

    private var chartBridge: com.lago.app.presentation.ui.chart.v5.JsBridge? = null

    /**
     * 차트 브릿지 설정 (HistoryChallengeChartScreen에서 호출)
     */
    fun setChartBridge(bridge: com.lago.app.presentation.ui.chart.v5.JsBridge?) {
        chartBridge = bridge
        // 패턴 분석 리스너 설정
        bridge?.setPatternAnalysisListener(this)
        android.util.Log.d("HistoryChallengeChartViewModel", "📊 차트 브릿지 설정 완료")
    }

    /**
     * UI에서 패턴 분석 버튼 클릭 시 호출
     */
    fun requestPatternAnalysis() {
        chartBridge?.analyzePatternInVisibleRange()
            ?: android.util.Log.w("HistoryChallengeChartViewModel", "📊 차트 브릿지가 설정되지 않음")
    }

    /**
     * JsBridge에서 호출되는 패턴 분석 메서드 (보이는 영역 기반)
     * @param fromTime JavaScript에서 전달받은 시작 시간 (seconds)
     * @param toTime JavaScript에서 전달받은 종료 시간 (seconds)
     */
    override fun onAnalyzePatternInRange(fromTime: String, toTime: String) {
        analyzePatternInRange(fromTime, toTime)
    }

    /**
     * 패턴 분석 에러 처리
     * @param message 에러 메시지
     */
    override fun onPatternAnalysisError(message: String) {
        android.util.Log.w("HistoryChallengeChartViewModel", "📊 JsBridge 패턴 분석 에러: $message")
        _uiState.update {
            it.copy(
                isPatternAnalyzing = false,
                patternAnalysisError = message
            )
        }
    }

    /**
     * 패턴 분석 완료 처리 (선택사항)
     * @param patternName 패턴명
     * @param description 패턴 설명
     */
    override fun onPatternAnalysisComplete(patternName: String, description: String) {
        android.util.Log.d("HistoryChallengeChartViewModel", "📊 JsBridge 패턴 분석 완료: $patternName - $description")
        // 이 메서드는 JavaScript에서 결과를 직접 표시할 때 사용 (현재는 Android UI에서 처리)
    }


    /**
     * JavaScript timestamp를 API 형식으로 변환
     * @param jsTimeString JavaScript에서 전달받은 시간 문자열 (seconds)
     * @return API 형식 시간 문자열 ("yyyy-MM-dd'T'HH:mm:ss")
     */
    private fun convertToApiFormat(jsTimeString: String): String {
        return try {
            val epochSeconds = jsTimeString.toLong()
            val instant = java.time.Instant.ofEpochSecond(epochSeconds)
            val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            localDateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        } catch (e: Exception) {
            android.util.Log.e("HistoryChallengeChartViewModel", "시간 변환 실패: $jsTimeString", e)
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        }
    }

    /**
     * 현재 시간을 문자열로 반환
     * @return 현재 시간 (yyyy-MM-dd HH:mm:ss 형식)
     */
    private fun getCurrentTime(): String {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return java.time.LocalDateTime.now().format(formatter)
    }

    /**
     * 패턴 분석 에러 메시지 클리어
     */
    fun clearPatternAnalysisError() {
        _uiState.update { it.copy(patternAnalysisError = null) }
    }

    /**
     * 역사챌린지에서 구매 처리 (웹소켓 실시간 가격 사용)
     */
    private fun handleBuyClicked() {
        android.util.Log.d("HistoryChallengeChart", "📈 역사챌린지 구매 버튼 클릭")
        val currentState = _uiState.value
        val currentPrice = currentState.currentStock.currentPrice
        val stockCode = currentState.currentStock.code
        val accountType = 1 // 역사챌린지 = 1
        
        if (stockCode.isEmpty() || currentPrice <= 0f) {
            android.util.Log.w("HistoryChallengeChart", "📈 구매 실패: 유효하지 않은 주식 정보")
            _uiState.update { it.copy(errorMessage = "주식 정보를 확인할 수 없습니다.") }
            return
        }
        
        // 웹소켓 실시간 가격으로 1주 구매 (데모용)
        val quantity = 1
        val priceInt = currentPrice.toInt()
        
        android.util.Log.d("HistoryChallengeChart", "📈 역사챌린지 구매 요청: $stockCode, ${quantity}주, ${priceInt}원")
        
        viewModelScope.launch {
            try {
                mockTradeRepository.buyStock(
                    stockCode = stockCode,
                    quantity = quantity,
                    price = priceInt,
                    accountType = accountType
                ).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            android.util.Log.d("HistoryChallengeChart", "📈 역사챌린지 구매 성공: ${quantity}주")
                            _uiState.update { it.copy(
                                errorMessage = null,
                                tradeMessage = "${currentState.currentStock.name} ${quantity}주를 ${String.format("%,d", priceInt)}원에 구매했습니다. (역사챌린지)"
                            )}
                            // 거래 후 보유현황 새로고침
                            loadHoldings()
                        }
                        is Resource.Error -> {
                            android.util.Log.e("HistoryChallengeChart", "📈 역사챌린지 구매 실패: ${resource.message}")
                            _uiState.update { it.copy(errorMessage = resource.message ?: "구매에 실패했습니다.") }
                        }
                        is Resource.Loading -> {
                            android.util.Log.d("HistoryChallengeChart", "📈 역사챌린지 구매 처리 중...")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "📈 역사챌린지 구매 예외", e)
                _uiState.update { it.copy(errorMessage = "구매 중 오류가 발생했습니다.") }
            }
        }
    }

    /**
     * 역사챌린지에서 판매 처리 (웹소켓 실시간 가격 사용)
     */
    private fun handleSellClicked() {
        android.util.Log.d("HistoryChallengeChart", "📉 역사챌린지 판매 버튼 클릭")
        val currentState = _uiState.value
        val currentPrice = currentState.currentStock.currentPrice
        val stockCode = currentState.currentStock.code
        val accountType = 1 // 역사챌린지 = 1
        
        if (stockCode.isEmpty() || currentPrice <= 0f) {
            android.util.Log.w("HistoryChallengeChart", "📉 판매 실패: 유효하지 않은 주식 정보")
            _uiState.update { it.copy(errorMessage = "주식 정보를 확인할 수 없습니다.") }
            return
        }
        
        // 웹소켓 실시간 가격으로 1주 판매 (데모용)
        val quantity = 1
        val priceInt = currentPrice.toInt()
        
        android.util.Log.d("HistoryChallengeChart", "📉 역사챌린지 판매 요청: $stockCode, ${quantity}주, ${priceInt}원")
        
        viewModelScope.launch {
            try {
                mockTradeRepository.sellStock(
                    stockCode = stockCode,
                    quantity = quantity,
                    price = priceInt,
                    accountType = accountType
                ).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            android.util.Log.d("HistoryChallengeChart", "📉 역사챌린지 판매 성공: ${quantity}주")
                            _uiState.update { it.copy(
                                errorMessage = null,
                                tradeMessage = "${currentState.currentStock.name} ${quantity}주를 ${String.format("%,d", priceInt)}원에 판매했습니다. (역사챌린지)"
                            )}
                            // 거래 후 보유현황 새로고침
                            loadHoldings()
                        }
                        is Resource.Error -> {
                            android.util.Log.e("HistoryChallengeChart", "📉 역사챌린지 판매 실패: ${resource.message}")
                            _uiState.update { it.copy(errorMessage = resource.message ?: "판매에 실패했습니다.") }
                        }
                        is Resource.Loading -> {
                            android.util.Log.d("HistoryChallengeChart", "📉 역사챌린지 판매 처리 중...")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "📉 역사챌린지 판매 예외", e)
                _uiState.update { it.copy(errorMessage = "판매 중 오류가 발생했습니다.") }
            }
        }
    }

    /**
     * 보유현황 새로고침 (역사챌린지용)
     */
    private fun loadHoldings() {
        viewModelScope.launch {
            try {
                mockTradeRepository.getStockHoldings().collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            android.util.Log.d("HistoryChallengeChart", "💰 역사챌린지 보유현황 조회 성공: ${resource.data?.size}개 종목")
                            // 보유현황 업데이트 로직 (필요시 UiState에 holdings 필드 추가)
                        }
                        is Resource.Error -> {
                            android.util.Log.e("HistoryChallengeChart", "💰 역사챌린지 보유현황 조회 실패: ${resource.message}")
                        }
                        is Resource.Loading -> {
                            android.util.Log.d("HistoryChallengeChart", "💰 역사챌린지 보유현황 조회 중...")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeChart", "💰 역사챌린지 보유현황 조회 예외", e)
            }
        }
    }

    private fun startChartLoadingTimeout() {
        cancelChartLoadingTimeout()
        chartLoadingTimeoutJob = viewModelScope.launch {
            delay(5000) // 5초 타임아웃
            _uiState.update {
                it.copy(
                    isLoading = false,
                    chartLoadingStage = ChartLoadingStage.CHART_READY,
                    errorMessage = "차트 로딩 시간이 초과되었습니다. 네트워크 상태를 확인해주세요."
                )
            }
        }
    }

    private fun cancelChartLoadingTimeout() {
        chartLoadingTimeoutJob?.cancel()
        chartLoadingTimeoutJob = null
    }

    override fun onCleared() {
        super.onCleared()
        historyChallengeWebSocket?.cancel()
        chartLoadingTimeoutJob?.cancel()
    }
}