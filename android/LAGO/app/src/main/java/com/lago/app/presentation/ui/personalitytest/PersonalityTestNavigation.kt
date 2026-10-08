package com.lago.app.presentation.ui.personalitytest

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.presentation.viewmodel.LoginViewModel

sealed class PersonalityTestStep {
    object TermsAgreement : PersonalityTestStep()
    object NicknameSetting : PersonalityTestStep()
    data class TestIntro(val nickname: String) : PersonalityTestStep()
    data class PersonalityTest(val nickname: String) : PersonalityTestStep()
    data class TestResult(val nickname: String, val score: Int) : PersonalityTestStep()
}

@Composable
fun PersonalityTestNavigation(
    userPreferences: UserPreferences,
    onBackToHome: () -> Unit = {},
    onTestComplete: (PersonalityResult) -> Unit = {},
    loginViewModel: LoginViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableStateOf<PersonalityTestStep>(PersonalityTestStep.TermsAgreement) }
    var nickname by remember { mutableStateOf("") }
    var testScore by remember { mutableStateOf(0) }
    
    // LoginViewModel 상태 관찰
    val uiState by loginViewModel.uiState.collectAsState()
    
    // 회원가입 완료 시 네비게이션 처리
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            android.util.Log.d("PersonalityTestNavigation", "회원가입 성공! 홈으로 네비게이션 시작")
            onTestComplete(PersonalityResult(
                type = PersonalityTestData.calculatePersonality(testScore),
                score = testScore,
                characterRes = when (PersonalityTestData.calculatePersonality(testScore)) {
                    PersonalityType.CAUTIOUS -> com.lago.app.R.drawable.character_green
                    PersonalityType.BALANCED -> com.lago.app.R.drawable.character_blue
                    PersonalityType.ACTIVE -> com.lago.app.R.drawable.character_yellow
                    PersonalityType.AGGRESSIVE -> com.lago.app.R.drawable.character_red
                },
                description = PersonalityTestData.getPersonalityDescription(PersonalityTestData.calculatePersonality(testScore)),
                nickname = nickname
            ))
            loginViewModel.resetLoginState()
        }
    }
    
    // 에러 발생 시 로그 출력
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.util.Log.e("PersonalityTestNavigation", "회원가입 중 에러 발생: $error")
        }
    }

    when (currentStep) {
        is PersonalityTestStep.TermsAgreement -> {
            TermsAgreementScreen(
                onBackClick = onBackToHome,
                onNextClick = {
                    currentStep = PersonalityTestStep.NicknameSetting
                }
            )
        }
        
        is PersonalityTestStep.NicknameSetting -> {
            NicknameSettingScreen(
                onBackClick = {
                    currentStep = PersonalityTestStep.TermsAgreement
                },
                onNextClick = { inputNickname ->
                    nickname = inputNickname
                    currentStep = PersonalityTestStep.TestIntro(inputNickname)
                }
            )
        }
        
        is PersonalityTestStep.TestIntro -> {
            PersonalityTestIntroScreen(
                nickname = (currentStep as PersonalityTestStep.TestIntro).nickname,
                onNextClick = {
                    currentStep = PersonalityTestStep.PersonalityTest(nickname)
                }
            )
        }
        
        is PersonalityTestStep.PersonalityTest -> {
            PersonalityTestScreen(
                nickname = (currentStep as PersonalityTestStep.PersonalityTest).nickname,
                onBackClick = {
                    currentStep = PersonalityTestStep.TestIntro(nickname)
                },
                onPreviousClick = {
                    currentStep = PersonalityTestStep.TestIntro(nickname)
                },
                onTestComplete = { score ->
                    testScore = score
                    currentStep = PersonalityTestStep.TestResult(nickname, score)
                }
            )
        }
        
        is PersonalityTestStep.TestResult -> {
            PersonalityTestResultScreen(
                nickname = (currentStep as PersonalityTestStep.TestResult).nickname,
                totalScore = (currentStep as PersonalityTestStep.TestResult).score,
                isLoading = uiState.isLoading,
                error = uiState.error,
                onCompleteClick = {
                    android.util.Log.d("PersonalityTestNavigation", "시작하기 버튼 클릭됨")
                    val personalityType = PersonalityTestData.calculatePersonality((currentStep as PersonalityTestStep.TestResult).score)
                    val personalityString = personalityType.characterName
                    
                    android.util.Log.d("PersonalityTestNavigation", "회원가입 API 호출 - nickname: ${(currentStep as PersonalityTestStep.TestResult).nickname}, personality: $personalityString")
                    
                    // 실제 회원가입 API 호출
                    loginViewModel.completeSignup(
                        nickname = (currentStep as PersonalityTestStep.TestResult).nickname,
                        personality = personalityString
                    )
                }
            )
        }
    }
}