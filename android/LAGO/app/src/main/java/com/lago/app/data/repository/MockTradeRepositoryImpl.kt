package com.lago.app.data.repository

import com.lago.app.data.local.prefs.UserPreferences
import com.lago.app.data.remote.api.ChartApiService
import com.lago.app.data.remote.ApiService
import com.lago.app.data.remote.dto.*
import com.lago.app.data.cache.FavoriteCache
import com.lago.app.domain.entity.AccountBalance
import com.lago.app.domain.entity.MockTradeResult
import com.lago.app.domain.entity.OrderType
import com.lago.app.domain.entity.PagedResult
import com.lago.app.domain.entity.StockHolding
import com.lago.app.domain.entity.TradingHistory
import com.lago.app.domain.entity.StockDisplayInfo
import com.lago.app.domain.repository.MockTradeRepository
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockTradeRepositoryImpl @Inject constructor(
    private val apiService: ChartApiService,
    private val userApiService: ApiService,
    private val userPreferences: UserPreferences,
    private val favoriteCache: FavoriteCache
) : MockTradeRepository {

    // =====================================
    // 매수/매도 거래
    // =====================================

    override suspend fun buyStock(
        stockCode: String,
        quantity: Int,
        price: Int,
        accountType: Int
    ): Flow<Resource<MockTradeResult>> = flow {
        try {
            emit(Resource.Loading())
            
            val userId = userPreferences.getUserIdLong()
            if (userId == 0L) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val request = MockTradeRequest(
                userId = userId,
                stockCode = stockCode,
                tradeType = "BUY",
                quantity = quantity,
                price = price,
                accountType = accountType
            )

            apiService.buyStock(request)
            
            // 성공시 response data 없음 - 간단한 성공 응답만 생성
            val result = MockTradeResult(
                tradeId = System.currentTimeMillis(), // 임시 tradeId
                stockCode = stockCode,
                stockName = getStockNameByCode(stockCode),
                quantity = quantity,
                price = price,
                totalAmount = quantity * price.toLong(),
                commission = 0,
                tradeAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                remainingBalance = 0L // 실제 잔고는 별도 조회 필요
            )
            emit(Resource.Success(result))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("매수 주문 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun sellStock(
        stockCode: String,
        quantity: Int,
        price: Int,
        accountType: Int
    ): Flow<Resource<MockTradeResult>> = flow {
        try {
            emit(Resource.Loading())
            
            val userId = userPreferences.getUserIdLong()
            if (userId == 0L) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val request = MockTradeRequest(
                userId = userId,
                stockCode = stockCode,
                tradeType = "SELL",
                quantity = quantity,
                price = price,
                accountType = accountType
            )

            apiService.sellStock(request)
            
            // 성공시 response data 없음 - 간단한 성공 응답만 생성
            val result = MockTradeResult(
                tradeId = System.currentTimeMillis(), // 임시 tradeId
                stockCode = stockCode,
                stockName = getStockNameByCode(stockCode),
                quantity = quantity,
                price = price,
                totalAmount = quantity * price.toLong(),
                commission = 0,
                tradeAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                remainingBalance = 0L // 실제 잔고는 별도 조회 필요
            )
            emit(Resource.Success(result))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("매도 주문 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    // =====================================
    // 계좌 관리
    // =====================================

    override suspend fun getAccountBalance(): Flow<Resource<AccountBalance>> = flow {
        try {
            emit(Resource.Loading())
            
            val userId = userPreferences.getUserIdLong()
            if (userId == 0L) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            // TODO: 실제로는 사용자별 계좌 ID를 조회해야 함. 현재는 기본값 사용
            val defaultAccountId = 1001L
            val response = apiService.getAccount(defaultAccountId)
            
            val balance = AccountBalance(
                accountId = response.accountId,
                balance = response.balance,
                totalAsset = response.totalAsset,
                profit = response.profit,
                profitRate = response.profitRate,
                totalStockValue = response.totalStockValue,
                createdAt = response.createdAt,
                type = response.type
            )
            emit(Resource.Success(balance))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("계좌 정보 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun getStockHoldings(): Flow<Resource<List<StockHolding>>> = flow {
        try {
            emit(Resource.Loading())
            
            val userId = userPreferences.getUserIdLong()
            if (userId == 0L) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val response = apiService.getUserPortfolio(userId.toString())
            
            val holdings = response.map { dto ->
                StockHolding(
                    stockCode = dto.stockCode,
                    stockName = dto.stockName,
                    market = dto.market,
                    quantity = dto.quantity,
                    avgBuyPrice = dto.avgBuyPrice,
                    currentPrice = dto.currentPrice,
                    totalBuyAmount = dto.totalBuyAmount,
                    currentValue = dto.currentValue,
                    profitLoss = dto.profitLoss,
                    profitLossRate = dto.profitLossRate
                )
            }
            emit(Resource.Success(holdings))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("보유 주식 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun getStockHoldingsByUserId(userId: Int): Flow<Resource<List<StockHolding>>> = flow {
        try {
            emit(Resource.Loading())

            // getUserCurrentStatus API 사용 (내 포트폴리오와 동일)
            val response = userApiService.getUserCurrentStatus(userId, 0)
            
            val holdings = response.holdings.map { dto ->
                StockHolding(
                    stockCode = dto.stockCode,
                    stockName = dto.stockName,
                    market = "KOSPI", // 기본값 설정
                    quantity = dto.quantity,
                    avgBuyPrice = if (dto.quantity > 0) (dto.totalPurchaseAmount / dto.quantity).toInt() else 0,
                    currentPrice = 0, // 실시간 데이터에서 업데이트
                    totalBuyAmount = dto.totalPurchaseAmount,
                    currentValue = 0, // 실시간 데이터에서 계산
                    profitLoss = 0, // 실시간 데이터에서 계산
                    profitLossRate = 0.0 // 실시간 데이터에서 계산
                )
            }
            emit(Resource.Success(holdings))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("보유 주식 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun getAccountBalanceByUserId(userId: Int): Flow<Resource<AccountBalance>> = flow {
        try {
            emit(Resource.Loading())

            // getUserCurrentStatus API 사용 (내 포트폴리오와 동일)
            val response = userApiService.getUserCurrentStatus(userId, 0)
            
            // 보유 주식들의 총 구매금액 계산
            val totalPurchaseAmount = response.holdings.sumOf { it.totalPurchaseAmount }
            
            val accountBalance = AccountBalance(
                accountId = response.accountId,
                balance = response.balance,
                totalAsset = response.balance + totalPurchaseAmount, // 현금 + 주식구매금액으로 추정
                profit = 0L, // 계산 불가, 실시간 데이터 필요
                profitRate = response.profitRate,
                totalStockValue = totalPurchaseAmount, // 현재는 구매금액으로 설정, 실시간 업데이트 필요
                createdAt = "",
                type = "MOCK"
            )
            emit(Resource.Success(accountBalance))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("계좌 정보 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun getTradingHistory(
        stockCode: String?,
        buySell: String?,
        page: Int,
        size: Int
    ): Flow<Resource<PagedResult<TradingHistory>>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val response = apiService.getMockTradeHistory("Bearer $token", stockCode, buySell, page, size)
            
            if (response.success) {
                val histories = response.data.content.map { dto ->
                    TradingHistory(
                        tradeId = dto.tradeId,
                        stockCode = dto.stockCode,
                        stockName = dto.stockName,
                        buySell = dto.buySell,
                        quantity = dto.quantity,
                        price = dto.price,
                        totalAmount = dto.totalAmount,
                        commission = dto.commission,
                        tradeAt = dto.tradeAt
                    )
                }
                
                val pagedResult = PagedResult(
                    content = histories,
                    page = response.data.page,
                    size = response.data.size,
                    totalElements = response.data.totalElements,
                    totalPages = response.data.totalPages
                )
                emit(Resource.Success(pagedResult))
            } else {
                emit(Resource.Error(response.message ?: "거래 내역 조회 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("거래 내역 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun initializeAccount(initialBalance: Long): Flow<Resource<AccountBalance>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val request = InitializeAccountRequest(
                initialBalance = initialBalance,
                type = "MOCK"
            )

            val response = apiService.initializeAccount("Bearer $token", request)
            
            if (response.success) {
                val balance = AccountBalance(
                    accountId = response.data.accountId,
                    balance = response.data.balance,
                    totalAsset = response.data.totalAsset,
                    profit = response.data.profit,
                    profitRate = response.data.profitRate,
                    totalStockValue = response.data.totalStockValue,
                    createdAt = response.data.createdAt,
                    type = response.data.type
                )
                emit(Resource.Success(balance))
            } else {
                emit(Resource.Error(response.message ?: "계좌 초기화 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("계좌 초기화 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun resetAccount(): Flow<Resource<AccountBalance>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val response = apiService.resetAccount("Bearer $token")
            
            if (response.success && response.data != null) {
                val balance = AccountBalance(
                    accountId = response.data.accountId,
                    balance = response.data.balance,
                    totalAsset = response.data.totalAsset,
                    profit = response.data.profit,
                    profitRate = response.data.profitRate,
                    totalStockValue = response.data.totalStockValue,
                    createdAt = response.data.createdAt,
                    type = response.data.type
                )
                emit(Resource.Success(balance))
            } else {
                emit(Resource.Error(response.message ?: "계좌 리셋 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("계좌 리셋 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    // =====================================
    // 주식 정보
    // =====================================

    override suspend fun getStockList(
        market: String?,
        category: String?,
        sort: String,
        page: Int,
        size: Int
    ): Flow<Resource<PagedResult<StockDisplayInfo>>> = flow {
        try {
            emit(Resource.Loading())

            val response = apiService.getStockListWithRealtime(market, category, sort, page, size)
            
            if (response.success) {
                val stocks = response.data.content.map { dto ->
                    StockDisplayInfo(
                        stockInfoId = dto.stockInfoId,
                        code = dto.code,
                        name = dto.name,
                        market = dto.market,
                        currentPrice = dto.currentPrice,
                        openPrice = dto.openPrice,
                        highPrice = dto.highPrice,
                        lowPrice = dto.lowPrice,
                        volume = dto.volume,
                        priceChange = dto.priceChange,
                        priceChangeRate = dto.priceChangeRate,
                        updatedAt = dto.updatedAt,
                        isFavorite = false // 기본값 추가
                    )
                }
                
                val pagedResult = PagedResult(
                    content = stocks,
                    page = response.data.page,
                    size = response.data.size,
                    totalElements = response.data.totalElements,
                    totalPages = response.data.totalPages
                )
                emit(Resource.Success(pagedResult))
            } else {
                emit(Resource.Error(response.message ?: "주식 목록 조회 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("주식 목록 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun getStockDisplayInfo(stockCode: String): Flow<Resource<StockDisplayInfo>> = flow {
        try {
            emit(Resource.Loading())

            val response = apiService.getStockInfoWithRealtime(stockCode)
            
            if (response.success) {
                val stock = StockDisplayInfo(
                    stockInfoId = response.data.stockInfoId,
                    code = response.data.code,
                    name = response.data.name,
                    market = response.data.market,
                    currentPrice = response.data.currentPrice,
                    openPrice = response.data.openPrice,
                    highPrice = response.data.highPrice,
                    lowPrice = response.data.lowPrice,
                    volume = response.data.volume,
                    priceChange = response.data.priceChange,
                    priceChangeRate = response.data.priceChangeRate,
                    updatedAt = response.data.updatedAt
                )
                emit(Resource.Success(stock))
            } else {
                emit(Resource.Error(response.message ?: "주식 정보 조회 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("주식 정보 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun searchStocks(
        query: String,
        market: String?,
        page: Int,
        size: Int
    ): Flow<Resource<PagedResult<StockDisplayInfo>>> = flow {
        try {
            emit(Resource.Loading())

            val response = apiService.searchStocksWithRealtime(query, market, page, size)
            
            if (response.success) {
                val stocks = response.data.content.map { dto ->
                    StockDisplayInfo(
                        stockInfoId = dto.stockInfoId,
                        code = dto.code,
                        name = dto.name,
                        market = dto.market,
                        currentPrice = dto.currentPrice,
                        openPrice = dto.openPrice,
                        highPrice = dto.highPrice,
                        lowPrice = dto.lowPrice,
                        volume = dto.volume,
                        priceChange = dto.priceChange,
                        priceChangeRate = dto.priceChangeRate,
                        updatedAt = dto.updatedAt,
                        isFavorite = false // 기본값 추가
                    )
                }
                
                val pagedResult = PagedResult(
                    content = stocks,
                    page = response.data.page,
                    size = response.data.size,
                    totalElements = response.data.totalElements,
                    totalPages = response.data.totalPages
                )
                emit(Resource.Success(pagedResult))
            } else {
                emit(Resource.Error(response.message ?: "주식 검색 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("주식 검색 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    // =====================================
    // 관심종목 관리
    // =====================================

    override suspend fun addToFavorites(stockCode: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val request = FavoriteStockRequest(stockCode = stockCode)
            val response = apiService.addToFavorites("Bearer $token", request)
            
            emit(Resource.Success(response.success))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("관심종목 추가 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun removeFromFavorites(stockCode: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val response = apiService.removeFromFavorites("Bearer $token", stockCode)
            
            emit(Resource.Success(response.success))
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("관심종목 삭제 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun getFavoriteStocks(): Flow<Resource<List<StockDisplayInfo>>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val response = apiService.getFavoriteStocks("Bearer $token")
            
            if (response.success) {
                val favorites = response.data.map { dto ->
                    StockDisplayInfo(
                        stockInfoId = dto.stockInfoId,
                        code = dto.code,
                        name = dto.name,
                        market = dto.market,
                        currentPrice = dto.currentPrice,
                        openPrice = dto.openPrice,
                        highPrice = dto.highPrice,
                        lowPrice = dto.lowPrice,
                        volume = dto.volume,
                        priceChange = dto.priceChange,
                        priceChangeRate = dto.priceChangeRate,
                        updatedAt = dto.updatedAt,
                        isFavorite = true
                    )
                }
                emit(Resource.Success(favorites))
            } else {
                emit(Resource.Error(response.message ?: "관심종목 조회 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("관심종목 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    override suspend fun isFavorite(stockCode: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val token = userPreferences.getAccessToken()
            if (token.isNullOrEmpty()) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }

            val response = apiService.getFavoriteStocks("Bearer $token")
            
            if (response.success) {
                val isFav = response.data.any { it.code == stockCode }
                emit(Resource.Success(isFav))
            } else {
                emit(Resource.Error(response.message ?: "관심종목 확인 실패"))
            }
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("관심종목 확인 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }

    // =====================================
    // 유틸리티 함수
    // =====================================

    private fun handleHttpError(exception: HttpException): String {
        return when (exception.code()) {
            401 -> "로그인이 만료되었습니다. 다시 로그인해주세요"
            403 -> "접근 권한이 없습니다"
            404 -> "요청한 데이터를 찾을 수 없습니다"
            500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
            else -> "네트워크 오류가 발생했습니다 (${exception.code()})"
        }
    }
    
    /**
     * 주식 코드로 주식명 조회 - API에서 가져오도록 수정 필요
     * 현재는 임시로 빈 값 반환
     */
    private fun getStockNameByCode(stockCode: String): String {
        // TODO: 실제 API에서 종목명 조회하도록 수정
        return ""
    }
    
    // =====================================
    // 새로운 관심종목 기능 (API 기반)
    // =====================================
    
    override suspend fun toggleFavorite(stockCode: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            val userId = userPreferences.getUserIdLong()
            if (userId == 0L) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }
            
            // Optimistic Update: 즉시 캐시 업데이트
            val wasAdded = favoriteCache.toggle(stockCode)
            android.util.Log.d("MockTradeRepository", "💖 관심종목 토글 (캐시): $stockCode → ${if (wasAdded) "추가" else "제거"}")
            
            try {
                // API 호출
                apiService.toggleFavoriteStock(userId, stockCode)
                android.util.Log.d("MockTradeRepository", "💖 관심종목 토글 (API 성공): $stockCode")
                emit(Resource.Success(wasAdded))
            } catch (e: Exception) {
                // API 실패 시 캐시 롤백
                favoriteCache.toggle(stockCode)
                android.util.Log.e("MockTradeRepository", "💖 관심종목 토글 (API 실패, 캐시 롤백): $stockCode", e)
                throw e
            }
            
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("관심종목 토글 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }
    
    override suspend fun loadUserFavorites(): Flow<Resource<Set<String>>> = flow {
        try {
            emit(Resource.Loading())
            
            val userId = userPreferences.getUserIdLong()
            if (userId == 0L) {
                emit(Resource.Error("로그인이 필요합니다"))
                return@flow
            }
            
            // 새로운 API로 사용자 관심종목 조회
            val favorites = apiService.getUserFavoriteStocks(userId)
            val stockCodes = favorites.map { it.stockCode }.toSet()
            
            // 캐시 업데이트
            favoriteCache.updateCache(stockCodes)
            android.util.Log.d("MockTradeRepository", "💖 사용자 관심종목 로드 완료: ${stockCodes.size}개")
            
            emit(Resource.Success(stockCodes))
            
        } catch (e: HttpException) {
            emit(Resource.Error(handleHttpError(e)))
        } catch (e: IOException) {
            emit(Resource.Error("네트워크 연결을 확인해주세요"))
        } catch (e: Exception) {
            emit(Resource.Error("관심종목 목록 조회 중 오류가 발생했습니다: ${e.localizedMessage}"))
        }
    }
}