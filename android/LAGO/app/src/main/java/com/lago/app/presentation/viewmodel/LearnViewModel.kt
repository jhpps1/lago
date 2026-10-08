package com.lago.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.domain.entity.DailyQuizStreak
import com.lago.app.domain.usecase.GetDailyQuizStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LearnUiState {
    object Loading : LearnUiState()
    data class Success(val streak: DailyQuizStreak) : LearnUiState()
    data class Error(val message: String) : LearnUiState()
}

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val getDailyQuizStreakUseCase: GetDailyQuizStreakUseCase,
    private val userPreferences: com.lago.app.data.local.prefs.UserPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<LearnUiState>(LearnUiState.Loading)
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()
    
    fun loadStreak(userId: Int = userPreferences.getUserIdLong().toInt()) {
        viewModelScope.launch {
            _uiState.value = LearnUiState.Loading
            
            getDailyQuizStreakUseCase(userId)
                .onSuccess { streak ->
                    _uiState.value = LearnUiState.Success(streak)
                }
                .onFailure { error ->
                    _uiState.value = LearnUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}