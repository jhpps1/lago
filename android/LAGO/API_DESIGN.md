# 실제 DB 구조 기반 모의투자 API 설계

## 1. 매수/매도 거래 API

### 1.1 주식 매수
```http
POST /api/mock-trade/buy
Authorization: Bearer {token}
Content-Type: application/json

{
  "stockCode": "005930",    // STOCK_INFO.code
  "quantity": 10,           // MOCK_TRADE.quantity
  "price": 75000           // MOCK_TRADE.price (주당 가격)
}
```

**응답:**
```json
{
  "success": true,
  "message": "매수 주문이 체결되었습니다",
  "data": {
    "tradeId": 12345,        // MOCK_TRADE.trade_id
    "stockCode": "005930",
    "stockName": "삼성전자",
    "quantity": 10,
    "price": 75000,
    "totalAmount": 750000,   // quantity * price
    "commission": 3750,      // MOCK_TRADE.commission (0.5%)
    "tradeAt": "2025-08-13T10:30:00",
    "remainingBalance": 9246250  // 거래 후 잔고
  }
}
```

### 1.2 주식 매도
```http
POST /api/mock-trade/sell
Authorization: Bearer {token}
Content-Type: application/json

{
  "stockCode": "005930",
  "quantity": 5,
  "price": 76000
}
```

## 2. 계좌 관리 API

### 2.1 계좌 정보 조회
```http
GET /api/accounts/balance
Authorization: Bearer {token}
```

**응답 (ACCOUNTS 테이블 기반):**
```json
{
  "success": true,
  "data": {
    "accountId": 1,          // ACCOUNTS.account_id
    "balance": 9246250,      // ACCOUNTS.balance (현금 잔고)
    "totalAsset": 10000000,  // ACCOUNTS.total_asset (총 자산)
    "profit": 753750,        // ACCOUNTS.profit (수익)
    "profitRate": 8.13,      // ACCOUNTS.profit_rate (수익률 %)
    "totalStockValue": 753750, // 보유 주식 총 평가액 (계산값)
    "createdAt": "2025-08-01T00:00:00",
    "type": "MOCK"           // ACCOUNTS.type
  }
}
```

### 2.2 보유 주식 현황 조회
```http
GET /api/accounts/holdings
Authorization: Bearer {token}
```

**응답 (STOCK_HOLDING 테이블 기반):**
```json
{
  "success": true,
  "data": {
    "holdings": [
      {
        "stockCode": "005930",    // STOCK_INFO.code
        "stockName": "삼성전자",   // STOCK_INFO.name
        "market": "KOSPI",        // STOCK_INFO.market
        "quantity": 10,           // STOCK_HOLDING.quantity (보유량)
        "avgBuyPrice": 75000,     // STOCK_HOLDING.avg_buy_price
        "currentPrice": 76000,    // 실시간 가격 (TICKS 테이블에서)
        "totalBuyAmount": 750000, // STOCK_HOLDING.total_buy_amount
        "currentValue": 760000,   // 현재 평가액 (quantity * currentPrice)
        "profitLoss": 10000,      // 손익 (currentValue - totalBuyAmount)
        "profitLossRate": 1.33    // 수익률 % ((currentPrice - avgBuyPrice) / avgBuyPrice * 100)
      }
    ],
    "totalValue": 760000,
    "totalProfitLoss": 10000,
    "totalProfitLossRate": 1.33
  }
}
```

### 2.3 거래 내역 조회
```http
GET /api/accounts/transactions
Authorization: Bearer {token}
Query Parameters:
- stockCode (optional): 특정 종목만 조회
- buySell (optional): "BUY" or "SELL"
- page: 페이지 번호 (default: 0)
- size: 페이지 크기 (default: 20)
```

**응답 (MOCK_TRADE 테이블 기반):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "tradeId": 12345,        // MOCK_TRADE.trade_id
        "stockCode": "005930",   // STOCK_INFO.code
        "stockName": "삼성전자",  // STOCK_INFO.name
        "buySell": "BUY",        // MOCK_TRADE.buy_sell
        "quantity": 10,          // MOCK_TRADE.quantity
        "price": 75000,          // MOCK_TRADE.price
        "totalAmount": 750000,   // quantity * price
        "commission": 3750,      // MOCK_TRADE.commission
        "tradeAt": "2025-08-13T10:30:00"  // MOCK_TRADE.trade_at
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

## 3. 주식 정보 API

