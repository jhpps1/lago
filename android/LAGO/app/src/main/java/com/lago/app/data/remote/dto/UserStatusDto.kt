package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 사용자 현재 계좌 상황 응답 DTO
 * GET /api/users/{userId}/current-status
 */
data class UserCurrentStatusDto(
    @SerializedName("accountId") 
    val accountId: Long,
    
    @SerializedName("balance") 
    val balance: Long,
    
    @SerializedName("profitRate") 
    val profitRate: Double,
    
    @SerializedName("holdings") 
    val holdings: List<HoldingResponseDto>,
    
    @SerializedName("userId") 
    val userId: Long,
    
    @SerializedName("nickname") 
    val nickname: String,
    
    @SerializedName("personality") 
    val personality: String
)

data class HoldingResponseDto(
    @SerializedName("stockCode") 
    val stockCode: String,
    
    @SerializedName("stockName") 
    val stockName: String,
    
    @SerializedName("quantity") 
    val quantity: Int,
    
    @SerializedName("totalPurchaseAmount") 
    val totalPurchaseAmount: Long
)

/**
 * 마이페이지용 포트폴리오 요약 데이터
 */
data class MyPagePortfolioSummary(
    val accountId: Long,
    val balance: Long,
    val totalPurchaseAmount: Long,
    val totalCurrentValue: Long,
    val profitLoss: Long,
    val profitRate: Double,
    val holdings: List<MyPageHolding>,
    val nickname: String? = null,
    val personality: String? = null
)

data class MyPageHolding(
    val stockCode: String,
    val stockName: String,
    val quantity: Int,
    val purchaseAmount: Long,
    val currentValue: Long,
    val profitLoss: Long,
    val profitRate: Double,
    val weight: Double // 총 매수 기준 비율
)

/**
 * 파이차트용 데이터 (상위 5개 + 기타)
 */
data class PieChartItem(
    val name: String,
    val value: Long,
    val percentage: Double,
    val color: androidx.compose.ui.graphics.Color,
    val isOthers: Boolean = false
)

/**
 * 역사챌린지 조회 응답 DTO
 * GET /api/history-challenge
 */
data class HistoryChallengeDto(
    @SerializedName("challengeId")
    val challengeId: Int,
    
    @SerializedName("theme")
    val theme: String,
    
    @SerializedName("stockName")
    val stockName: String,
    
    @SerializedName("stockCode")
    val stockCode: String,
    
    @SerializedName("startDate")
    val startDate: String,
    
    @SerializedName("endDate")
    val endDate: String,
    
    @SerializedName("originDate")
    val originDate: String,
    
    @SerializedName("currentPrice")
    val currentPrice: Long,
    
    @SerializedName("fluctuationPrice")
    val fluctuationPrice: Long,
    
    @SerializedName("fluctuationRate")
    val fluctuationRate: Double
)