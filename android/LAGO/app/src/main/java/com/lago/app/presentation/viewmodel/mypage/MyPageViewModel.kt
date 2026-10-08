package com.lago.app.presentation.viewmodel.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.data.cache.RealTimeStockCache
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.domain.repository.UserRepository
import com.lago.app.data.remote.dto.MyPagePortfolioSummary
import com.lago.app.data.remote.dto.PieChartItem
import com.lago.app.data.remote.dto.UserCurrentStatusDto
import com.lago.app.data.remote.dto.HoldingResponseDto
import com.lago.app.data.remote.websocket.SmartStockWebSocketService
import com.lago.app.data.scheduler.SmartUpdateScheduler
import com.lago.app.domain.entity.ScreenType
import com.lago.app.util.PortfolioCalculator
import com.lago.app.util.HybridPriceCalculator
import com.lago.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

/**
 * 마이페이지 UI 상태
 */
data class MyPageUiState(
    val portfolioSummary: MyPagePortfolioSummary? = null,
    val pieChartData: List<PieChartItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val refreshing: Boolean = false,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences,
    private val realTimeStockCache: RealTimeStockCache,
    private val smartWebSocketService: SmartStockWebSocketService,
    private val smartUpdateScheduler: SmartUpdateScheduler,
    private val hybridPriceCalculator: HybridPriceCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState

    // 캐시된 API 데이터
    private var cachedUserStatus: UserCurrentStatusDto? = null
    
    // 하이브리드 가격 데이터
    private var hybridPrices: Map<String, HybridPriceCalculator.HybridPriceData> = emptyMap()
    
    // 매매봇용 플래그 - 매매봇용일 때는 init에서 기본 로드하지 않음
    private var isBotMode = false

    init {
        // 매매봇 모드가 아닐 때만 기본 로드
        if (!isBotMode) {
            checkLoginStatus()
            observeRealTimeUpdates()
            // 임시 테스트용: 항상 API 호출
            loadUserPortfolio()
        }
    }
    
    /**
     * 매매봇 모드로 설정
     */
    fun setBotMode() {
        isBotMode = true
        observeRealTimeUpdates() // 실시간 업데이트는 유지
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
     * 매매봇용 포트폴리오 로드 (API 호출)
     */
    fun loadUserPortfolioForBot(userId: Int) {
        viewModelScope.launch {
            // 상태 완전 리셋
            cachedUserStatus = null
            _uiState.value = MyPageUiState(isLoading = true)
            
            try {
                android.util.Log.d("MyPageViewModel", "🤖 매매봇 API 요청 시작: userId=$userId")

                userRepository.getUserCurrentStatus(userId).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            android.util.Log.d("MyPageViewModel", "⏳ 매매봇 API 로딩 중...")
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            android.util.Log.d("MyPageViewModel", "✅ 매매봇 API 성공: ${resource.data}")
                            val userStatus = resource.data!!
                            cachedUserStatus = userStatus
                            
                            // 보유 종목들을 WebSocket 구독 목록에 추가
                            val stockCodes = userStatus.holdings.map { it.stockCode }
                            smartWebSocketService.updatePortfolioStocks(stockCodes)
                            
                            // 하이브리드 가격 계산으로 초기 포트폴리오 계산
                            initializeHybridPrices(stockCodes, userStatus)
                        }
                        is Resource.Error -> {
                            android.util.Log.e("MyPageViewModel", "❌ 매매봇 API 에러: ${resource.message}")
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
                android.util.Log.e("MyPageViewModel", "💥 매매봇 예외 발생: ${e.localizedMessage}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "매매봇 데이터 로드 중 오류가 발생했습니다: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * 사용자 포트폴리오 로드 (API 호출)
     */
    fun loadUserPortfolio() {
        // 임시 테스트용: 로그인 체크 비활성화
        // if (!_uiState.value.isLoggedIn) {
        //     _uiState.update { 
        //         it.copy(errorMessage = "로그인이 필요합니다.") 
        //     }
        //     return
        // }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                val userId = userPreferences.getUserIdLong().toInt()
                android.util.Log.d("MyPageViewModel", "📡 API 요청 시작: userId=$userId")

                // 실제 API 호출
                userRepository.getUserCurrentStatus(userId).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            android.util.Log.d("MyPageViewModel", "⏳ API 로딩 중...")
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is Resource.Success -> {
                            android.util.Log.d("MyPageViewModel", "✅ API 성공: ${resource.data}")
                            val userStatus = resource.data!!
                            android.util.Log.d("MyPageViewModel", "📊 API profitRate: ${userStatus.profitRate}")
                            cachedUserStatus = userStatus
                            
                            // 보유 종목들을 WebSocket 구독 목록에 추가
                            val stockCodes = userStatus.holdings.map { it.stockCode }
                            smartWebSocketService.updatePortfolioStocks(stockCodes)
                            
                            // 하이브리드 가격 계산으로 초기 포트폴리오 계산
                            initializeHybridPrices(stockCodes, userStatus)
                        }
                        is Resource.Error -> {
                            android.util.Log.e("MyPageViewModel", "❌ API 에러: ${resource.message}")
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
                android.util.Log.e("MyPageViewModel", "💥 예외 발생: ${e.localizedMessage}", e)
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
     * 실시간 데이터 업데이트 감시 (성능 최적화)
     */
    private fun observeRealTimeUpdates() {
        viewModelScope.launch {
            // 1초마다 한 번만 업데이트 (쓰로틀링)
            realTimeStockCache.quotes
                .sample(1000.milliseconds) // 1초 쓰로틀링
                .collect { quotesMap ->
                    cachedUserStatus?.let { userStatus ->
                        if (hybridPrices.isNotEmpty()) {
                            // 하이브리드 모드: 실시간 데이터로 업데이트
                            updateHybridPricesWithRealTime(quotesMap, userStatus)
                        } else {
                            // 폴백 모드: 기존 방식
                            updatePortfolioWithRealTimeData(userStatus)
                        }
                    }
                }
        }
    }

    /**
     * 실시간 데이터와 결합하여 포트폴리오 계산
     */
    private fun updatePortfolioWithRealTimeData(userStatus: UserCurrentStatusDto) {
        viewModelScope.launch {
            try {
                // 실시간 가격 데이터 가져오기
                val realTimePrices = realTimeStockCache.quotes.value
                
                // 포트폴리오 계산
                val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                    userStatus = userStatus,
                    realTimePrices = realTimePrices
                )
                
                // 비율 계산
                val holdingsWithWeights = PortfolioCalculator.calculateHoldingWeights(
                    portfolioSummary.holdings
                )
                
                // 파이차트 데이터 생성
                val pieChartData = PortfolioCalculator.createPieChartData(holdingsWithWeights)
                
                // UI 상태 업데이트
                _uiState.update { 
                    it.copy(
                        portfolioSummary = portfolioSummary.copy(holdings = holdingsWithWeights),
                        pieChartData = pieChartData,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                
            } catch (e: Exception) {
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
     * 새로고침
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            loadUserPortfolio()
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    /**
     * 로그인 후 호출
     */
    fun onLoginSuccess() {
        checkLoginStatus()
    }

    /**
     * 로그아웃
     */
    fun logout() {
        viewModelScope.launch {
            try {
                // UserRepository를 통한 완전한 로그아웃 처리
                userRepository.logout().fold(
                    onSuccess = {
                        android.util.Log.d("MyPageViewModel", "로그아웃 성공")
                        // 상태 초기화
                        cachedUserStatus = null
                        _uiState.update { 
                            MyPageUiState(isLoggedIn = false)
                        }
                    },
                    onFailure = { exception ->
                        android.util.Log.e("MyPageViewModel", "로그아웃 실패", exception)
                        // 실패해도 로컬 데이터는 지우고 로그아웃 처리
                        userPreferences.clearAllData()
                        cachedUserStatus = null
                        _uiState.update { 
                            MyPageUiState(isLoggedIn = false)
                        }
                    }
                )
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "로그아웃 중 오류가 발생했습니다: ${e.localizedMessage}")
                }
            }
        }
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
        return when {
            amount >= 100_000_000 -> "${amount / 100_000_000}억${if (amount % 100_000_000 != 0L) " ${(amount % 100_000_000) / 10_000}만" else ""}원"
            amount >= 10_000 -> "${amount / 10_000}만${if (amount % 10_000 != 0L) " ${amount % 10_000}" else ""}원"
            else -> "${amount}원"
        }
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
    
    /**
     * 하이브리드 가격 초기화
     */
    private fun initializeHybridPrices(stockCodes: List<String>, userStatus: UserCurrentStatusDto) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MyPageViewModel", "🔄 하이브리드 가격 초기화 시작: ${stockCodes.size}개 종목")
                
                val initialResult = hybridPriceCalculator.calculateInitialPrices(stockCodes)
                hybridPrices = initialResult.prices
                
                android.util.Log.d("MyPageViewModel", "✅ 초기 가격 계산 완료: ${initialResult.successCount}/${stockCodes.size}개 성공")
                
                // 초기 포트폴리오 계산
                updatePortfolioWithHybridPrices(userStatus)
                
            } catch (e: Exception) {
                android.util.Log.e("MyPageViewModel", "❌ 하이브리드 가격 초기화 실패", e)
                // 실패 시 기존 방식으로 폴백
                updatePortfolioWithRealTimeData(userStatus)
            }
        }
    }
    
    /**
     * 하이브리드 가격으로 포트폴리오 계산
     */
    private fun updatePortfolioWithHybridPrices(userStatus: UserCurrentStatusDto) {
        try {
            // 하이브리드 가격을 StockRealTimeData 형태로 변환
            val hybridRealTimePrices = hybridPriceCalculator.toStockRealTimeDataMap(hybridPrices)
            
            // 포트폴리오 계산
            val portfolioSummary = PortfolioCalculator.calculateMyPagePortfolio(
                userStatus = userStatus,
                realTimePrices = hybridRealTimePrices
            )
            
            // 비율 계산
            val holdingsWithWeights = PortfolioCalculator.calculateHoldingWeights(
                portfolioSummary.holdings
            )
            
            // 파이차트 데이터 생성
            val pieChartData = PortfolioCalculator.createPieChartData(holdingsWithWeights)
            
            // UI 상태 업데이트
            _uiState.update { 
                it.copy(
                    portfolioSummary = portfolioSummary.copy(holdings = holdingsWithWeights),
                    pieChartData = pieChartData,
                    isLoading = false,
                    errorMessage = null
                )
            }
            
            android.util.Log.d("MyPageViewModel", "📊 하이브리드 포트폴리오 계산 완료")
            
        } catch (e: Exception) {
            android.util.Log.e("MyPageViewModel", "💥 하이브리드 포트폴리오 계산 오류: ${e.localizedMessage}", e)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    errorMessage = "포트폴리오 계산 중 오류가 발생했습니다: ${e.localizedMessage}"
                )
            }
        }
    }
    
    /**
     * 실시간 데이터로 하이브리드 가격 업데이트
     */
    private fun updateHybridPricesWithRealTime(realTimeQuotes: Map<String, com.lago.app.domain.entity.StockRealTimeData>, userStatus: UserCurrentStatusDto) {
        try {
            // 하이브리드 가격을 실시간 데이터로 업데이트
            hybridPrices = hybridPriceCalculator.updateWithRealTimeData(hybridPrices, realTimeQuotes)
            
            // 업데이트된 하이브리드 가격으로 포트폴리오 재계산
            updatePortfolioWithHybridPrices(userStatus)
            
        } catch (e: Exception) {
            android.util.Log.e("MyPageViewModel", "💥 하이브리드 실시간 업데이트 오류: ${e.localizedMessage}", e)
        }
    }
    
}