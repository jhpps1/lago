package com.lago.app.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lago.app.util.rememberStockLogo
import com.lago.app.util.toStockCode

/**
 * 주식 로고 컴포넌트
 * 
 * @param stockCode 주식 종목 코드 (예: "005930", "AAPL")
 * @param stockName 주식 이름 (로고가 없을 때 표시할 이니셜용)
 * @param size 로고 크기
 * @param cornerRadius 모서리 둥글기
 */
@Composable
fun StockLogo(
    stockCode: String,
    stockName: String = "",
    size: Dp = 40.dp,
    cornerRadius: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val cleanStockCode = stockCode.toStockCode()
    val logo = rememberStockLogo(stockCode = cleanStockCode)
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (logo == null) MaterialTheme.colorScheme.surfaceVariant 
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (logo != null) {
            Image(
                bitmap = logo,
                contentDescription = "$stockName 로고",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // 로고가 없을 때 이니셜 표시
            Text(
                text = getStockInitials(stockName, stockCode),
                fontSize = (size.value / 3).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 원형 주식 로고 컴포넌트
 */
@Composable
fun CircularStockLogo(
    stockCode: String,
    stockName: String = "",
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    StockLogo(
        stockCode = stockCode,
        stockName = stockName,
        size = size,
        cornerRadius = size / 2, // 원형
        modifier = modifier
    )
}

/**
 * 작은 주식 로고 (리스트 아이템용)
 */
@Composable
fun SmallStockLogo(
    stockCode: String,
    stockName: String = "",
    modifier: Modifier = Modifier
) {
    StockLogo(
        stockCode = stockCode,
        stockName = stockName,
        size = 32.dp,
        cornerRadius = 6.dp,
        modifier = modifier
    )
}

/**
 * 큰 주식 로고 (상세 화면용)
 */
@Composable
fun LargeStockLogo(
    stockCode: String,
    stockName: String = "",
    modifier: Modifier = Modifier
) {
    StockLogo(
        stockCode = stockCode,
        stockName = stockName,
        size = 64.dp,
        cornerRadius = 12.dp,
        modifier = modifier
    )
}

/**
 * 주식 이름이나 코드에서 이니셜 추출
 */
private fun getStockInitials(stockName: String, stockCode: String): String {
    return when {
        stockName.isNotBlank() -> {
            // 한글인 경우 첫 글자만
            if (stockName.first().code > 127) {
                stockName.take(1)
            } else {
                // 영문인 경우 단어별 첫 글자
                stockName.split(" ", "-")
                    .take(2)
                    .map { it.firstOrNull()?.uppercaseChar() ?: "" }
                    .joinToString("")
                    .ifEmpty { stockName.take(2).uppercase() }
            }
        }
        stockCode.isNotBlank() -> stockCode.take(2).uppercase()
        else -> "?"
    }
}