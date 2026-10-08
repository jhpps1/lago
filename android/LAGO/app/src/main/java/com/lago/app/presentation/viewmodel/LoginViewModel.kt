package com.lago.app.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.tasks.Tasks
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        Log.d("LoginViewModel", "initializeGoogleSignIn 시작")
        try {
            // 기존 계정 로그아웃 (캐시 초기화)
            val tempGso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val tempClient = GoogleSignIn.getClient(context, tempGso)
            tempClient.signOut()
            
            // OAuth 동의 화면 설정 후 토큰 요청
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("641435048942-va9dhns7ghn2404k4gmsar6h02batoeh.apps.googleusercontent.com")
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
            Log.d("LoginViewModel", "Google Sign-In 클라이언트 초기화 완료")
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Google Sign-In 초기화 실패: ${e.message}", e)
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        Log.d("LoginViewModel", "getGoogleSignInClient 호출됨")
        return googleSignInClient
    }

    fun handleGoogleSignInResult(data: android.content.Intent?, context: Context) {
        viewModelScope.launch {
            Log.d("LoginViewModel", "handleGoogleSignInResult 시작")
            Log.d("LoginViewModel", "Intent 데이터: $data")
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                Log.d("LoginViewModel", "Task 생성 완료: $task")
                
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginViewModel", "계정 정보 추출 완료")
                
                Log.d("LoginViewModel", "Google 계정 정보:")
                Log.d("LoginViewModel", "  - Email: ${account.email}")
                Log.d("LoginViewModel", "  - Display Name: ${account.displayName}")
                Log.d("LoginViewModel", "  - ID: ${account.id}")
                Log.d("LoginViewModel", "  - Server Auth Code: ${account.serverAuthCode}")
                Log.d("LoginViewModel", "  - ID Token: ${account.idToken}")
                
                // Google AccessToken 가져오기 (IO 스레드에서 실행)
                val accessToken = try {
                    withContext(Dispatchers.IO) {
                        // 기존 토큰들 모두 클리어
                        val scopes = "oauth2:https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile"
                        
                        // 기존 캐시된 토큰들 삭제
                        try {
                            val oldToken = GoogleAuthUtil.getToken(context, account.account!!, scopes)
                            GoogleAuthUtil.clearToken(context, oldToken)
                        } catch (ignored: Exception) {
                            Log.d("LoginViewModel", "기존 토큰 클리어 시도: ${ignored.message}")
                        }
                        
                        // 새로운 토큰 요청
                        GoogleAuthUtil.getToken(context, account.account!!, scopes)
                    }
                } catch (e: Exception) {
                    Log.e("LoginViewModel", "AccessToken 가져오기 실패: ${e.message}")
                    ""
                }
                
                Log.d("LoginViewModel", "사용할 AccessToken 앞 50자: ${accessToken.take(50)}")
                Log.d("LoginViewModel", "AccessToken 길이: ${accessToken.length}")
                Log.d("LoginViewModel", "AccessToken이 ya29로 시작하는지: ${accessToken.startsWith("ya29")}")
                
                // 토큰 직접 테스트
                Log.d("LoginViewModel", "Google UserInfo API 직접 테스트 시작")
                try {
                    val testUrl = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=$accessToken"
                    Log.d("LoginViewModel", "테스트 URL: $testUrl")
                } catch (e: Exception) {
                    Log.e("LoginViewModel", "직접 테스트 중 오류: ${e.message}")
                }
                
                if (accessToken.isNotEmpty()) {
                    Log.d("LoginViewModel", "백엔드로 로그인 요청 전송 (AccessToken 길이: ${accessToken.length})")
                    when (val result = authRepository.googleLogin(accessToken)) {
                        is Resource.Success -> {
                            val loginData = result.data?.data
                            if (result.data?.success == true && loginData != null) {
                                // needsSignup 필드로 판단
                                if (loginData.needsSignup == true) {
                                    // 신규 사용자 - 회원가입 필요
                                    loginData.tempToken?.let { 
                                        userPreferences.saveTempToken(it) 
                                    }
                                    loginData.email?.let {
                                        userPreferences.saveUserEmail(it)
                                    }
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        loginSuccess = false,
                                        needsSignup = true,
                                        suggestedNickname = loginData.suggestedNickname
                                    )
                                } else {
                                    // 기존 사용자 - 토큰 저장하고 메인으로
                                    loginData.accessToken?.let { 
                                        userPreferences.saveAccessToken(it) 
                                    }
                                    loginData.refreshToken?.let { 
                                        userPreferences.saveRefreshToken(it) 
                                    }
                                    loginData.user?.let { user ->
                                        Log.d("LoginViewModel", "기존 로그인 - User 객체: ${user}")
                                        Log.d("LoginViewModel", "기존 로그인 - User.id 값: ${user.id}")
                                        userPreferences.saveUserId(user.id)
                                        userPreferences.saveUserEmail(user.email)
                                        user.nickname?.let { userPreferences.saveUserNickname(it) }
                                        Log.d("LoginViewModel", "기존 로그인 - 저장 후 확인 userId: ${userPreferences.getUserIdLong()}")
                                    }
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        loginSuccess = true,
                                        needsSignup = false
                                    )
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = result.data?.message ?: "로그인 실패"
                                )
                            }
                        }
                        is Resource.Error -> {
                            Log.e("LoginViewModel", "백엔드 로그인 실패: ${result.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message ?: "네트워크 오류"
                            )
                        }
                        is Resource.Loading -> {
                            Log.d("LoginViewModel", "백엔드 로그인 요청 처리 중...")
                        }
                    }
                } else {
                    Log.e("LoginViewModel", "AccessToken을 가져올 수 없습니다")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Google 인증 토큰을 가져올 수 없습니다"
                    )
                }
            } catch (e: ApiException) {
                Log.e("LoginViewModel", "ApiException 발생: ${e.message}, statusCode: ${e.statusCode}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Google 로그인 실패: ${e.message} (코드: ${e.statusCode})"
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception 발생: ${e.message}", e)
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

    fun completeSignup(nickname: String, personality: String) {
        viewModelScope.launch {
            Log.d("LoginViewModel", "===== 회원가입 완료 시작 =====")
            Log.d("LoginViewModel", "입력된 닉네임: $nickname")
            Log.d("LoginViewModel", "선택된 성격: $personality")
            
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val tempToken = userPreferences.getTempToken()
            Log.d("LoginViewModel", "저장된 임시 토큰: $tempToken")
            
            if (tempToken.isNullOrEmpty()) {
                Log.e("LoginViewModel", "임시 토큰이 없음!")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "임시 토큰이 없습니다. 다시 로그인해주세요."
                )
                return@launch
            }
            
            try {
                Log.d("LoginViewModel", "AuthRepository.completeSignup 호출 시작")
                when (val result = authRepository.completeSignup(tempToken, nickname, personality)) {
                    is Resource.Success -> {
                        Log.d("LoginViewModel", "회원가입 완료 성공 응답 수신")
                        Log.d("LoginViewModel", "응답 데이터: ${result.data}")
                        
                        if (result.data?.success == true) {
                            Log.d("LoginViewModel", "회원가입 성공 - 토큰 저장 시작")
                            
                            // 회원가입 완료 - 토큰 저장하고 메인으로
                            result.data.accessToken?.let { 
                                userPreferences.saveAccessToken(it)
                                Log.d("LoginViewModel", "AccessToken 저장 완료")
                            }
                            result.data.refreshToken?.let { 
                                userPreferences.saveRefreshToken(it)
                                Log.d("LoginViewModel", "RefreshToken 저장 완료")
                            }
                            result.data.user?.let { user ->
                                Log.d("LoginViewModel", "User 객체 파싱됨: ${user}")
                                Log.d("LoginViewModel", "User.id 값: ${user.id}")
                                userPreferences.saveUserId(user.id)
                                userPreferences.saveUserEmail(user.email)
                                userPreferences.saveUserNickname(user.nickname ?: nickname)
                                Log.d("LoginViewModel", "사용자 정보 저장 완료: ${user}")
                                Log.d("LoginViewModel", "저장 후 확인 - userId: ${userPreferences.getUserIdLong()}")
                            } ?: Log.e("LoginViewModel", "User 객체가 null입니다!")
                            
                            // 임시 토큰 삭제
                            userPreferences.clearTempToken()
                            Log.d("LoginViewModel", "임시 토큰 삭제 완료")
                            
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                loginSuccess = true,
                                needsSignup = false
                            )
                            Log.d("LoginViewModel", "회원가입 완료 - UI 상태 업데이트 완료")
                        } else {
                            Log.e("LoginViewModel", "회원가입 응답 실패")
                            Log.e("LoginViewModel", "success: ${result.data?.success}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.data?.message ?: "회원가입 완료 실패"
                            )
                        }
                    }
                    is Resource.Error -> {
                        Log.e("LoginViewModel", "회원가입 완료 실패: ${result.message}")
                        Log.e("LoginViewModel", "에러 메시지: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message ?: "회원가입 완료 중 오류가 발생했습니다."
                        )
                    }
                    is Resource.Loading -> {
                        Log.d("LoginViewModel", "회원가입 완료 요청 처리 중...")
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "회원가입 완료 중 예외 발생: ${e.message}", e)
                Log.e("LoginViewModel", "예외 스택 트레이스: ", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "회원가입 완료 중 오류가 발생했습니다: ${e.message}"
                )
            }
            Log.d("LoginViewModel", "===== 회원가입 완료 요청 종료 =====")
        }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val needsSignup: Boolean = false,
    val error: String? = null,
    val suggestedNickname: String? = null
)