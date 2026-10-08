package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 모의투자 매수/매도 요청 DTO (백엔드 TradeRequest 형식)
 */
data class MockTradeRequest(
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("tradeType")
    val tradeType: String? = null,  // "BUY" or "SELL" (통합 API 사용시)
    @SerializedName("quantity") 
    val quantity: Int,
    @SerializedName("price")
    val price: Int,  // 주당 가격
    @SerializedName("accountType")
    val accountType: Int = 0  // 0=실시간모의투자, 1=역사챌린지, 2=자동매매봇
)

/**
 * 백엔드 TradeResponse 형식에 맞는 응답 DTO
 */
data class TradeApiResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("tradeId")
    val tradeId: Long,
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("tradeType")
    val tradeType: String,  // "BUY" or "SELL"
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("executedPrice")
    val executedPrice: Int,
    @SerializedName("totalAmount")
    val totalAmount: Long,
    @SerializedName("commission")
    val commission: Int,
    @SerializedName("tax")
    val tax: Int,
    @SerializedName("remainingBalance")
    val remainingBalance: Long
)

/**
 * 기존 MockTradeResponse (내부 사용용)
 */
data class MockTradeResponse(
    @SerializedName("tradeId")
    val tradeId: Long,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("price")
    val price: Int,
    @SerializedName("totalAmount")
    val totalAmount: Long,
    @SerializedName("commission")
    val commission: Int,
    @SerializedName("tradeAt")
    val tradeAt: String,
    @SerializedName("remainingBalance")
    val remainingBalance: Long
)

/**
 * 거래내역 DTO (MOCK_TRADE 테이블 기반)
 */
data class MockTradeHistoryDto(
    @SerializedName("tradeId")
    val tradeId: Long,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("buySell")
    val buySell: String,    // "BUY" or "SELL"
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("price")
    val price: Int,
    @SerializedName("totalAmount")
    val totalAmount: Long,
    @SerializedName("commission")
    val commission: Int,
    @SerializedName("tradeAt")
    val tradeAt: String
)

/**
 * 거래내역 페이징 응답 DTO
 */
data class MockTradeHistoryResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message") 
    val message: String? = null,
    @SerializedName("data")
    val data: MockTradeHistoryPageDto
)

data class MockTradeHistoryPageDto(
    @SerializedName("content")
    val content: List<MockTradeHistoryDto>,
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
 * 백엔드 StockHoldingResponse 형식 (포트폴리오)
 */
data class StockHoldingApiResponse(
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("market")
    val market: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("avgBuyPrice")
    val avgBuyPrice: Int,
    @SerializedName("currentPrice")
    val currentPrice: Int,
    @SerializedName("totalBuyAmount")
    val totalBuyAmount: Long,
    @SerializedName("currentValue")
    val currentValue: Long,
    @SerializedName("profitLoss")
    val profitLoss: Long,
    @SerializedName("profitLossRate")
    val profitLossRate: Double
)

/**
 * 백엔드 AccountDto 형식
 */
data class AccountDto(
    @SerializedName("accountId")
    val accountId: Long,
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("balance")
    val balance: Long,
    @SerializedName("totalAsset")
    val totalAsset: Long,
    @SerializedName("profit")
    val profit: Long,
    @SerializedName("profitRate")
    val profitRate: Double,
    @SerializedName("totalStockValue")
    val totalStockValue: Long,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("type")
    val type: String
)

/**
 * 백엔드 StockHoldingResponse 형식
 */
data class StockHoldingResponse(
    @SerializedName("holdingId")
    val holdingId: Long,
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("averagePrice")
    val averagePrice: Int,
    @SerializedName("totalCost")
    val totalCost: Int,
    @SerializedName("currentPrice")
    val currentPrice: Int,
    @SerializedName("currentValue")
    val currentValue: Int,
    @SerializedName("profitLoss")
    val profitLoss: Int,
    @SerializedName("profitLossRate")
    val profitLossRate: Float,
    @SerializedName("firstPurchaseDate")
    val firstPurchaseDate: String?,
    @SerializedName("lastTradeDate")
    val lastTradeDate: String?,
    @SerializedName("market")
    val market: String?,
    @SerializedName("sector")
    val sector: String?
)

/**
 * 백엔드 AccountCurrentStatusResponse 형식
 */
data class AccountCurrentStatusResponse(
    @SerializedName("accountId")
    val accountId: Long,
    @SerializedName("balance")
    val balance: Int,
    @SerializedName("profitRate")
    val profitRate: Double,
    @SerializedName("holdings")
    val holdings: List<CurrentHoldingInfo>,
    @SerializedName("userId")
    val userId: Long,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("personality")
    val personality: String
)

/**
 * 백엔드 CurrentHoldingInfo 형식
 */
data class CurrentHoldingInfo(
    @SerializedName("stockCode")
    val stockCode: String,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("totalPurchaseAmount")
    val totalPurchaseAmount: Int
)

/**
 * 백엔드 TransactionHistoryResponse 형식 (AccountController용)
 */
data class TransactionHistoryResponse(
    @SerializedName("tradeId")
    val tradeId: Long,
    @SerializedName("accountId")
    val accountId: Long,
    @SerializedName("stockName")
    val stockName: String?,
    @SerializedName("stockId")
    val stockId: String?,
    @SerializedName("quantity")
    val quantity: Int?,
    @SerializedName("buySell")
    val buySell: String?,
    @SerializedName("price")
    val price: Int,
    @SerializedName("tradeAt")
    val tradeAt: String,
    @SerializedName("isQuiz")
    val isQuiz: Boolean
)