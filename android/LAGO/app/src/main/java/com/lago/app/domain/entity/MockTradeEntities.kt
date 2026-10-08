package com.lago.app.domain.entity

/**
 * 매수/매도 거래 결과 엔티티
 */
data class MockTradeResult(
    val tradeId: Long,
    val stockCode: String,
    val stockName: String,
    val quantity: Int,
    val price: Int,
    val totalAmount: Long,
    val commission: Int,
    val tradeAt: String,
    val remainingBalance: Long
)

/**
 * 계좌 잔고 정보 엔티티 (ACCOUNTS 테이블 기반)
 */
data class AccountBalance(
    val accountId: Long,
    val balance: Long,          // 현금 잔고
    val totalAsset: Long,       // 총 자산
    val profit: Long,           // 수익
    val profitRate: Double,     // 수익률 (%)
    val totalStockValue: Long,  // 보유 주식 총 평가액
    val createdAt: String,
    val type: String            // 계정 타입
)

/**
 * 보유 주식 정보 엔티티 (STOCK_HOLDING 테이블 기반)
 */
data class StockHolding(
    val stockCode: String,
    val stockName: String,
    val market: String,
    val quantity: Int,              // 보유량
    val avgBuyPrice: Int,           // 평균 매수가
    val currentPrice: Int,          // 현재가
    val totalBuyAmount: Long,       // 총 매수금액
    val currentValue: Long,         // 현재 평가액
    val profitLoss: Long,           // 손익
    val profitLossRate: Double      // 수익률 (%)
)

/**
 * 거래 내역 엔티티 (MOCK_TRADE 테이블 기반)
 */
data class TradingHistory(
    val tradeId: Long,
    val stockCode: String,
    val stockName: String,
    val buySell: String,        // "BUY" or "SELL"
    val quantity: Int,
    val price: Int,
    val totalAmount: Long,
    val commission: Int,
    val tradeAt: String
)

/**
 * 주식 기본 정보 엔티티 (STOCK_INFO 테이블)
 */
data class StockInfoEntity(
    val stockInfoId: Int,
    val code: String,
    val name: String,
    val market: String  // "KOSPI", "KOSDAQ"
)

/**
 * 주식 가격 정보 엔티티 (TICKS 테이블)
 */
data class StockPriceData(
    val stockInfoId: Int,
    val timestamp: String,
    val openPrice: Int,
    val highPrice: Int,
    val lowPrice: Int,
    val closePrice: Int,  // 현재가
    val volume: Long
)

/**
 * UI 표시용 주식 정보 (STOCK_INFO + TICKS 조인 결과)
 */
data class StockDisplayInfo(
    val stockInfoId: Int,
    val code: String,
    val name: String,
    val market: String,             // "KOSPI", "KOSDAQ"
    val currentPrice: Int,          // 현재가 (TICKS.close_price)
    val openPrice: Int,             // 시가
    val highPrice: Int,             // 고가
    val lowPrice: Int,              // 저가
    val volume: Long,               // 거래량
    val priceChange: Int,           // 전일 대비 등락
    val priceChangeRate: Double,    // 등락률 (%)
    val updatedAt: String,
    val isFavorite: Boolean = false // UI 표시용
)

/**
 * 페이징 결과 엔티티
 */
data class PagedResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

/**
 * 매수/매도 주문 타입
 */
enum class OrderType(val value: String) {
    BUY("BUY"),
    SELL("SELL")
}

/**
 * 계좌 타입
 */
enum class AccountType(val value: String) {
    MOCK("MOCK"),
    REAL("REAL") // 향후 확장용
}

/**
 * 시장 구분
 */
enum class MarketType(val value: String) {
    KOSPI("KOSPI"),
    KOSDAQ("KOSDAQ"),
    ALL("ALL")
}

/**
 * 정렬 타입
 */
enum class StockSortType(val value: String) {
    CODE("code"),
    NAME("name"),
    PRICE("price"),
    CHANGE_RATE("changeRate"),
    VOLUME("volume")
}