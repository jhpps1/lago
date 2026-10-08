package com.lago.app.presentation.ui.login

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lago.app.R
import com.lago.app.presentation.theme.LagoTheme
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.presentation.viewmodel.LoginViewModel
import android.content.SharedPreferences

@Composable
fun LoginScreen(
    userPreferences: UserPreferences,
    onLoginSuccess: () -> Unit = {},
    onSignupNeeded: () -> Unit = {},
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by loginViewModel.uiState.collectAsState()
    
    // Google 로그인 초기화
    LaunchedEffect(Unit) {
        Log.d("LoginScreen", "Google 로그인 초기화 시작")
        loginViewModel.initializeGoogleSignIn(context)
    }
    
    // Google 로그인 결과 처리
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("LoginScreen", "Google 로그인 결과 수신: resultCode=${result.resultCode}")
        Log.d("LoginScreen", "RESULT_OK=${Activity.RESULT_OK}, RESULT_CANCELED=${Activity.RESULT_CANCELED}")
        Log.d("LoginScreen", "Intent data: ${result.data}")
        Log.d("LoginScreen", "Intent extras: ${result.data?.extras}")
        
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LoginScreen", "로그인 성공, 결과 처리 시작")
            loginViewModel.handleGoogleSignInResult(result.data, context)
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.d("LoginScreen", "사용자가 로그인을 취소했습니다")
            // 취소된 경우에도 Intent 데이터가 있는지 확인
            if (result.data != null) {
                Log.d("LoginScreen", "취소되었지만 Intent 데이터가 존재함, 처리 시도")
                loginViewModel.handleGoogleSignInResult(result.data, context)
            }
        } else {
            Log.d("LoginScreen", "알 수 없는 결과코드: ${result.resultCode}")
        }
    }
    
    // UI 상태 처리
    LaunchedEffect(uiState) {
        when {
            uiState.loginSuccess -> {
                onLoginSuccess()
                loginViewModel.resetLoginState()
            }
            uiState.needsSignup -> {
                onSignupNeeded()
                loginViewModel.resetLoginState()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // 로고 이미지
        Image(
            painter = painterResource(id = R.drawable.login_text_image),
            contentDescription = "Login text",
            modifier = Modifier
                .width(230.dp)
                .padding(bottom = 30.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.login_screen),
            contentDescription = "Login Image",
            modifier = Modifier
                .size(270.dp)
                .padding(bottom = 60.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 구글 로그인 버튼
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { 
                    Log.d("LoginScreen", "Google 로그인 버튼 클릭됨")
                    if (!uiState.isLoading) {
                        try {
                            Log.d("LoginScreen", "Google Sign-In 클라이언트 가져오는 중...")
                            val signInIntent = loginViewModel.getGoogleSignInClient().signInIntent
                            Log.d("LoginScreen", "Sign-In Intent 생성 완료, 런처 실행")
                            googleSignInLauncher.launch(signInIntent)
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "Google 로그인 시작 실패: ${e.message}", e)
                        }
                    } else {
                        Log.d("LoginScreen", "이미 로딩 중이므로 클릭 무시")
                    }
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.google_login),
                        contentDescription = "Google Login",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    )
                }
            }
        }
        
        // 에러 메시지 표시
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 에러 메시지 자동 삭제
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                loginViewModel.clearError()
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LagoTheme {
        val mockSharedPrefs = object : SharedPreferences {
            override fun getAll(): MutableMap<String, *> = mutableMapOf<String, Any>()
            override fun getString(key: String?, defValue: String?): String? = defValue
            override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
            override fun getInt(key: String?, defValue: Int): Int = defValue
            override fun getLong(key: String?, defValue: Long): Long = defValue
            override fun getFloat(key: String?, defValue: Float): Float = defValue
            override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
            override fun contains(key: String?): Boolean = false
            override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                override fun putString(key: String?, value: String?) = this
                override fun putStringSet(key: String?, values: MutableSet<String>?) = this
                override fun putInt(key: String?, value: Int) = this
                override fun putLong(key: String?, value: Long) = this
                override fun putFloat(key: String?, value: Float) = this
                override fun putBoolean(key: String?, value: Boolean) = this
                override fun remove(key: String?) = this
                override fun clear() = this
                override fun commit() = true
                override fun apply() {}
            }
            override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
            override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        }
        LoginScreen(userPreferences = UserPreferences(mockSharedPrefs))
    }
}