### 3.1 주식 목록 조회 (기존 수정)
```http
GET /api/stocks/list
Query Parameters:
- market (optional): "KOSPI", "KOSDAQ"
- page: 페이지 번호
- size: 페이지 크기
```

**응답 (STOCK_INFO + TICKS 테이블 조인):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "stockInfoId": 1,        // STOCK_INFO.stock_info_id
        "code": "005930",        // STOCK_INFO.code
        "name": "삼성전자",       // STOCK_INFO.name
        "market": "KOSPI",       // STOCK_INFO.market
        "currentPrice": 76000,   // TICKS.close_price (최신)
        "openPrice": 75500,      // TICKS.open_price
        "highPrice": 76500,      // TICKS.high_price
        "lowPrice": 75000,       // TICKS.low_price
        "volume": 15000000,      // TICKS.volume
        "priceChange": 500,      // 전일 대비 등락
        "priceChangeRate": 0.66, // 등락률 %
        "updatedAt": "2025-08-13T15:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 3.2 관심종목 관리 (INTEREST 테이블 기반)
```http
# 관심종목 추가
POST /api/stocks/favorites
Authorization: Bearer {token}
{
  "stockCode": "005930"
}

# 관심종목 삭제  
DELETE /api/stocks/favorites/{stockCode}
Authorization: Bearer {token}

# 관심종목 목록
GET /api/stocks/favorites
Authorization: Bearer {token}
```

## 4. 필요한 DTO 클래스

### 매수/매도 요청 DTO
```kotlin
data class MockTradeRequest(
    val stockCode: String,
    val quantity: Int,
    val price: Int  // 주당 가격
)

data class MockTradeResponse(
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
```

### 계좌 정보 DTO
```kotlin
data class AccountBalanceDto(
    val accountId: Long,
    val balance: Long,      // 현금 잔고
    val totalAsset: Long,   // 총 자산
    val profit: Long,       // 수익
    val profitRate: Double, // 수익률
    val totalStockValue: Long, // 보유 주식 총 평가액
    val createdAt: String,
    val type: String
)
```

### 보유 주식 DTO  
```kotlin
data class StockHoldingDto(
    val stockCode: String,
    val stockName: String,
    val market: String,
    val quantity: Int,           // 보유량
    val avgBuyPrice: Int,        // 평균 매수가
    val currentPrice: Int,       // 현재가
    val totalBuyAmount: Long,    // 총 매수금액
    val currentValue: Long,      // 현재 평가액
    val profitLoss: Long,        // 손익
    val profitLossRate: Double   // 수익률
)
```

### 거래내역 DTO
```kotlin
data class MockTradeDto(
    val tradeId: Long,
    val stockCode: String,
    val stockName: String,
    val buySell: String,    // "BUY" or "SELL"
    val quantity: Int,
    val price: Int,
    val totalAmount: Long,
    val commission: Int,
    val tradeAt: String
)
```

### 주식 정보 DTO
```kotlin
data class StockInfoDto(
    val stockInfoId: Int,
    val code: String,
    val name: String,
    val market: String,
    val currentPrice: Int,
    val openPrice: Int,
    val highPrice: Int,
    val lowPrice: Int,
    val volume: Long,
    val priceChange: Int,
    val priceChangeRate: Double,
    val updatedAt: String
)
```

## 5. Repository 메서드 설계

```kotlin
interface MockTradeRepository {
    // 매수/매도
    suspend fun buyStock(stockCode: String, quantity: Int, price: Int): Flow<Resource<MockTradeResponse>>
    suspend fun sellStock(stockCode: String, quantity: Int, price: Int): Flow<Resource<MockTradeResponse>>
    
    // 계좌 관리
    suspend fun getAccountBalance(): Flow<Resource<AccountBalanceDto>>
    suspend fun getStockHoldings(): Flow<Resource<List<StockHoldingDto>>>
    suspend fun getTradingHistory(stockCode: String?, buySell: String?, page: Int, size: Int): Flow<Resource<PagedResult<MockTradeDto>>>
    
    // 주식 정보
    suspend fun getStockList(market: String?, page: Int, size: Int): Flow<Resource<PagedResult<StockInfoDto>>>
    suspend fun getStockInfo(stockCode: String): Flow<Resource<StockInfoDto>>
}
```

이제 실제 DB 구조에 맞춰 API가 설계되었습니다. 기존 하드코딩된 부분들을 이 스키마에 맞춰 수정해야 합니다.