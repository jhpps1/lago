package com.lago.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.data.remote.dto.CalculatedRankingUser
import com.lago.app.domain.repository.RankingRepository
import com.lago.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 랭킹 화면 UI 상태
 */
data class RankingUiState(
    val rankings: List<CalculatedRankingUser> = emptyList(),
    val currentUser: CalculatedRankingUser? = null,
    val top3Users: List<CalculatedRankingUser> = emptyList(),
    val otherUsers: List<CalculatedRankingUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val rankingRepository: RankingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(RankingUiState())
    val uiState: StateFlow<RankingUiState> = _uiState

    init {
        checkLoginStatus()
        loadRanking()
    }

    /**
     * 로그인 상태 확인
     */
    private fun checkLoginStatus() {
        val isLoggedIn = userPreferences.getAccessToken() != null
        _uiState.update { it.copy(isLoggedIn = isLoggedIn) }
    }

    /**
     * 랭킹 데이터 로드
     */
    fun loadRanking() {
        viewModelScope.launch {
            rankingRepository.getRanking().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        android.util.Log.d("RankingViewModel", "⏳ 랭킹 로딩 중...")
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is Resource.Success -> {
                        android.util.Log.d("RankingViewModel", "✅ 랭킹 로드 성공: ${resource.data?.size}명")
                        val rankings = resource.data ?: emptyList()
                        
                        // 랭킹 데이터 분류
                        val currentUser = rankings.find { it.isCurrentUser }
                        val top3 = rankings.filter { it.rank <= 3 }
                        val others = rankings.filter { it.rank > 3 }
                        
                        _uiState.update { 
                            it.copy(
                                rankings = rankings,
                                currentUser = currentUser,
                                top3Users = top3,
                                otherUsers = others,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        android.util.Log.e("RankingViewModel", "❌ 랭킹 로드 실패: ${resource.message}")
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 새로고침
     */
    fun refresh() {
        loadRanking()
    }

    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * 금액 포맷팅 (콤마 구분)
     */
    fun formatAmount(amount: Long): String {
        return String.format("%,d원", amount)
    }

    /**
     * 수익률 포맷팅
     */
    fun formatProfitRate(profitRate: Double): String {
        val sign = if (profitRate > 0) "+" else ""
        return "${sign}${String.format("%.2f", profitRate)}%"
    }

    /**
     * 수익 금액 + 비율 포맷팅
     */
    fun formatProfitWithRate(profit: Long, profitRate: Double): String {
        val sign = if (profit > 0) "+" else ""
        return "${sign}${formatAmount(profit)}(${formatProfitRate(profitRate)})"
    }

    /**
     * 수익률 색상 반환
     */
    fun getProfitColor(profitRate: Double): androidx.compose.ui.graphics.Color {
        return when {
            profitRate > 0 -> androidx.compose.ui.graphics.Color(0xFFFF99C5) // MainPink
            profitRate < 0 -> androidx.compose.ui.graphics.Color.Blue
            else -> androidx.compose.ui.graphics.Color.Gray
        }
    }
}