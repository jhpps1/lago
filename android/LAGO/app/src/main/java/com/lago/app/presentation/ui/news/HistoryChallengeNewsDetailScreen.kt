package com.lago.app.presentation.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lago.app.presentation.theme.*
import com.lago.app.presentation.ui.components.CommonTopAppBar
import com.lago.app.presentation.viewmodel.HistoryChallengeNewsDetailViewModel
import com.lago.app.presentation.viewmodel.HistoryChallengeNewsDetailUiState
import com.lago.app.domain.entity.HistoryChallengeNews
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryChallengeNewsDetailScreen(
    challengeNewsId: String = "1",
    onBackClick: () -> Unit = {},
    viewModel: HistoryChallengeNewsDetailViewModel = hiltViewModel()
) {
    val newsDetailState by viewModel.newsDetailState.collectAsStateWithLifecycle()
    
    LaunchedEffect(challengeNewsId) {
        val id = challengeNewsId.toIntOrNull() ?: 1
        viewModel.loadNewsDetail(id)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Top App Bar
        CommonTopAppBar(
            title = "역사적 챌린지 뉴스",
            onBackClick = onBackClick
        )
        
        when (newsDetailState) {
            is HistoryChallengeNewsDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MainBlue)
                }
            }
            is HistoryChallengeNewsDetailUiState.Success -> {
                HistoryChallengeNewsDetailContent(
                    news = (newsDetailState as HistoryChallengeNewsDetailUiState.Success).news
                )
            }
            is HistoryChallengeNewsDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "뉴스를 불러올 수 없습니다",
                            style = TitleB16,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                val id = challengeNewsId.toIntOrNull() ?: 1
                                viewModel.loadNewsDetail(id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MainBlue)
                        ) {
                            Text("다시 시도", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChallengeNewsDetailContent(
    news: HistoryChallengeNews
) {
    // Scrollable Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // News Title
        Text(
            text = news.title,
            style = TitleB20,
            color = Color.Black,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Date
        Text(
            text = formatHistoryChallengeDateTime(news.publishedAt),
            style = BodyR14,
            color = Gray700,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Main Image (imageUrl 또는 content의 첫 번째 URL)
        val mainImageUrl = if (news.imageUrl.isNotEmpty()) {
            news.imageUrl
        } else {
            extractFirstImageUrlFromHistoryNews(news.content)
        }
        
        if (mainImageUrl.isNotBlank()) {
            var showMainImage by remember { mutableStateOf(true) }
            
            if (showMainImage) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mainImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "뉴스 대표 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onError = {
                        showMainImage = false
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // News Content with embedded images (첫 번째 이미지 제외)
        HistoryChallengeNewsContentWithImages(content = news.content, skipFirstImage = true)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HistoryChallengeNewsContentWithImages(content: String, skipFirstImage: Boolean = false) {
    val context = LocalContext.current

    // {} 안의 URL을 찾아서 분리하는 로직
    val urlPattern = Regex("\\{([^}]+)\\}")
    val parts = content.split(urlPattern)
    val urls = urlPattern.findAll(content).map { it.groupValues[1] }.toList()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        var urlIndex = 0

        parts.forEachIndexed { index, part ->
            if (part.isNotBlank()) {
                Text(
                    text = part.trim().replace("", ""),
                    style = BodyR16,
                    color = Color.Black,
                    lineHeight = 24.sp
                )
            }

            // URL이 있으면 이미지로 표시 (첫 번째 이미지는 skipFirstImage에 따라 건너뛰기)
            if (urlIndex < urls.size && index < parts.size - 1) {
                val imageUrl = urls[urlIndex].trim()
                
                if (imageUrl.isNotBlank() && !(skipFirstImage && urlIndex == 0)) {
                    var showImage by remember { mutableStateOf(true) }

                    if (showImage) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "뉴스 이미지",
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            onError = {
                                showImage = false
                            }
                        )
                    }
                }
                urlIndex++
            }
        }
    }
}

fun formatHistoryChallengeDateTime(dateTimeStr: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm")
        val dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter)
        dateTime.format(outputFormatter)
    } catch (e: Exception) {
        dateTimeStr // 파싱 실패 시 원본 반환
    }
}

fun extractFirstImageUrlFromHistoryNews(content: String): String {
    val urlPattern = Regex("\\{([^}]+)\\}")
    val urls = urlPattern.findAll(content).map { it.groupValues[1] }.toList()
    return if (urls.isNotEmpty()) urls[0].trim() else ""
}