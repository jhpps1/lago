package com.lago.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.domain.repository.AuthRepository
import com.lago.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private lateinit var googleSignInClient: GoogleSignInClient

    fun initializeGoogleSignIn(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken("YOUR_WEB_CLIENT_ID") // google-services.json에서 확인
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getGoogleSignInClient(): GoogleSignInClient = googleSignInClient

    fun handleGoogleSignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                
                account.idToken?.let { idToken ->
                    // Google ID Token을 백엔드로 전송
                    when (val result = authRepository.googleLogin(idToken)) {
                        is Resource.Success -> {
                            val loginData = result.data?.data
                            if (result.data?.success == true && loginData != null) {
                                when (loginData.status) {
                                    "EXISTING_USER" -> {
                                        // 기존 사용자 - 토큰 저장하고 메인으로
                                        loginData.accessToken?.let { 
                                            userPreferences.saveAccessToken(it) 
                                        }
                                        loginData.refreshToken?.let { 
                                            userPreferences.saveRefreshToken(it) 
                                        }
                                        loginData.user?.let { user ->
                                            userPreferences.saveUserId(user.id)
                                            userPreferences.saveUserEmail(user.email)
                                            user.nickname?.let { userPreferences.saveUserNickname(it) }
                                        }
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            loginSuccess = true,
                                            needsSignup = false
                                        )
                                    }
                                    "NEW_USER" -> {
                                        // 신규 사용자 - 회원가입 필요
                                        loginData.tempToken?.let { 
                                            userPreferences.saveTempToken(it) 
                                        }
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            loginSuccess = false,
                                            needsSignup = true
                                        )
                                    }
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = result.data?.message ?: "로그인 실패"
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message ?: "네트워크 오류"
                            )
                        }
                        is Resource.Loading -> {
                            // 이미 로딩 상태
                        }
                    }
                }
            } catch (e: ApiException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google 로그인 실패: ${e.message}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "로그인 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetLoginState() {
        _uiState.value = LoginUiState()
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val needsSignup: Boolean = false,
    val error: String? = null
)