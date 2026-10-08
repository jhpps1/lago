package com.lago.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.domain.entity.News
import com.lago.app.domain.usecase.GetNewsDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsDetailViewModel @Inject constructor(
    private val getNewsDetailUseCase: GetNewsDetailUseCase
) : ViewModel() {

    private val _newsDetailState = MutableStateFlow<NewsDetailUiState>(NewsDetailUiState.Loading)
    val newsDetailState: StateFlow<NewsDetailUiState> = _newsDetailState.asStateFlow()

    fun loadNewsDetail(newsId: Int) {
        viewModelScope.launch {
            _newsDetailState.value = NewsDetailUiState.Loading
            getNewsDetailUseCase(newsId).fold(
                onSuccess = { news ->
                    android.util.Log.d("NewsDetailViewModel", "📰 뉴스 상세 로드 성공: ${news.title}")
                    _newsDetailState.value = NewsDetailUiState.Success(news)
                },
                onFailure = { exception ->
                    android.util.Log.e("NewsDetailViewModel", "📰 뉴스 상세 로드 실패: ${exception.message}")
                    _newsDetailState.value = NewsDetailUiState.Error(exception.message ?: "Unknown error")
                }
            )
        }
    }
}

sealed class NewsDetailUiState {
    object Loading : NewsDetailUiState()
    data class Success(val news: News) : NewsDetailUiState()
    data class Error(val message: String) : NewsDetailUiState()
}