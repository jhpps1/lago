package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 계좌 잔고 정보 DTO (ACCOUNTS 테이블 기반)
 */
data class AccountBalanceDto(
    @SerializedName("accountId")
    val accountId: Long,
    @SerializedName("balance")
    val balance: Long,      // 현금 잔고
    @SerializedName("totalAsset") 
    val totalAsset: Long,   // 총 자산
    @SerializedName("profit")
    val profit: Long,       // 수익
    @SerializedName("profitRate")
    val profitRate: Double, // 수익률
    @SerializedName("totalStockValue")
    val totalStockValue: Long, // 보유 주식 총 평가액 (계산값)
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("type")
    val type: String        // 계정 타입
)

/**
 * 계좌 잔고 응답 DTO
 */
data class AccountBalanceResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data") 
    val data: AccountBalanceDto
)

/**
 * 보유 주식 정보 DTO (STOCK_HOLDING 테이블 기반)
 */
data class StockHoldingDto(
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("market")
    val market: String,
    @SerializedName("quantity")
    val quantity: Int,           // 보유량
    @SerializedName("avgBuyPrice")
    val avgBuyPrice: Int,        // 평균 매수가
    @SerializedName("currentPrice")
    val currentPrice: Int,       // 현재가 (TICKS 테이블에서)
    @SerializedName("totalBuyAmount")
    val totalBuyAmount: Long,    // 총 매수금액
    @SerializedName("currentValue")
    val currentValue: Long,      // 현재 평가액 (quantity * currentPrice)
    @SerializedName("profitLoss")
    val profitLoss: Long,        // 손익 (currentValue - totalBuyAmount)
    @SerializedName("profitLossRate")
    val profitLossRate: Double   // 수익률 % ((currentPrice - avgBuyPrice) / avgBuyPrice * 100)
)

/**
 * 보유 주식 목록 응답 DTO
 */
data class StockHoldingsResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: StockHoldingsDataDto
)

data class StockHoldingsDataDto(
    @SerializedName("holdings")
    val holdings: List<StockHoldingDto>,
    @SerializedName("totalValue")
    val totalValue: Long,        // 총 평가액
    @SerializedName("totalProfitLoss")
    val totalProfitLoss: Long,   // 총 손익
    @SerializedName("totalProfitLossRate")
    val totalProfitLossRate: Double  // 총 수익률
)

/**
 * 계좌 초기화 요청 DTO  
 */
data class InitializeAccountRequest(
    @SerializedName("initialBalance")
    val initialBalance: Long = 10000000L,  // 초기 자금 (1천만원)
    @SerializedName("type")
    val type: String = "MOCK"              // 계정 타입
)

/**
 * 계좌 리셋 응답 DTO
 */
data class AccountResetResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: AccountBalanceDto? = null
)