package com.lago.app.presentation.ui.personalitytest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lago.app.presentation.theme.*
import com.lago.app.presentation.ui.components.CommonTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NicknameSettingScreen(
    onBackClick: () -> Unit = {},
    onNextClick: (String) -> Unit = {}
) {
    var nickname by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val isValidNickname = nickname.isNotBlank() && 
                         nickname.length >= 2 && 
                         nickname.length <= 10 &&
                         nickname.all { it.isLetterOrDigit() || it.isWhitespace() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(nickname) {
        isError = false
        errorMessage = ""
        
        when {
            nickname.isEmpty() -> {
                // 아무 에러 메시지 표시 안함
            }
            nickname.length < 2 -> {
                isError = true
                errorMessage = "닉네임은 2자 이상이어야 해요"
            }
            nickname.length > 10 -> {
                isError = true
                errorMessage = "닉네임은 10자 이하여야 해요"
            }
            !nickname.all { it.isLetterOrDigit() || it.isWhitespace() } -> {
                isError = true
                errorMessage = "한글, 영문, 숫자만 사용할 수 있어요"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "닉네임을 설정해 주세요",
                style = HeadEb28,
                color = Color.Black,
                textAlign = TextAlign.Center
            )

//            Text(
//                text = "언제든 마이페이지에서 변경할 수 있어요",
//                style = BodyR16,
//                color = Gray600,
//                textAlign = TextAlign.Center
//            )
            
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = { 
                    if (it.length <= 10) {
                        nickname = it
                    }
                },
                label = { 
                    Text(
                        text = "닉네임",
                        style = SubtitleSb16,
                        color = if (isError) Color(0xFFFF6B6B) else Gray600
                    )
                },
                placeholder = {
                    Text(
                        text = "닉네임을 입력해 주세요",
                        style = BodyR16,
                        color = Gray400
                    )
                },
                isError = isError,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isError) errorMessage else "",
                            style = BodyR14,
                            color = if (isError) Color(0xFFFF6B6B) else Color.Transparent
                        )
                        Text(
                            text = "${nickname.length}/10",
                            style = BodyR14,
                            color = Gray500
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (isValidNickname) {
                            onNextClick(nickname)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isError) Color(0xFFFF6B6B) else MainBlue,
                    unfocusedBorderColor = if (isError) Color(0xFFFF6B6B) else Gray300,
                    cursorColor = MainBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "닉네임 규칙",
                        style = SubtitleSb14,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 2~10자 이내\n• 한글, 영문, 숫자 사용 가능\n• 특수문자 사용 불가",
                        style = BodyR14,
                        color = Gray600
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onNextClick(nickname) },
                enabled = isValidNickname,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainBlue,
                    disabledContainerColor = Gray300
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "다음",
                    style = TitleB16,
                    color = Color.White
                )
            }
        }
    }
}