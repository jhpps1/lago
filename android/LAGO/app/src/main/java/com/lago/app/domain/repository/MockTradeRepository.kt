package com.lago.app.domain.repository

import com.lago.app.data.remote.dto.*
import com.lago.app.domain.entity.*
import com.lago.app.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * 모의투자 관련 Repository 인터페이스
 * 실제 DB 구조 (MOCK_TRADE, ACCOUNTS, STOCK_HOLDING, INTEREST 테이블) 기반
 */
interface MockTradeRepository {
    
    // =====================================
    // 매수/매도 거래
    // =====================================
    
    /**
     * 주식 매수 (MOCK_TRADE 테이블에 INSERT)
     */
    suspend fun buyStock(
        stockCode: String,
        quantity: Int,
        price: Int,
        accountType: Int = 0  // 0=실시간모의투자, 1=역사챌린지, 2=자동매매봇
    ): Flow<Resource<MockTradeResult>>
    
    /**
     * 주식 매도 (MOCK_TRADE 테이블에 INSERT)
     */
    suspend fun sellStock(
        stockCode: String,
        quantity: Int,
        price: Int,
        accountType: Int = 0  // 0=실시간모의투자, 1=역사챌린지, 2=자동매매봇
    ): Flow<Resource<MockTradeResult>>
    
    // =====================================
    // 계좌 관리
    // =====================================
    
    /**
     * 계좌 잔고 조회 (ACCOUNTS 테이블)
     */
    suspend fun getAccountBalance(): Flow<Resource<AccountBalance>>
    
    /**
     * 특정 유저의 계좌 잔고 조회 (ACCOUNTS 테이블)
     */
    suspend fun getAccountBalanceByUserId(userId: Int): Flow<Resource<AccountBalance>>
    
    /**
     * 보유 주식 현황 조회 (STOCK_HOLDING + STOCK_INFO + TICKS 조인)
     */
    suspend fun getStockHoldings(): Flow<Resource<List<StockHolding>>>
    
    /**
     * 특정 유저의 보유 주식 현황 조회 (STOCK_HOLDING + STOCK_INFO + TICKS 조인)
     */
    suspend fun getStockHoldingsByUserId(userId: Int): Flow<Resource<List<StockHolding>>>
    
    /**
     * 거래 내역 조회 (MOCK_TRADE 테이블 페이징)
     */
    suspend fun getTradingHistory(
        stockCode: String? = null,
        buySell: String? = null, // "BUY" or "SELL"
        page: Int = 0,
        size: Int = 20
    ): Flow<Resource<PagedResult<TradingHistory>>>
    
    /**
     * 계좌 초기화 (신규 가입시)
     */
    suspend fun initializeAccount(
        initialBalance: Long = 10000000L
    ): Flow<Resource<AccountBalance>>
    
    /**
     * 모의투자 리셋
     */
    suspend fun resetAccount(): Flow<Resource<AccountBalance>>
    
    // =====================================
    // 주식 정보 (실시간 가격 포함)
    // =====================================
    
    /**
     * 주식 목록 조회 (STOCK_INFO + TICKS 조인)
     */
    suspend fun getStockList(
        market: String? = null, // "KOSPI", "KOSDAQ"
        category: String? = null,
        sort: String = "code",
        page: Int = 0,
        size: Int = 20
    ): Flow<Resource<PagedResult<StockDisplayInfo>>>
    
    /**
     * 개별 주식 정보 조회 (실시간 가격 포함)
     */
    suspend fun getStockDisplayInfo(stockCode: String): Flow<Resource<StockDisplayInfo>>
    
    /**
     * 주식 검색
     */
    suspend fun searchStocks(
        query: String,
        market: String? = null,
        page: Int = 0,
        size: Int = 20
    ): Flow<Resource<PagedResult<StockDisplayInfo>>>
    
    // =====================================
    // 관심종목 관리 (INTEREST 테이블)
    // =====================================
    
    /**
     * 관심종목 추가
     */
    suspend fun addToFavorites(stockCode: String): Flow<Resource<Boolean>>
    
    /**
     * 관심종목 삭제
     */
    suspend fun removeFromFavorites(stockCode: String): Flow<Resource<Boolean>>
    
    /**
     * 관심종목 목록 조회
     */
    suspend fun getFavoriteStocks(): Flow<Resource<List<StockDisplayInfo>>>
    
    /**
     * 관심종목 여부 확인
     */
    suspend fun isFavorite(stockCode: String): Flow<Resource<Boolean>>
    
    /**
     * 관심종목 토글 (새로운 API 기반)
     */
    suspend fun toggleFavorite(stockCode: String): Flow<Resource<Boolean>>
    
    /**
     * 사용자 관심종목 목록 로드 (캐시 초기화용)
     */
    suspend fun loadUserFavorites(): Flow<Resource<Set<String>>>
}