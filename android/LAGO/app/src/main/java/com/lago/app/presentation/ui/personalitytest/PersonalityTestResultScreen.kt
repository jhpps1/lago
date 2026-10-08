package com.lago.app.presentation.ui.personalitytest

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lago.app.R
import com.lago.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityTestResultScreen(
    nickname: String = "",
    totalScore: Int = 0,
    isLoading: Boolean = false,
    error: String? = null,
    onCompleteClick: () -> Unit = {}
) {
    val personalityType = PersonalityTestData.calculatePersonality(totalScore)
    val description = PersonalityTestData.getPersonalityDescription(personalityType)
    
    val characterRes = when (personalityType) {
        PersonalityType.CAUTIOUS -> R.drawable.character_green
        PersonalityType.BALANCED -> R.drawable.character_blue
        PersonalityType.ACTIVE -> R.drawable.character_yellow
        PersonalityType.AGGRESSIVE -> R.drawable.character_red
    }
    
    val themeColor = when (personalityType) {
        PersonalityType.CAUTIOUS -> Color(0xFF4CAF50)
        PersonalityType.BALANCED -> MainBlue
        PersonalityType.ACTIVE -> Color(0xFFFFB74D)
        PersonalityType.AGGRESSIVE -> Color(0xFFFF6B6B)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "${nickname}님의 결과",
                style = HeadEb28,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 캐릭터와 결과 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = themeColor.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 캐릭터 이미지
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = characterRes),
                            contentDescription = "${personalityType.characterName} 캐릭터",
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = personalityType.characterName,
                        style = HeadEb32,
                        color = themeColor
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = personalityType.displayName,
                        style = TitleB18,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 성향 설명 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF8F9FA)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "${personalityType.characterName}의 특징",
                        style = TitleB18,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = description,
                        style = BodyR16,
                        color = Gray600
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 투자 스타일 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    themeColor.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "추천 투자 스타일",
                        style = TitleB18,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val investmentStyles = when (personalityType) {
                        PersonalityType.CAUTIOUS -> listOf("예·적금", "원금보장형 상품", "안전자산 위주")
                        PersonalityType.BALANCED -> listOf("ETF", "우량주", "중위험 중수익")
                        PersonalityType.ACTIVE -> listOf("테마주", "펀드", "분산투자")
                        PersonalityType.AGGRESSIVE -> listOf("고위험 고수익", "단기 트레이딩", "성장주")
                    }
                    
                    investmentStyles.forEach { style ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = themeColor,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = style,
                                style = BodyR16,
                                color = Gray600
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Button(
                onClick = onCompleteClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "시작하기",
                        style = TitleB16,
                        color = Color.White
                    )
                }
            }
            
            // 에러 메시지 표시
            error?.let { errorMessage ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = BodyR14,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}