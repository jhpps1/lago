package com.lago.app.data.service

import android.util.Log
import com.lago.app.data.remote.api.ChartApiService
import com.lago.app.data.remote.dto.StockDayDto
import com.lago.app.util.MarketTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 초기 가격 계산을 위한 일봉 데이터 서비스
 * 일주일치 일봉 데이터에서 최신 거래일의 종가를 추출
 */
@Singleton
class InitialPriceService @Inject constructor(
    private val chartApiService: ChartApiService
) {
    
    companion object {
        private const val TAG = "InitialPriceService"
        private const val WEEK_DAYS = 7
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    /**
     * 일주일치 일봉 데이터에서 최신 거래일 종가 추출
     * @param stockCodes 종목 코드 리스트
     * @return Map<종목코드, 최신 거래일 종가>
     */
    suspend fun getLatestClosePrices(stockCodes: List<String>): Map<String, Int> = withContext(Dispatchers.IO) {
        if (stockCodes.isEmpty()) {
            Log.d(TAG, "종목 코드 리스트가 비어있음")
            return@withContext emptyMap()
        }
        
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -WEEK_DAYS)
        val startDate = calendar.time
        val startDateStr = DATE_FORMATTER.format(startDate)
        val endDateStr = DATE_FORMATTER.format(endDate)
        
        Log.d(TAG, "일주일치 일봉 조회 시작: ${stockCodes.size}개 종목 ($startDateStr ~ $endDateStr)")
        
        val results = mutableMapOf<String, Int>()
        
        // 병렬 처리로 성능 최적화
        val deferredResults = stockCodes.map { stockCode ->
            async {
                try {
                    val latestClosePrice = getLatestClosePriceForStock(stockCode, startDateStr, endDateStr)
                    if (latestClosePrice != null && latestClosePrice > 0) {
                        stockCode to latestClosePrice
                    } else {
                        Log.w(TAG, "최신 종가 조회 실패: $stockCode")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "종목 처리 중 오류: $stockCode", e)
                    null
                }
            }
        }
        
        // 모든 비동기 작업 완료 대기
        deferredResults.awaitAll().filterNotNull().forEach { (stockCode, price) ->
            results[stockCode] = price
        }
        
        Log.d(TAG, "일주일치 일봉 조회 완료: ${results.size}/${stockCodes.size}개 성공")
        return@withContext results
    }
    
    /**
     * 단일 종목의 최신 거래일 종가 조회
     * @param stockCode 종목 코드
     * @param startDate 시작 날짜 (yyyy-MM-dd)
     * @param endDate 종료 날짜 (yyyy-MM-dd)
     * @return 최신 거래일 종가 (실패 시 null)
     */
    private suspend fun getLatestClosePriceForStock(
        stockCode: String, 
        startDate: String, 
        endDate: String
    ): Int? {
        return try {
            Log.v(TAG, "주식 시세 데이터 조회: $stockCode")
            Log.d(TAG, "🔍 종목코드 형태 확인: '$stockCode' (길이: ${stockCode.length}자)")
            
            // 새로운 API 사용: /api/stocks/{stockCode}?interval=DAY&fromDateTime=...&toDateTime=...
            val calendar = Calendar.getInstance()
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -14)
            val twoWeeksAgo = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
            
            Log.d(TAG, "🌐 API 호출: GET /api/stocks/$stockCode?interval=DAY&fromDateTime=$twoWeeksAgo&toDateTime=$now")
            
            val priceDataList = chartApiService.getStockPriceData(stockCode, "DAY", twoWeeksAgo, now)
            
            Log.d(TAG, "📡 API 응답: ${priceDataList.size}개 데이터 수신")
            
            if (priceDataList.isEmpty()) {
                Log.w(TAG, "주식 시세 데이터 없음: $stockCode")
                return null
            } else {
                Log.d(TAG, "✅ 첫 번째 데이터: ${priceDataList[0]}")
            }
            
            // 날짜순 정렬 후 최신 데이터 선택 (StockPriceDataDto 사용)
            val latestData = priceDataList
                .filter { it.closePrice > 0 } // 유효한 종가만 필터링
                .maxByOrNull { it.bucket } // 가장 최신 날짜
            
            if (latestData != null) {
                Log.d(TAG, "최신 종가 추출: $stockCode = ${latestData.closePrice} (${latestData.bucket})")
                latestData.closePrice.toInt()
            } else {
                Log.w(TAG, "유효한 종가 없음: $stockCode")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 주식 시세 데이터 조회 실패: $stockCode", e)
            Log.e(TAG, "💥 에러 타입: ${e.javaClass.simpleName}")
            Log.e(TAG, "💥 에러 메시지: ${e.message}")
            if (e is retrofit2.HttpException) {
                Log.e(TAG, "💥 HTTP 상태: ${e.code()}")
                Log.e(TAG, "💥 HTTP 메시지: ${e.message()}")
                try {
                    Log.e(TAG, "💥 응답 본문: ${e.response()?.errorBody()?.string()}")
                } catch (ex: Exception) {
                    Log.e(TAG, "💥 응답 본문 읽기 실패", ex)
                }
            }
            null
        }
    }
    
    /**
     * 특정 종목의 최신 거래일 종가 조회 (단일 종목용)
     * @param stockCode 종목 코드
     * @return 최신 거래일 종가 (실패 시 null)
     */
    suspend fun getLatestClosePrice(stockCode: String): Int? = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val endDateStr = DATE_FORMATTER.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -WEEK_DAYS)
        val startDateStr = DATE_FORMATTER.format(calendar.time)
        
        return@withContext getLatestClosePriceForStock(stockCode, startDateStr, endDateStr)
    }
    
    /**
     * 최신 거래일 종가와 변동률 정보 함께 조회
     * @param stockCodes 종목 코드 리스트
     * @return Map<종목코드, PriceInfo>
     */
    suspend fun getLatestPriceInfo(stockCodes: List<String>): Map<String, PriceInfo> = withContext(Dispatchers.IO) {
        if (stockCodes.isEmpty()) {
            return@withContext emptyMap()
        }
        
        val calendar = Calendar.getInstance()
        val endDateStr = DATE_FORMATTER.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -WEEK_DAYS)
        val startDateStr = DATE_FORMATTER.format(calendar.time)
        
        Log.d(TAG, "가격 정보 조회 시작: ${stockCodes.size}개 종목")
        
        val results = mutableMapOf<String, PriceInfo>()
        
        val deferredResults = stockCodes.map { stockCode ->
            async {
                try {
                    val priceInfo = getPriceInfoForStock(stockCode, startDateStr, endDateStr)
                    if (priceInfo != null) {
                        stockCode to priceInfo
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "가격 정보 조회 실패: $stockCode", e)
                    null
                }
            }
        }
        
        deferredResults.awaitAll().filterNotNull().forEach { (stockCode, priceInfo) ->
            results[stockCode] = priceInfo
        }
        
        Log.d(TAG, "가격 정보 조회 완료: ${results.size}/${stockCodes.size}개 성공")
        return@withContext results
    }
    
    /**
     * 단일 종목의 가격 정보 조회
     */
    private suspend fun getPriceInfoForStock(
        stockCode: String,
        startDate: String,  // 예: "2024-08-01T09:00:00"
        endDate: String     // 예: "2024-08-16T15:30:00"
    ): PriceInfo? {
        return try {
            // 2주간 데이터로 API 호출
            val calendar = Calendar.getInstance()
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -14)
            val twoWeeksAgo = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(calendar.time)
            
            Log.d(TAG, "🌐 API 호출: GET /api/stocks/$stockCode?interval=DAY&fromDateTime=$twoWeeksAgo&toDateTime=$now")
            
            val priceDataList = chartApiService.getStockPriceData(
                stockCode = stockCode,
                interval = "DAY",
                fromDateTime = twoWeeksAgo,
                toDateTime = now
            )

            if (priceDataList.isEmpty()) return null

            val latestData = priceDataList
                .filter { it.closePrice > 0 }
                .maxByOrNull { it.bucket } ?: return null

            val previousData = priceDataList
                .filter { it.closePrice > 0 && it.bucket < latestData.bucket }
                .maxByOrNull { it.bucket }

            val changePrice = previousData?.let { (latestData.closePrice - it.closePrice).toInt() } ?: 0
            val changeRate = previousData?.takeIf { it.closePrice > 0 }?.let {
                ((latestData.closePrice - it.closePrice).toDouble() / it.closePrice) * 100
            } ?: 0.0

            PriceInfo(
                closePrice = latestData.closePrice.toInt(),
                changePrice = changePrice,
                changeRate = changeRate,
                date = latestData.bucket
            )
        } catch (e: Exception) {
            Log.e(TAG, "가격 정보 처리 실패: $stockCode", e)
            null
        }
    }


    /**
     * 서비스 상태 정보 (디버깅용)
     */
    fun getServiceStatus(): String {
        val calendar = Calendar.getInstance()
        val endDateStr = DATE_FORMATTER.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -WEEK_DAYS)
        val startDateStr = DATE_FORMATTER.format(calendar.time)
        
        return """
            InitialPriceService 상태:
            - 현재 시각: ${MarketTimeUtils.getCurrentKoreaTime()}
            - 조회 기간: $startDateStr ~ $endDateStr
            - 시장 상태: ${MarketTimeUtils.getMarketStatusString()}
        """.trimIndent()
    }
}

/**
 * 가격 정보 데이터 클래스
 */
data class PriceInfo(
    val closePrice: Int,
    val changePrice: Int,
    val changeRate: Double,
    val date: String
)