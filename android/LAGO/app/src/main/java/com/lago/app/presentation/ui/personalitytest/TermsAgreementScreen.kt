package com.lago.app.presentation.ui.personalitytest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lago.app.presentation.theme.*
import com.lago.app.presentation.ui.components.CommonTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAgreementScreen(
    onBackClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    var allAgreed by remember { mutableStateOf(false) }
    var serviceTermsAgreed by remember { mutableStateOf(false) }
    var privacyPolicyAgreed by remember { mutableStateOf(false) }
    var marketingAgreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            Text(
                text = "서비스 이용을 위해\n약관에 동의해 주세요",
                style = HeadEb28,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
//            Text(
//                text = "LAGO 서비스 이용을 위해 필요한 약관들입니다",
//                style = BodyR16,
//                color = Gray600
//            )
            
            Spacer(modifier = Modifier.height(40.dp))

            // 전체 동의
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        allAgreed = !allAgreed
                        serviceTermsAgreed = allAgreed
                        privacyPolicyAgreed = allAgreed
                        marketingAgreed = allAgreed
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (allAgreed) MainBlue.copy(alpha = 0.1f) else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (allAgreed) MainBlue else Gray300
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allAgreed,
                        onCheckedChange = {
                            allAgreed = it
                            serviceTermsAgreed = it
                            privacyPolicyAgreed = it
                            marketingAgreed = it
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MainBlue,
                            uncheckedColor = Gray300
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "전체 동의",
                        style = TitleB18,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 개별 약관들
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TermsCheckItem(
                    title = "[필수] 서비스 이용약관",
                    isChecked = serviceTermsAgreed,
                    onCheckedChange = {
                        serviceTermsAgreed = it
                        allAgreed = serviceTermsAgreed && privacyPolicyAgreed && marketingAgreed
                    },
                    required = true
                )
                
                TermsCheckItem(
                    title = "[필수] 개인정보 처리방침",
                    isChecked = privacyPolicyAgreed,
                    onCheckedChange = {
                        privacyPolicyAgreed = it
                        allAgreed = serviceTermsAgreed && privacyPolicyAgreed && marketingAgreed
                    },
                    required = true
                )
                
                TermsCheckItem(
                    title = "[선택] 마케팅 정보 수신 동의",
                    isChecked = marketingAgreed,
                    onCheckedChange = {
                        marketingAgreed = it
                        allAgreed = serviceTermsAgreed && privacyPolicyAgreed && marketingAgreed
                    },
                    required = false
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNextClick,
                enabled = serviceTermsAgreed && privacyPolicyAgreed,
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

@Composable
private fun TermsCheckItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    required: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MainBlue,
                uncheckedColor = Gray300
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = SubtitleSb16,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "보기",
            style = BodyR14,
            color = MainBlue,
            modifier = Modifier.clickable { /* TODO: 약관 내용 보기 */ }
        )
    }
}