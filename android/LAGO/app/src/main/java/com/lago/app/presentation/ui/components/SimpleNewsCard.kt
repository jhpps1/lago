package com.lago.app.presentation.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lago.app.R
import com.lago.app.domain.entity.News
import com.lago.app.presentation.theme.*
import com.lago.app.util.formatTimeAgo

/**
 * 역사적 챌린지용 간단한 뉴스 카드
 * 호재/악재 배지와 AI 요약 없이 제목과 시간만 표시
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SimpleNewsCard(
    news: News,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = ShadowColor,
                ambientColor = ShadowColor
            )
            .height(100.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = news.title,
                    style = SubtitleSb14,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                
                // Time
                Text(
                    text = formatTimeAgo(news.publishedAt),
                    style = BodyR12,
                    color = Gray700
                )
            }
            
            // News Image
            Image(
                painter = painterResource(id = R.drawable.megaphone_image),
                contentDescription = "뉴스 이미지",
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .padding(8.dp)
            )
        }
    }
}