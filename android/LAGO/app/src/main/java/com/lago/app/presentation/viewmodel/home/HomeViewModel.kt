package com.lago.app.presentation.viewmodel.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.data.cache.RealTimeStockCache
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.domain.repository.UserRepository
import com.lago.app.data.remote.dto.MyPagePortfolioSummary
import com.lago.app.data.remote.dto.UserCurrentStatusDto
import com.lago.app.data.remote.dto.HoldingResponseDto
import com.lago.app.data.remote.websocket.SmartStockWebSocketService
import com.lago.app.data.scheduler.SmartUpdateScheduler
import com.lago.app.data.service.CloseDataService
import com.lago.app.util.MarketTimeUtils
import com.lago.app.util.PortfolioCalculator
import com.lago.app.util.HybridPriceCalculator
import com.lago.app.util.Resource
import com.skydoves.flexible.core.log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

/**
 * 홈 화면용 주식 데이터
 */
data class HomeStock(
    val stockCode: String,
    val stockName: String,
    val quantity: Int,
    val totalPurchaseAmount: Long,
    val currentPrice: Double?,
    val profitLoss: Long,
    val profitRate: Double
)

/**
 * 홈 화면 UI 상태
 */
data class HomeUiState(
    val portfolioSummary: MyPagePortfolioSummary? = null,
    val stockList: List<HomeStock> = emptyList(),
    val tradingBots: List<com.lago.app.presentation.ui.TradingBot> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val refreshing: Boolean = false,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    private val realTimeStockCache: RealTimeStockCache,
    private val smartWebSocketService: SmartStockWebSocketService,
    private val smartUpdateScheduler: SmartUpdateScheduler,
    private val closeDataService: CloseDataService,
    private val hybridPriceCalculator: HybridPriceCalculator,
    private val initialPriceService: com.lago.app.data.service.InitialPriceService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    // 캐시된 API 데이터
    private var cachedUserStatus: UserCurrentStatusDto? = null
    
    // 초기 종가 캐시 (종목코드 -> 종가)
    private var cachedClosePrices: Map<String, Double> = emptyMap()
    
    // 역사챌린지 가격 데이터
    private var historyChallengePrice: Map<String, Double> = emptyMap()

    init {
        checkLoginStatus()
        observeRealTimeUpdates()
        loadUserPortfolio()
        loadTradingBots()
        
        // WebSocket 연결 시작
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "🔌 HomeViewModel WebSocket 연결 시작")
            smartWebSocketService.connect()
        }
        
        // WebSocket 연결 상태 모니터링 추가
        viewModelScope.launch {
            smartWebSocketService.connectionState.collect { state ->
                android.util.Log.d("HomeViewModel", "🔗 WebSocket 연결 상태: $state")
                android.util.Log.d("HomeViewModel", "📊 구독 통계: ${smartWebSocketService.getSubscriptionStats()}")
            }
        }
    }

    /**
     * 로그인 상태 확인
     */
    private fun checkLoginStatus() {
        val isLoggedIn = userPreferences.getAccessToken() != null
        _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
        
        if (isLoggedIn) {
            loadUserPortfolio()
        }
    }

    /**
     * 사용자 포트폴리오 로드 (API 호출)
     */
    fun loadUserPortfolio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val userId = userPreferences.getUserIdLong().toInt()
                val type = userPreferences.getInvestmentMode() // 저장된 투자 모드 사용
                android.util.Log.d("HomeViewModel", "📡 API 요청 시작: userId=$userId, type=$type")
                android.util.Log.d("HomeViewModel", "🔍 현재 투자모드 상세: ${if (type == 1) "역사챌린지" else "모의투자"}")
                android.util.Log.d("HomeViewModel", "🎯 UserPreferences 저장값: ${userPreferences.getInvestmentMode()}")

                userRepository.getUserCurrentStatus(userId, type).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            android.util.Log.d("HomeViewModel", "⏳ API 로딩 중...")
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            android.util.Log.d("HomeViewModel", "✅ API 성공: ${resource.data}")
                            val userStatus = resource.data!!
                            cachedUserStatus = userStatus
                            
                            // 보유 종목들을 WebSocket 구독 목록에 추가
                            val stockCodes = userStatus.holdings.map { it.stockCode }
                            smartWebSocketService.updatePortfolioStocks(stockCodes)
                            
                            // 투자 모드에 따라 가격 데이터 초기화
                            val type = userPreferences.getInvestmentMode()
                            if (type == 1) {
                                // 역사챌린지 모드: 역사챌린지 가격 사용
                                loadHistoryChallengePrice(stockCodes, userStatus)
                            } else {
                                // 모의투자 모드: REST API로 초기 가격 설정 후 실시간 업데이트
                                android.util.Log.d("HomeViewModel", "💼 모의투자 모드: REST API로 초기 데이터 로드")
                                android.util.Log.d("HomeViewModel", "📋 모의투자 보유종목: ${stockCodes.joinToString()}")
                                loadInitialPricesFromApi(stockCodes, userStatus)
                            }
                        }
                        is Resource.Error -> {
                            android.util.Log.e("HomeViewModel", "❌ API 에러: ${resource.message}")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    errorMessage = resource.message
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "💥 예외 발생: ${e.localizedMessage}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "포트폴리오 로드 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * REST API로 초기 가격 로드 (모의투자용)
     */
    private fun loadInitialPricesFromApi(stockCodes: List<String>, userStatus: UserCurrentStatusDto) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "📡 REST API로 최신 종가 조회 시작: ${stockCodes.size}개 종목")
                android.util.Log.d("HomeViewModel", "📋 대상 종목들: ${stockCodes.joinToString()}")
                stockCodes.forEachIndexed { index, code ->
                    android.util.Log.d("HomeViewModel", "🔍 종목[$index]: '$code' (타입: ${code.javaClass.simpleName}, 길이: ${code.length})")
                }
                
                @Suppress("NewApi")
                val initialPrices = initialPriceService.getLatestClosePrices(stockCodes)
                
                android.util.Log.d("HomeViewModel", "📊 REST API 응답 확인: ${initialPrices.size}개 종목 가격 수신")
                
                android.util.Log.d("HomeViewModel", "✅ 초기 가격 조회 완료: ${initialPrices.size}/${stockCodes.size}개 성공")
                
                // 종가를 캐시에 저장 (Double 형태로)
                cachedClosePrices = initialPrices.mapValues { (stockCode, closePrice) ->
                    android.util.Log.d("HomeViewModel", "💰 $stockCode 초기 종가 캐시: ${closePrice}원")
                    closePrice.toDouble()
                }
                
                // 가격 정보를 StockRealTimeData 형태로 변환
                val initialRealTimePrices = initialPrices.mapValues { (stockCode, closePrice) ->
                    com.lago.app.domain.entity.StockRealTimeData(
                        stockCode = stockCode,
                        closePrice = closePrice.toLong(),
                        tradePrice = closePrice.toLong(),
                        currentPrice = closePrice.toLong(),
                        changePrice = 0L,
                        changeRate = 0.0
                    )
                }
                
                // 초기 포트폴리오 계산
                updatePortfolioWithInitialPrices(userStatus, initialRealTimePrices)
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ REST API 초기 가격 로드 실패", e)
                // 실패 시 기존 하이브리드 방식으로 폴백
                updatePortfolioWithCloseData(userStatus)
            }
        }
    }
    
    /**
     * 초기 가격으로 포트폴리오 계산
     */
    private fun updatePortfolioWithInitialPrices(userStatus: UserCurrentStatusDto, initialPrices: Map<String, com.lago.app.domain.entity.StockRealTimeData>) {
        try {
            // 포트폴리오 계산
            val portfolioSummary = com.lago.app.util.PortfolioCalculator.calculateMyPagePortfolio(
                userStatus = userStatus,
                realTimePrices = initialPrices
            )
            
            // 홈 화면용 주식 데이터 생성
            val stockList = userStatus.holdings.map { holding ->
                val initialPrice = initialPrices[holding.stockCode]
                createHomeStock(holding, initialPrice?.price?.toDouble())
            }
            
            // UI 상태 업데이트
            _uiState.update { 
                it.copy(
                    portfolioSummary = portfolioSummary,
                    stockList = stockList,
                    isLoading = false,
                    errorMessage = null
                )
            }
            
            android.util.Log.d("HomeViewModel", "✅ 초기 가격 기반 포트폴리오 계산 완료")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "💥 초기 가격 포트폴리오 계산 오류: ${e.localizedMessage}", e)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = "초기 데이터 로드 중 오류가 발생했습니다: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * 역사챌린지 가격 로드
     */
    private fun loadHistoryChallengePrice(stockCodes: List<String>, userStatus: UserCurrentStatusDto) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "🏛️ 역사챌린지 가격 로드 시작")
                
                userRepository.getHistoryChallenge().collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val challengeData = resource.data!!
                            android.util.Log.d("HomeViewModel", "✅ 역사챌린지 데이터: ${challengeData.stockName} - ${challengeData.currentPrice}원")
                            
                            // 역사챌린지 종목의 가격을 맵에 저장
                            historyChallengePrice = mapOf(
                                challengeData.stockCode to challengeData.currentPrice.toDouble()
                            )
                            
                            // 역사챌린지 WebSocket 구독 시작
                            android.util.Log.d("HomeViewModel", "🏛️ 역사챌린지 WebSocket 구독 시작")
                            smartWebSocketService.subscribeToHistoryChallenge(challengeData.stockCode)
                            
                            // 역사챌린지 가격으로 포트폴리오 계산
                            updatePortfolioWithHistoryPrice(userStatus)
                        }
                        is Resource.Error -> {
                            android.util.Log.e("HomeViewModel", "❌ 역사챌린지 가격 로드 실패: ${resource.message}")
                            // 실패 시 기본 가격 계산으로 폴백
                            updatePortfolioWithCloseData(userStatus)
                        }
                        is Resource.Loading -> {
                            android.util.Log.d("HomeViewModel", "⏳ 역사챌린지 가격 로딩 중...")
                        }
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "💥 역사챌린지 가격 로드 예외: ${e.localizedMessage}", e)
                // 실패 시 기본 가격 계산으로 폴백
                updatePortfolioWithCloseData(userStatus)
            }
        }
    }
    
    /**
     * 역사챌린지 가격으로 포트폴리오 계산
     */
    private fun updatePortfolioWithHistoryPrice(userStatus: UserCurrentStatusDto) {
        try {
            android.util.Log.d("HomeViewModel", "🏛️ 역사챌린지 가격으로 포트폴리오 계산 시작")
            
            // 홈 화면용 주식 데이터 생성 (역사챌린지 가격 사용)
            val stockList = userStatus.holdings.map { holding ->
                val historyPrice = historyChallengePrice[holding.stockCode]
                android.util.Log.d("HomeViewModel", "📈 ${holding.stockName}: 역사가격 ${historyPrice}원")
                createHomeStock(holding, historyPrice)
            }
            
            // MyPagePortfolioSummary도 역사가격으로 계산
            val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                userStatus = userStatus,
                realTimePrices = historyChallengePrice.mapValues { (stockCode, price) ->
                    com.lago.app.domain.entity.StockRealTimeData(
                        stockCode = stockCode,
                        closePrice = price.toLong(),
                        tradePrice = price.toLong(),
                        currentPrice = price.toLong(),
                        changePrice = 0L,
                        changeRate = 0.0
                    )
                }
            )
            
            // UI 상태 업데이트
            _uiState.update { 
                it.copy(
                    portfolioSummary = portfolioSummary,
                    stockList = stockList,
                    isLoading = false,
                    errorMessage = null
                )
            }
            
            android.util.Log.d("HomeViewModel", "✅ 역사챌린지 포트폴리오 계산 완료")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "💥 역사챌린지 포트폴리오 계산 오류: ${e.localizedMessage}", e)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = "역사챌린지 포트폴리오 계산 중 오류가 발생했습니다: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * 실시간 데이터 업데이트 감시 (성능 최적화)
     */
    private fun observeRealTimeUpdates() {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "🔌 observeRealTimeUpdates 시작 - 소켓 감시 중...")

            realTimeStockCache.quotes
                .sample(1000.milliseconds)
                .collect { quotesMap ->
                    android.util.Log.d("HomeViewModel", "🔥 소켓 데이터 수신! 종목 수: ${quotesMap.size}")

                    val userStatus = cachedUserStatus
                    if (userStatus == null) {
                        android.util.Log.w("HomeViewModel", "⚠️ cachedUserStatus가 null - 소켓 데이터 무시")
                        return@collect
                    }

                    val investmentMode = userPreferences.getInvestmentMode()

                    if (quotesMap.isEmpty()) {
                        android.util.Log.w("HomeViewModel", "⚠️ 빈 소켓 데이터 수신 - 캐시 기반 처리")
                        when (investmentMode) {
                            1 -> { // 역사
                                if (historyChallengePrice.isNotEmpty()) {
                                    updatePortfolioWithHistoryPrice(userStatus)
                                }
                            }
                            else -> { // 모의
                                if (cachedClosePrices.isNotEmpty()) {
                                    updatePortfolioWithCachedPrices(userStatus)
                                }
                            }
                        }
                        return@collect
                    }

                    when (investmentMode) {
                        1 -> {
                            // 역사: 역사 소켓 키 규칙 그대로 사용
                            if (historyChallengePrice.isNotEmpty()) {
                                updateHistoryPriceWithRealTime(quotesMap, userStatus)
                            } else {
                                android.util.Log.w("HomeViewModel", "🏛️ 역사챌린지 모드: 역사가격 데이터 없음 - 스킵")
                            }
                        }
                        else -> {
                            // 모의: 종가 캐시 + 일반 소켓 합성
                            updatePortfolioWithRealTimeData(userStatus)
                        }
                    }
                }
        }
    }


    /**
     * 역사챌린지 가격을 실시간 데이터로 업데이트
     */
    private fun updateHistoryPriceWithRealTime(realTimeQuotes: Map<String, com.lago.app.domain.entity.StockRealTimeData>, userStatus: UserCurrentStatusDto) {
        try {
            android.util.Log.d("HomeViewModel", "🏛️📡 역사챌린지 실시간 가격 업데이트 시작")
            android.util.Log.d("HomeViewModel", "📦 받은 소켓 데이터: ${realTimeQuotes.size}개 종목")
            android.util.Log.d("HomeViewModel", "🗃️ 현재 역사챌린지 가격 맵: ${historyChallengePrice.size}개 종목")
            
            // 소켓에서 받은 모든 종목 로그
            realTimeQuotes.forEach { (code, data) ->
                android.util.Log.d("HomeViewModel", "📡 소켓 데이터: $code = ${data.closePrice}원")
            }
            
            // 현재 역사챌린지 가격 맵 로그
            historyChallengePrice.forEach { (code, price) ->
                android.util.Log.d("HomeViewModel", "🏛️ 역사가격: $code = ${price}원")
            }
            
            // 사용자 보유종목 로그
            android.util.Log.d("HomeViewModel", "💼 보유종목: ${userStatus.holdings.map { "${it.stockCode}(${it.stockName})" }.joinToString()}")
            
            // 기존 역사챌린지 가격을 실시간 데이터로 업데이트
            val updatedHistoryPrice = historyChallengePrice.toMutableMap()
            
            // 역사챌린지 전용 키로 데이터 조회
            historyChallengePrice.keys.forEach { stockCode ->
                val historyChallengeKey = "HISTORY_CHALLENGE_$stockCode"
                val realTimeData = realTimeQuotes[historyChallengeKey]
                
                if (realTimeData != null) {
                    val oldPrice = updatedHistoryPrice[stockCode]
                    val newPrice = realTimeData.closePrice?.toDouble()
                    if (newPrice != null) {
                        updatedHistoryPrice[stockCode] = newPrice
                        android.util.Log.d("HomeViewModel", "📈 $stockCode 가격 업데이트: ${oldPrice}원 → ${newPrice}원 (키: $historyChallengeKey)")
                    }
                } else {
                    android.util.Log.d("HomeViewModel", "❌ $stockCode 역사챌린지 소켓 데이터 없음 (키: $historyChallengeKey)")
                }
            }
            
            historyChallengePrice = updatedHistoryPrice
            
            // 업데이트된 역사챌린지 가격으로 포트폴리오 재계산
            updatePortfolioWithHistoryPrice(userStatus)
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "💥 역사챌린지 실시간 업데이트 오류: ${e.localizedMessage}", e)
        }
    }

    /**
     * 실시간 데이터와 결합하여 포트폴리오 계산 (폴백용)
     */
    private fun updatePortfolioWithRealTimeData(userStatus: UserCurrentStatusDto) {
        viewModelScope.launch {
            try {
                val quotes = realTimeStockCache.quotes.value
                val useCachedBase = cachedClosePrices.isNotEmpty()

                // 1) 모드 확인
                val mode = userPreferences.getInvestmentMode()
                if (mode == 1) {
                    // 안전가드: 역사 모드에서는 이 함수가 호출되지 않는 게 정상
                    android.util.Log.w("HomeViewModel", "⚠️ 역사 모드에서 updatePortfolioWithRealTimeData 호출됨 - 무시")
                    updatePortfolioWithHistoryPrice(userStatus)
                    return@launch
                }

                // 2) 합성 가격 맵 생성 (모의)
                //    - 종가 캐시가 있으면: 소켓 현재가가 있으면 그걸 사용, 없으면 종가
                //    - 종가 캐시가 비어있으면: 소켓 현재가만 사용(있을 때), 없으면 빈 맵
                val merged: Map<String, com.lago.app.domain.entity.StockRealTimeData> = buildMap {
                    val codes = userStatus.holdings.map { it.stockCode }.toSet()

                    if (useCachedBase) {
                        // 종가 기반으로 초기화
                        codes.forEach { code ->
                            val baseClose = cachedClosePrices[code]
                            if (baseClose != null) {
                                put(
                                    code,
                                    com.lago.app.domain.entity.StockRealTimeData(
                                        stockCode = code,
                                        closePrice = baseClose.toLong(),
                                        tradePrice = baseClose.toLong(),
                                        currentPrice = baseClose.toLong(),
                                        changePrice = 0L,
                                        changeRate = 0.0
                                    )
                                )
                            }
                        }
                    }

                    // 소켓 실시간 반영: 있으면 덮어씀
                    codes.forEach { code ->
                        val rt = quotes[code]
                        if (rt != null) {
                            put(code, rt)
                        }
                    }
                }

                // 3) 포트폴리오 계산 및 UI 반영
                val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                    userStatus = userStatus,
                    realTimePrices = merged
                )

                val stockList = userStatus.holdings.map { holding ->
                    // 표시용 현재가: 소켓이 있으면 소켓 가격, 없으면 종가
                    val price =
                        quotes[holding.stockCode]?.price
                            ?: cachedClosePrices[holding.stockCode]
                    createHomeStock(holding, price)
                }

                _uiState.update {
                    it.copy(
                        portfolioSummary = portfolioSummary,
                        stockList = stockList,
                        isLoading = false,
                        errorMessage = null
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "💥 실시간 데이터 처리 오류: ${e.localizedMessage}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "실시간 데이터 처리 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            }
        }
    }


    /**
     * 캐시된 종가를 사용해서 포트폴리오 계산
     */
    private fun updatePortfolioWithCachedPrices(userStatus: UserCurrentStatusDto) {
        try {
            android.util.Log.d("HomeViewModel", "💾 캐시된 종가로 포트폴리오 계산 시작")
            android.util.Log.d("HomeViewModel", "📋 캐시된 종가: ${cachedClosePrices.size}개 종목")
            
            if (cachedClosePrices.isEmpty()) {
                android.util.Log.w("HomeViewModel", "⚠️ 캐시된 종가 없음 - API로 종가 조회")
                updatePortfolioWithCloseData(userStatus)
                return
            }
            
            // 캐시된 종가를 실시간 가격 형태로 변환
            val cachedRealTimePrices = cachedClosePrices.mapValues { (stockCode, closePrice) ->
                android.util.Log.v("HomeViewModel", "💾 $stockCode 캐시 종가 사용: ${closePrice}원")
                com.lago.app.domain.entity.StockRealTimeData(
                    stockCode = stockCode,
                    closePrice = closePrice.toLong(),
                    tradePrice = closePrice.toLong(),
                    currentPrice = closePrice.toLong(),
                    changePrice = 0L,
                    changeRate = 0.0
                )
            }
            
            // 포트폴리오 계산 (캐시된 종가 기준)
            val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                userStatus = userStatus,
                realTimePrices = cachedRealTimePrices
            )
            
            // 홈 화면용 주식 데이터 생성 (캐시된 종가 기준)
            val stockList = userStatus.holdings.map { holding ->
                val cachedPrice = cachedClosePrices[holding.stockCode]
                createHomeStock(holding, cachedPrice)
            }
            
            // UI 상태 업데이트
            _uiState.update { 
                it.copy(
                    portfolioSummary = portfolioSummary,
                    stockList = stockList,
                    isLoading = false,
                    errorMessage = null
                )
            }
            
            android.util.Log.d("HomeViewModel", "✅ 캐시된 종가 기준 포트폴리오 계산 완료")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "❌ 캐시 데이터 처리 오류: ${e.localizedMessage}", e)
            // 캐시 실패 시 API로 폴백
            updatePortfolioWithCloseData(userStatus)
        }
    }

    /**
     * API로 종가 조회해서 포트폴리오 계산 (폴백용)
     */
    private fun updatePortfolioWithCloseData(userStatus: UserCurrentStatusDto) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "🔒 API로 종가 조회 - 포트폴리오 계산 시작")
                
                // 보유 종목 코드 리스트
                val stockCodes = userStatus.holdings.map { it.stockCode }
                android.util.Log.d("HomeViewModel", "📋 종가 조회 대상: ${stockCodes.joinToString()}")
                
                // InitialPriceService로 최신 종가 조회 (더 안정적)
                val closePrices = try {
                    val apiResult = initialPriceService.getLatestClosePrices(stockCodes)
                    // API 결과를 캐시에도 저장
                    cachedClosePrices = apiResult.mapValues { it.value.toDouble() }
                    android.util.Log.d("HomeViewModel", "💾 API 결과를 캐시에 저장: ${cachedClosePrices.size}개 종목")
                    apiResult
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "❌ InitialPriceService 실패, CloseDataService로 폴백", e)
                    // 폴백: 기존 CloseDataService 사용
                    closeDataService.getPortfolioClosePrices(stockCodes)
                }
                
                android.util.Log.d("HomeViewModel", "💰 종가 데이터: ${closePrices.size}개 종목 가격 확보")
                
                // 종가를 실시간 가격 형태로 변환
                val closeRealTimePrices = closePrices.mapValues { (stockCode, closePrice) ->
                    android.util.Log.d("HomeViewModel", "💰 $stockCode API 종가: ${closePrice}원")
                    com.lago.app.domain.entity.StockRealTimeData(
                        stockCode = stockCode,
                        closePrice = closePrice.toLong(),
                        tradePrice = closePrice.toLong(),
                        currentPrice = closePrice.toLong(),
                        changePrice = 0L,
                        changeRate = 0.0
                    )
                }
                
                // 포트폴리오 계산 (종가 기준)
                val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                    userStatus = userStatus,
                    realTimePrices = closeRealTimePrices
                )
                
                // 홈 화면용 주식 데이터 생성 (종가 기준)
                val stockList = userStatus.holdings.map { holding ->
                    val closePrice = closePrices[holding.stockCode]?.toDouble()
                    createHomeStock(holding, closePrice)
                }
                
                // UI 상태 업데이트
                _uiState.update { 
                    it.copy(
                        portfolioSummary = portfolioSummary,
                        stockList = stockList,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                
                android.util.Log.d("HomeViewModel", "✅ API 종가 기준 포트폴리오 계산 완료")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ 종가 데이터 처리 오류: ${e.localizedMessage}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "종가 데이터 처리 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * 홈 화면용 주식 데이터 생성
     */
    private fun createHomeStock(holding: HoldingResponseDto, currentPrice: Double?): HomeStock {
        val realPrice = currentPrice ?: (holding.totalPurchaseAmount.toDouble() / holding.quantity)
        val currentValue = realPrice * holding.quantity
        val profitLoss = currentValue.toLong() - holding.totalPurchaseAmount
        val profitRate = if (holding.totalPurchaseAmount > 0) {
            (profitLoss.toDouble() / holding.totalPurchaseAmount) * 100
        } else 0.0

        android.util.Log.d("HomeViewModel", "🏠 ${holding.stockName}: ${holding.quantity}개, 총매수 ${holding.totalPurchaseAmount}원, 현재가 ${realPrice}원, 평가손익 ${profitLoss}원")

        return HomeStock(
            stockCode = holding.stockCode,
            stockName = holding.stockName,
            quantity = holding.quantity,
            totalPurchaseAmount = holding.totalPurchaseAmount,
            currentPrice = currentPrice,
            profitLoss = profitLoss,
            profitRate = profitRate
        )
    }

    /**
     * 매매봇 데이터 로드 (type=2로 각 봇의 데이터 조회)
     */
    fun loadTradingBots() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "🤖 매매봇 데이터 로드 시작")
                
                // AI 봇 ID 리스트 (1~4번)
                val botIds = listOf(1, 2, 3, 4)
                val botData = mutableListOf<com.lago.app.presentation.ui.TradingBot>()
                
                // 각 봇의 데이터를 type=2로 조회
                for (botId in botIds) {
                    try {
                        userRepository.getUserCurrentStatus(botId, 2).collect { resource ->
                            when (resource) {
                                is Resource.Success -> {
                                    val userStatus = resource.data!!
                                    
                                    // 매매봇 보유 종목들을 WebSocket 구독에 추가
                                    val botStockCodes = userStatus.holdings.map { it.stockCode }
                                    if (botStockCodes.isNotEmpty()) {
                                        smartWebSocketService.updatePortfolioStocks(botStockCodes)
                                        android.util.Log.d("HomeViewModel", "🤖📡 봇 $botId 종목 WebSocket 구독: ${botStockCodes.joinToString()}")
                                    }
                                    
                                    // 🔄 매매봇 종목들의 하이브리드 가격 계산 (종가 기준)
                                    val botHybridPrices = try {
                                        val initialResult = hybridPriceCalculator.calculateInitialPrices(botStockCodes)
                                        android.util.Log.d("HomeViewModel", "🤖💰 봇 $botId 하이브리드 가격 계산: ${initialResult.successCount}/${botStockCodes.size}개 성공")
                                        initialResult.prices
                                    } catch (e: Exception) {
                                        android.util.Log.e("HomeViewModel", "🤖❌ 봇 $botId 하이브리드 가격 계산 실패", e)
                                        emptyMap()
                                    }
                                    
                                    // 📊 PortfolioCalculator 사용해서 유저 모의투자와 동일하게 계산
                                    val realTimePrices = if (botHybridPrices.isNotEmpty()) {
                                        // 매매봇 전용 하이브리드 가격을 실시간 데이터로 변환
                                        hybridPriceCalculator.toStockRealTimeDataMap(botHybridPrices)
                                    } else {
                                        // 폴백: 실시간 캐시 사용
                                        realTimeStockCache.quotes.value
                                    }
                                    
                                    val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                                        userStatus = userStatus,
                                        realTimePrices = realTimePrices
                                    )
                                    
                                    val totalAssets = userStatus.balance + portfolioSummary.totalCurrentValue
                                    val profitLoss = totalAssets - 1000000 // 초기자금 1000만원 가정
                                    val profitRate = if (totalAssets > 0) (profitLoss.toDouble() / 1000000) * 100 else 0.0
                                    
                                    android.util.Log.d("HomeViewModel", "🤖💰 봇 $botId 자산 계산 (PortfolioCalculator 사용):")
                                    android.util.Log.d("HomeViewModel", "   잔고: ${userStatus.balance}")
                                    android.util.Log.d("HomeViewModel", "   보유종목 평가액: ${portfolioSummary.totalCurrentValue}")
                                    android.util.Log.d("HomeViewModel", "   총자산: $totalAssets")
                                    android.util.Log.d("HomeViewModel", "   매매봇 하이브리드 가격 사용: ${botHybridPrices.isNotEmpty()}")
                                    android.util.Log.d("HomeViewModel", "   실시간 가격 데이터: ${realTimePrices.size}개")
                                    
                                    // 봇 정보 생성
                                    val bot = when (botId) {
                                        1 -> com.lago.app.presentation.ui.TradingBot(
                                            1, "화끈이", com.lago.app.R.drawable.character_red,
                                            formatAmount(totalAssets),
                                            "${if (profitLoss >= 0) "+" else ""}${formatAmount(profitLoss)}",
                                            "${if (profitRate >= 0) "+" else ""}${String.format("%.2f", profitRate)}%",
                                            "공격투자형"
                                        )
                                        2 -> com.lago.app.presentation.ui.TradingBot(
                                            2, "적극이", com.lago.app.R.drawable.character_yellow,
                                            formatAmount(totalAssets),
                                            "${if (profitLoss >= 0) "+" else ""}${formatAmount(profitLoss)}",
                                            "${if (profitRate >= 0) "+" else ""}${String.format("%.2f", profitRate)}%",
                                            "적극투자형"
                                        )
                                        3 -> com.lago.app.presentation.ui.TradingBot(
                                            3, "균형이", com.lago.app.R.drawable.character_blue,
                                            formatAmount(totalAssets),
                                            "${if (profitLoss >= 0) "+" else ""}${formatAmount(profitLoss)}",
                                            "${if (profitRate >= 0) "+" else ""}${String.format("%.2f", profitRate)}%",
                                            "위험중립형"
                                        )
                                        4 -> com.lago.app.presentation.ui.TradingBot(
                                            4, "조심이", com.lago.app.R.drawable.character_green,
                                            formatAmount(totalAssets),
                                            "${if (profitLoss >= 0) "+" else ""}${formatAmount(profitLoss)}",
                                            "${if (profitRate >= 0) "+" else ""}${String.format("%.2f", profitRate)}%",
                                            "안정추구형"
                                        )
                                        else -> return@collect
                                    }
                                    
                                    botData.add(bot)
                                    android.util.Log.d("HomeViewModel", "🤖 봇 $botId 데이터 로드 완료: ${bot.amount}")
                                    
                                    // 모든 봇 데이터가 로드되면 UI 업데이트
                                    if (botData.size == botIds.size) {
                                        _uiState.update { it.copy(tradingBots = botData.sortedBy { bot -> bot.id }) }
                                        android.util.Log.d("HomeViewModel", "✅ 모든 매매봇 데이터 로드 완료")
                                    }
                                }
                                is Resource.Error -> {
                                    android.util.Log.e("HomeViewModel", "❌ 봇 $botId 데이터 로드 실패: ${resource.message}")
                                }
                                is Resource.Loading -> {
                                    android.util.Log.d("HomeViewModel", "⏳ 봇 $botId 데이터 로딩 중...")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "💥 봇 $botId 데이터 로드 예외: ${e.localizedMessage}", e)
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "💥 매매봇 데이터 로드 전체 실패: ${e.localizedMessage}", e)
                // 실패 시 기본 데이터 사용
                loadDefaultTradingBots()
            }
        }
    }
    
    /**
     * 기본 매매봇 데이터 로드 (API 실패 시)
     */
    private fun loadDefaultTradingBots() {
        val defaultBots = listOf(
            com.lago.app.presentation.ui.TradingBot(1, "화끈이", com.lago.app.R.drawable.character_red, "12,450,000원", "+137,000원", "2.56%", "공격투자형"),
            com.lago.app.presentation.ui.TradingBot(2, "적극이", com.lago.app.R.drawable.character_yellow, "8,750,000원", "+25,000원", "1.2%", "적극투자형"),
            com.lago.app.presentation.ui.TradingBot(3, "균형이", com.lago.app.R.drawable.character_blue, "15,200,000원", "-45,000원", "0.8%", "위험중립형"),
            com.lago.app.presentation.ui.TradingBot(4, "조심이", com.lago.app.R.drawable.character_green, "6,800,000원", "+12,000원", "0.4%", "안정추구형")
        )
        _uiState.update { it.copy(tradingBots = defaultBots) }
        android.util.Log.d("HomeViewModel", "🔄 기본 매매봇 데이터 로드")
    }

    /**
     * 새로고침
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }

            android.util.Log.d("HomeViewModel", "🔄 새로고침: ${MarketTimeUtils.getMarketStatusString()}")

            val mode = userPreferences.getInvestmentMode()
            if (mode == 1) {
                historyChallengePrice = emptyMap()
            } else {
                cachedClosePrices = emptyMap()
            }

            loadUserPortfolio()
            loadTradingBots() // 봇은 유지
            _uiState.update { it.copy(refreshing = false) }
        }
    }


    /**
     * 투자 모드 변경 시 호출 (스위치 변경)
     */
    fun onInvestmentModeChanged() {
        android.util.Log.d("HomeViewModel", "🔄 투자 모드 변경 - 계좌 스위치 감지")

        cachedUserStatus?.let { userStatus ->
            val stockCodes = userStatus.holdings.map { it.stockCode }
            val newType = userPreferences.getInvestmentMode()

            android.util.Log.d("HomeViewModel", "🎯 모드 변경: type=$newType (${if (newType == 1) "역사챌린지" else "모의투자"})")

            _uiState.update { it.copy(isLoading = true) }

            if (newType == 1) {
                // 역사모드: 역사 가격 로드, 모의 캐시는 보존
                historyChallengePrice = emptyMap()
                smartWebSocketService.subscribeToHistoryChallenge(null.toString()) // 필요한 경우 재구독 준비
                loadHistoryChallengePrice(stockCodes, userStatus)
            } else {
                // 모의모드: 역사 데이터 클리어, 캐시 종가 즉시 사용 (없으면 API)
                smartWebSocketService.unsubscribeFromHistoryChallenge()
                historyChallengePrice = emptyMap()

                if (cachedClosePrices.isNotEmpty()) {
                    updatePortfolioWithCachedPrices(userStatus)
                } else {
                    updatePortfolioWithCloseData(userStatus)
                }
            }
        } ?: run {
            // 캐시된 상태가 없으면 전체 로드
            android.util.Log.d("HomeViewModel", "📡 캐시 없음 - 전체 포트폴리오 재로드")
            loadUserPortfolio()
        }
    }


    /**
     * 로그인 후 호출
     */
    fun onLoginSuccess() {
        checkLoginStatus()
    }

    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 포맷팅된 금액 반환
     */
    fun formatAmount(amount: Long): String {
        return String.format("%,d원", amount)
    }

    /**
     * 수익률 색상 반환
     */
    fun getProfitLossColor(profitLoss: Long): androidx.compose.ui.graphics.Color {
        return when {
            profitLoss > 0 -> androidx.compose.ui.graphics.Color.Red
            profitLoss < 0 -> androidx.compose.ui.graphics.Color.Blue
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }

    /**
     * 수익률 텍스트 반환
     */
    fun formatProfitLoss(profitLoss: Long, profitRate: Double): String {
        val sign = if (profitLoss > 0) "+" else ""
        return "${sign}${formatAmount(profitLoss)} (${sign}${String.format("%.2f", profitRate)}%)"
    }
}