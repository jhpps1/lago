package com.lago.app.data.service

import android.util.Log
import com.lago.app.data.remote.api.ChartApiService
import com.lago.app.data.remote.dto.StockDayDto
import com.lago.app.data.remote.dto.StockPriceDataDto
import com.lago.app.util.MarketTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 장 마감 시 종가 데이터 처리 서비스
 */
@Singleton
class CloseDataService @Inject constructor(
    private val chartApiService: ChartApiService
) {
    
    companion object {
        private const val TAG = "CloseDataService"
    }
    
    /**
     * 특정 종목의 종가 조회
     * @param stockCode 종목 코드 (예: "005930")
     * @return 종가 (없으면 null)
     */
    suspend fun getClosePrice(stockCode: String): Int? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "종가 조회 시작: $stockCode")
            
            val targetDate = MarketTimeUtils.getTargetDateForClosePrice()
            Log.d(TAG, "조회 날짜: $targetDate")
            
            var stockDayData = fetchStockDayData(stockCode, targetDate)
            
            // 데이터가 없거나 종가가 0이면 전날 시도
            if (stockDayData == null || stockDayData.closePrice == 0) {
                Log.w(TAG, "당일 데이터 없음 또는 종가 0, 전날 시도: $stockCode")
                val yesterdayDate = MarketTimeUtils.getYesterdayString()
                stockDayData = fetchStockDayData(stockCode, yesterdayDate)
            }
            
            val closePrice = stockDayData?.closePrice
            Log.d(TAG, "종가 조회 완료: $stockCode = $closePrice")
            
            closePrice
            
        } catch (e: Exception) {
            Log.e(TAG, "종가 조회 실패: $stockCode", e)
            null
        }
    }
    
    /**
     * 여러 종목의 종가 일괄 조회
     * @param stockCodes 종목 코드 리스트
     * @return Map<종목코드, 종가>
     */
    suspend fun getMultipleClosePrices(stockCodes: List<String>): Map<String, Int> = withContext(Dispatchers.IO) {
        Log.d(TAG, "여러 종목 종가 조회 시작: ${stockCodes.size}개")
        
        val results = mutableMapOf<String, Int>()
        
        stockCodes.forEach { stockCode ->
            try {
                val closePrice = getClosePrice(stockCode)
                if (closePrice != null && closePrice > 0) {
                    results[stockCode] = closePrice
                    Log.d(TAG, "종가 저장: $stockCode = $closePrice")
                } else {
                    Log.w(TAG, "종가 조회 실패: $stockCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "종목 처리 중 오류: $stockCode", e)
            }
        }
        
        Log.d(TAG, "여러 종목 종가 조회 완료: ${results.size}개 성공")
        return@withContext results
    }
    
    /**
     * 실제 API 호출 (재사용 가능한 내부 함수)
     */
    private suspend fun fetchStockDayData(stockCode: String, date: String): StockDayDto? {
        return try {
            Log.d(TAG, "API 호출: $stockCode, $date")
            
            // 새로운 API 사용 (하루치 데이터)
            val startDateTime = "${date}T09:00:00"
            val endDateTime = "${date}T15:30:00"
            val encodedStart = java.net.URLEncoder.encode(startDateTime, "UTF-8")
            val encodedEnd = java.net.URLEncoder.encode(endDateTime, "UTF-8")
            
            val response = chartApiService.getStockPriceData(stockCode, "DAY", encodedStart, encodedEnd)
            
            if (response.isNotEmpty()) {
                val priceData = response[0]
                Log.d(TAG, "API 응답 성공: $stockCode, 종가=${priceData.closePrice}")
                
                // StockPriceDataDto를 StockDayDto로 변환
                StockDayDto(
                    date = priceData.bucket.split("T")[0], // "2024-08-13T09:00:00" -> "2024-08-13"
                    openPrice = priceData.openPrice.toInt(),
                    highPrice = priceData.highPrice.toInt(),
                    lowPrice = priceData.lowPrice.toInt(),
                    closePrice = priceData.closePrice.toInt(),
                    volume = priceData.volume.toInt(),
                    fluctuationRate = 0.0f // 기본값으로 0 설정
                )
            } else {
                Log.w(TAG, "API 응답 빈 배열: $stockCode, $date")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "API 호출 실패: $stockCode, $date", e)
            null
        }
    }
    
    /**
     * 포트폴리오 보유 종목들의 종가 조회
     * @param portfolioStockCodes 보유 종목 코드 리스트
     * @return Map<종목코드, 종가>
     */
    suspend fun getPortfolioClosePrices(portfolioStockCodes: List<String>): Map<String, Int> {
        if (portfolioStockCodes.isEmpty()) {
            Log.d(TAG, "보유 종목 없음")
            return emptyMap()
        }
        
        Log.d(TAG, "포트폴리오 종가 조회: ${portfolioStockCodes.joinToString()}")
        return getMultipleClosePrices(portfolioStockCodes)
    }
    
    /**
     * 현재 시장 상태에 따른 가격 데이터 조회
     * @param stockCode 종목 코드
     * @return 현재 시장 상태에 적합한 가격 (실시간 또는 종가)
     */
    suspend fun getCurrentPrice(stockCode: String): Int? {
        return if (MarketTimeUtils.isMarketOpen()) {
            Log.d(TAG, "장 중 - 실시간 가격 사용: $stockCode")
            // 장 중에는 WebSocket에서 처리하므로 여기서는 null 반환
            null
        } else {
            Log.d(TAG, "장 마감 - 종가 조회: $stockCode")
            getClosePrice(stockCode)
        }
    }
    
    /**
     * 디버깅용 상태 정보
     */
    fun getServiceStatus(): String {
        return """
            CloseDataService 상태:
            - 현재 시각: ${MarketTimeUtils.getCurrentKoreaTime()}
            - 시장 상태: ${MarketTimeUtils.getMarketStatusString()}
            - 조회 대상 날짜: ${MarketTimeUtils.getTargetDateForClosePrice()}
        """.trimIndent()
    }
}