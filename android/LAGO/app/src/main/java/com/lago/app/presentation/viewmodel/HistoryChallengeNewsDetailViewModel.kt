package com.lago.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.domain.entity.HistoryChallengeNews
import com.lago.app.domain.usecase.GetHistoryChallengeNewsDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryChallengeNewsDetailUiState {
    object Loading : HistoryChallengeNewsDetailUiState()
    data class Success(val news: HistoryChallengeNews) : HistoryChallengeNewsDetailUiState()
    data class Error(val message: String) : HistoryChallengeNewsDetailUiState()
}

@HiltViewModel
class HistoryChallengeNewsDetailViewModel @Inject constructor(
    private val getHistoryChallengeNewsDetailUseCase: GetHistoryChallengeNewsDetailUseCase
) : ViewModel() {

    private val _newsDetailState = MutableStateFlow<HistoryChallengeNewsDetailUiState>(HistoryChallengeNewsDetailUiState.Loading)
    val newsDetailState: StateFlow<HistoryChallengeNewsDetailUiState> = _newsDetailState

    fun loadNewsDetail(challengeNewsId: Int) {
        viewModelScope.launch {
            _newsDetailState.value = HistoryChallengeNewsDetailUiState.Loading
            
            try {
                android.util.Log.d("HistoryChallengeNewsDetailViewModel", "📰 역사적 챌린지 뉴스 상세 로드 시작 - challengeNewsId: $challengeNewsId")
                
                val result = getHistoryChallengeNewsDetailUseCase(challengeId = 1, challengeNewsId = challengeNewsId)
                
                result.fold(
                    onSuccess = { news ->
                        android.util.Log.d("HistoryChallengeNewsDetailViewModel", "📰 역사적 챌린지 뉴스 상세 로드 성공: ${news.title}")
                        
                        // 눈속임용 이미지 URL 배정 (challengeNewsId를 인덱스로 사용)
                        val imageUrls = listOf(
                            "https://cdn.mkhealth.co.kr/news/photo/202507/74161_81722_246.jpg",
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR-bDIwX9WkNd6uaMY07SxejeVc7BRmYpzw5g&s",
                            "https://img.biz.sbs.co.kr/upload/2023/08/18/0xd1692311387997.jpg",
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT_bFJT00njJbmoVtvicgKeuCVsmMerJpXZWg&s",
                            "https://www.medipana.com/upload/editor/20230213234604_EC49B.jpg",
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTPMp1xR_Ig64wDIbPkbczsd2gJ4I16zgIlbw&s",
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTMRyNfinWnwr3_DaKvxYIWqZPwY8ZPBjlIvg&s",
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRe1bDhvqv4rCq8UQ2AcfZvPi44VNLyYVrT3A&s",
                            "https://img.investchosun.com/site/data/img_dir/2025/04/21/2025042180224_0.png",
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR3Ry23wZWaeJ_yly0mcqtc33-5IQtV57bJbg&s"
                        )
                        
                        val imageIndex = (challengeNewsId - 1) % imageUrls.size
                        val newsWithImage = news.copy(imageUrl = imageUrls[imageIndex])
                        
                        _newsDetailState.value = HistoryChallengeNewsDetailUiState.Success(newsWithImage)
                    },
                    onFailure = { error ->
                        android.util.Log.e("HistoryChallengeNewsDetailViewModel", "📰 역사적 챌린지 뉴스 상세 로드 실패: ${error.message}")
                        _newsDetailState.value = HistoryChallengeNewsDetailUiState.Error("뉴스를 불러오는데 실패했습니다")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("HistoryChallengeNewsDetailViewModel", "📰 역사적 챌린지 뉴스 상세 로드 예외", e)
                _newsDetailState.value = HistoryChallengeNewsDetailUiState.Error("뉴스를 불러오는데 실패했습니다: ${e.message}")
            }
        }
    }
}