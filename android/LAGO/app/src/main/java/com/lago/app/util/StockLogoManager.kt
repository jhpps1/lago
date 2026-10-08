package com.lago.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 주식 종목 로고 관리 매니저
 * 
 * 기능:
 * - assets/logos/ 폴더의 logo_{종목코드}.png 파일들을 관리
 * - 메모리 캐싱으로 성능 최적화
 * - Compose에서 사용하기 쉬운 API 제공
 */
object StockLogoManager {
    
    private val logoCache = mutableMapOf<String, ImageBitmap?>()
    
    /**
     * 종목 코드로 로고 파일명 생성
     */
    private fun getLogoFileName(stockCode: String): String {
        return "logos/logo_$stockCode.png"
    }
    
    /**
     * Assets에서 로고 로드 (suspend 함수)
     */
    private suspend fun loadLogoFromAssets(context: Context, stockCode: String): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = getLogoFileName(stockCode)
                val inputStream = context.assets.open(fileName)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap?.asImageBitmap()
            } catch (e: IOException) {
                null // 로고 파일이 없는 경우
            }
        }
    }
    
    /**
     * 종목 코드로 로고 가져오기 (캐시 활용)
     */
    suspend fun getStockLogo(context: Context, stockCode: String): ImageBitmap? {
        // 캐시에서 먼저 확인
        if (logoCache.containsKey(stockCode)) {
            return logoCache[stockCode]
        }
        
        // 캐시에 없으면 로드하고 캐시에 저장
        val logo = loadLogoFromAssets(context, stockCode)
        logoCache[stockCode] = logo
        return logo
    }
    
    /**
     * 로고 존재 여부 확인
     */
    fun hasLogo(context: Context, stockCode: String): Boolean {
        return try {
            val fileName = getLogoFileName(stockCode)
            context.assets.open(fileName).use { true }
        } catch (e: IOException) {
            false
        }
    }
    
    /**
     * 캐시 정리
     */
    fun clearCache() {
        logoCache.clear()
    }
}

/**
 * Compose에서 주식 로고를 사용하기 위한 Composable 함수
 * 
 * @param stockCode 주식 종목 코드 (예: "005930")
 * @param fallback 로고가 없을 때 표시할 기본 ImageBitmap
 * @return ImageBitmap? (로고 이미지 또는 null)
 */
@Composable
fun rememberStockLogo(
    stockCode: String,
    fallback: ImageBitmap? = null
): ImageBitmap? {
    val context = LocalContext.current
    var logo by remember(stockCode) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(stockCode) {
        logo = StockLogoManager.getStockLogo(context, stockCode) ?: fallback
    }
    
    return logo
}

/**
 * 종목 코드 정리 유틸리티
 * - 앞의 0을 제거하지 않고 6자리 유지
 */
fun String.toStockCode(): String {
    return when {
        this.length < 6 -> this.padStart(6, '0')
        this.length > 6 -> this.take(6)
        else -> this
    }
}