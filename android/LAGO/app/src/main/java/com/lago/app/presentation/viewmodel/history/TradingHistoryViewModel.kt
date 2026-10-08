package com.lago.app.presentation.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.domain.entity.TradingHistory
import com.lago.app.domain.entity.PagedResult
import com.lago.app.domain.entity.OrderType
import com.lago.app.domain.repository.MockTradeRepository
import com.lago.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 거래 내역 필터 정보
 */
data class TradingFilter(
    val stockCode: String? = null,
    val orderType: OrderType? = null, // BUY, SELL 또는 전체
    val startDate: String? = null,
    val endDate: String? = null
)

/**
 * 거래 내역 UI 상태
 */
data class TradingHistoryUiState(
    val tradingHistory: List<TradingHistory> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val hasMoreData: Boolean = true,
    val filter: TradingFilter = TradingFilter(),
    val refreshing: Boolean = false
)

@HiltViewModel
class TradingHistoryViewModel @Inject constructor(
    private val mockTradeRepository: MockTradeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradingHistoryUiState())
    val uiState: StateFlow<TradingHistoryUiState> = _uiState

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        loadTradingHistory()
    }

    /**
     * 거래 내역 로드 (첫 페이지)
     */
    fun loadTradingHistory(newFilter: TradingFilter? = null) {
        viewModelScope.launch {
            val filter = newFilter ?: _uiState.value.filter
            
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null,
                    filter = filter,
                    currentPage = 0,
                    tradingHistory = emptyList() // 새로운 필터 적용시 기존 데이터 클리어
                )
            }
            
            loadTradingHistoryPage(0, filter, isInitialLoad = true)
        }
    }

    /**
     * 다음 페이지 로드 (무한 스크롤)
     */
    fun loadMoreHistory() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMoreData) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            loadTradingHistoryPage(state.currentPage + 1, state.filter, isInitialLoad = false)
        }
    }

    /**
     * 특정 페이지의 거래 내역 로드
     */
    private suspend fun loadTradingHistoryPage(
        page: Int, 
        filter: TradingFilter,
        isInitialLoad: Boolean
    ) {
        try {
            mockTradeRepository.getTradingHistory(
                stockCode = filter.stockCode,
                buySell = filter.orderType?.value,
                page = page,
                size = PAGE_SIZE
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        if (isInitialLoad) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                    is Resource.Success -> {
                        val pagedResult = resource.data!!
                        val currentHistory = if (isInitialLoad) {
                            emptyList()
                        } else {
                            _uiState.value.tradingHistory
                        }
                        
                        val newHistory = currentHistory + pagedResult.content
                        val hasMore = page < pagedResult.totalPages - 1
                        
                        _uiState.update { state ->
                            state.copy(
                                tradingHistory = newHistory,
                                isLoading = false,
                                isLoadingMore = false,
                                currentPage = page,
                                totalPages = pagedResult.totalPages,
                                totalElements = pagedResult.totalElements,
                                hasMoreData = hasMore,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                isLoadingMore = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = "거래 내역 로드 중 오류가 발생했습니다: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * 새로고침
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true) }
            loadTradingHistory(_uiState.value.filter)
            _uiState.update { it.copy(refreshing = false) }
        }
    }

    /**
     * 필터 적용
     */
    fun applyFilter(filter: TradingFilter) {
        loadTradingHistory(filter)
    }

    /**
     * 필터 초기화
     */
    fun clearFilter() {
        loadTradingHistory(TradingFilter())
    }

    /**
     * 종목별 필터링
     */
    fun filterByStock(stockCode: String) {
        val newFilter = _uiState.value.filter.copy(stockCode = stockCode)
        loadTradingHistory(newFilter)
    }

    /**
     * 매수/매도 타입별 필터링
     */
    fun filterByOrderType(orderType: OrderType?) {
        val newFilter = _uiState.value.filter.copy(orderType = orderType)
        loadTradingHistory(newFilter)
    }

    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 거래 내역 통계 계산
     */
    fun getTradingStats(): TradingStats {
        val history = _uiState.value.tradingHistory
        
        val totalBuyTrades = history.count { it.buySell == "BUY" }
        val totalSellTrades = history.count { it.buySell == "SELL" }
        val totalBuyAmount = history.filter { it.buySell == "BUY" }.sumOf { it.totalAmount }
        val totalSellAmount = history.filter { it.buySell == "SELL" }.sumOf { it.totalAmount }
        val totalCommission = history.sumOf { it.commission.toLong() }
        
        return TradingStats(
            totalTrades = history.size,
            totalBuyTrades = totalBuyTrades,
            totalSellTrades = totalSellTrades,
            totalBuyAmount = totalBuyAmount,
            totalSellAmount = totalSellAmount,
            totalCommission = totalCommission,
            netAmount = totalSellAmount - totalBuyAmount - totalCommission
        )
    }

    /**
     * 월별 거래 통계
     */
    fun getMonthlyStats(): Map<String, TradingStats> {
        val history = _uiState.value.tradingHistory
        
        return history.groupBy { 
            // "YYYY-MM-DD HH:mm:ss" 형태에서 "YYYY-MM" 추출
            it.tradeAt.substring(0, 7) 
        }.mapValues { (_, trades) ->
            val buyTrades = trades.filter { it.buySell == "BUY" }
            val sellTrades = trades.filter { it.buySell == "SELL" }
            
            TradingStats(
                totalTrades = trades.size,
                totalBuyTrades = buyTrades.size,
                totalSellTrades = sellTrades.size,
                totalBuyAmount = buyTrades.sumOf { it.totalAmount },
                totalSellAmount = sellTrades.sumOf { it.totalAmount },
                totalCommission = trades.sumOf { it.commission.toLong() },
                netAmount = sellTrades.sumOf { it.totalAmount } - buyTrades.sumOf { it.totalAmount } - trades.sumOf { it.commission.toLong() }
            )
        }
    }

    /**
     * 종목별 거래 통계
     */
    fun getStockStats(): Map<String, TradingStats> {
        val history = _uiState.value.tradingHistory
        
        return history.groupBy { it.stockCode }.mapValues { (_, trades) ->
            val buyTrades = trades.filter { it.buySell == "BUY" }
            val sellTrades = trades.filter { it.buySell == "SELL" }
            
            TradingStats(
                totalTrades = trades.size,
                totalBuyTrades = buyTrades.size,
                totalSellTrades = sellTrades.size,
                totalBuyAmount = buyTrades.sumOf { it.totalAmount },
                totalSellAmount = sellTrades.sumOf { it.totalAmount },
                totalCommission = trades.sumOf { it.commission.toLong() },
                netAmount = sellTrades.sumOf { it.totalAmount } - buyTrades.sumOf { it.totalAmount } - trades.sumOf { it.commission.toLong() }
            )
        }
    }
}

/**
 * 거래 통계 데이터 클래스
 */
data class TradingStats(
    val totalTrades: Int,
    val totalBuyTrades: Int,
    val totalSellTrades: Int,
    val totalBuyAmount: Long,
    val totalSellAmount: Long,
    val totalCommission: Long,
    val netAmount: Long // 순 손익
)