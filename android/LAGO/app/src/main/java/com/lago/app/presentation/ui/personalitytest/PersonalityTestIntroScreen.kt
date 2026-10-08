package com.lago.app.presentation.ui.personalitytest

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lago.app.R
import com.lago.app.presentation.theme.*

@Composable
fun PersonalityTestIntroScreen(
    nickname: String = "",
    onNextClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // 캐릭터 이미지들
        Row(
            modifier = Modifier.padding(vertical = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.character_red),
                contentDescription = "빨간 캐릭터",
                modifier = Modifier.size(60.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.character_yellow),
                contentDescription = "노란 캐릭터",
                modifier = Modifier.size(60.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.character_blue),
                contentDescription = "파란 캐릭터",
                modifier = Modifier.size(60.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.character_green),
                contentDescription = "초록 캐릭터", 
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "${nickname}님의 투자 성향을\n알아보는 시간이에요!",
            style = HeadEb28,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "몇 가지 질문을 통해\n나만의 투자 스타일을 찾아보세요",
            style = BodyR16,
            color = Gray600,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onNextClick,
                modifier = Modifier
                    .width(120.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MainBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "시작하기",
                    style = TitleB16,
                    color = Color.White
                )
            }
        }
    }
}