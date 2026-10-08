package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 주식 정보 DTO (STOCK_INFO + TICKS 테이블 조인)
 */
data class StockInfoDto(
    @SerializedName("stockInfoId")
    val stockInfoId: Int,
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("market")
    val market: String,          // "KOSPI", "KOSDAQ"
    @SerializedName("currentPrice")
    val currentPrice: Int,       // TICKS.close_price (최신)
    @SerializedName("openPrice")
    val openPrice: Int,          // TICKS.open_price
    @SerializedName("highPrice")
    val highPrice: Int,          // TICKS.high_price
    @SerializedName("lowPrice")
    val lowPrice: Int,           // TICKS.low_price
    @SerializedName("volume")
    val volume: Long,            // TICKS.volume
    @SerializedName("priceChange")
    val priceChange: Int,        // 전일 대비 등락
    @SerializedName("priceChangeRate")
    val priceChangeRate: Double, // 등락률 %
    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * 주식 목록 페이징 응답 DTO
 */
data class StockListResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: StockListPageDto
)

data class StockListPageDto(
    @SerializedName("content")
    val content: List<StockInfoDto>,
    @SerializedName("page")
    val page: Int,
    @SerializedName("size")
    val size: Int,
    @SerializedName("totalElements")
    val totalElements: Long,
    @SerializedName("totalPages")
    val totalPages: Int
)

/**
 * 개별 주식 정보 응답 DTO
 */
data class StockInfoResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: StockInfoDto
)

/**
 * 관심종목 추가/삭제 요청 DTO (INTEREST 테이블 기반)
 */
data class FavoriteStockRequest(
    @SerializedName("stockCode")
    val stockCode: String
)

/**
 * 관심종목 목록 응답 DTO
 */
data class FavoriteStocksResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: List<StockInfoDto>
)

/**
 * 주식 검색 요청을 위한 Query Parameters는 별도 데이터 클래스로 관리
 */
data class StockSearchParams(
    val query: String?,      // 검색어 (종목명 또는 종목코드)
    val market: String? = null,  // "KOSPI", "KOSDAQ"
    val page: Int = 0,
    val size: Int = 20
)

/**
 * 주식 목록 조회 요청을 위한 Query Parameters
 */
data class StockListParams(
    val market: String? = null,  // "KOSPI", "KOSDAQ"
    val category: String? = null, // "trending", "volume" 등
    val sort: String = "code",   // "code", "name", "price", "changeRate"
    val page: Int = 0,
    val size: Int = 20
)

/**
 * 사용자별 관심종목 DTO (새로운 API 응답용)
 */
data class UserFavoriteStockDto(
    @SerializedName("interestId")
    val interestId: Int,
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String
)