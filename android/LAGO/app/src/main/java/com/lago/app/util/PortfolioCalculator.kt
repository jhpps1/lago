package com.lago.app.util

import com.lago.app.domain.entity.PortfolioReturn
import com.lago.app.domain.entity.StockRealTimeData
import com.lago.app.domain.entity.TotalPortfolioSummary
import com.lago.app.domain.entity.HoldingItem
import com.lago.app.data.remote.dto.UserCurrentStatusDto
import com.lago.app.data.remote.dto.HoldingResponseDto
import com.lago.app.data.remote.dto.MyPagePortfolioSummary
import com.lago.app.data.remote.dto.MyPageHolding
import com.lago.app.data.remote.dto.PieChartItem
import androidx.compose.ui.graphics.Color

object PortfolioCalculator {
    
    /**
     * API 응답에서 마이페이지 포트폴리오 계산 (실시간 가격 적용)
     */
    fun calculateMyPagePortfolio(
        userStatus: UserCurrentStatusDto,
        realTimePrices: Map<String, StockRealTimeData>
    ): MyPagePortfolioSummary {
        android.util.Log.d("PortfolioCalculator", "📈 포트폴리오 계산 시작")
        android.util.Log.d("PortfolioCalculator", "  - 실시간 가격 데이터: ${realTimePrices.size}개")
        realTimePrices.forEach { (code, data) ->
            android.util.Log.d("PortfolioCalculator", "    * $code: ${data.price}원")
        }
        
        val portfolioReturns = userStatus.holdings.map { holding ->
            val realTimeData = realTimePrices[holding.stockCode]
            android.util.Log.d("PortfolioCalculator", "  - ${holding.stockName} 계산:")
            android.util.Log.d("PortfolioCalculator", "    * 수량: ${holding.quantity}주")
            android.util.Log.d("PortfolioCalculator", "    * 매수금액: ${holding.totalPurchaseAmount}원")
            android.util.Log.d("PortfolioCalculator", "    * 실시간 가격: ${realTimeData?.price ?: "없음"}")
            
            val result = calculateHoldingReturn(holding, realTimeData)
            android.util.Log.d("PortfolioCalculator", "    * 현재가치: ${result.currentValue}원")
            android.util.Log.d("PortfolioCalculator", "    * 손익: ${result.profitLoss}원")
            result
        }
        
        val totalPurchaseAmount = userStatus.holdings.sumOf { it.totalPurchaseAmount }
        val totalCurrentValue = portfolioReturns.sumOf { it.currentValue }
        val profitLoss = totalCurrentValue - totalPurchaseAmount
        val profitRate = if (totalPurchaseAmount > 0) (profitLoss.toDouble() / totalPurchaseAmount) * 100 else 0.0
        
        android.util.Log.d("PortfolioCalculator", "💰 최종 계산 결과:")
        android.util.Log.d("PortfolioCalculator", "  - 보유현금: ${userStatus.balance}원")
        android.util.Log.d("PortfolioCalculator", "  - 총매수: ${totalPurchaseAmount}원")
        android.util.Log.d("PortfolioCalculator", "  - 총평가: ${totalCurrentValue}원")
        android.util.Log.d("PortfolioCalculator", "  - 총자산: ${userStatus.balance + totalCurrentValue}원")
        android.util.Log.d("PortfolioCalculator", "  - 평가손익: ${profitLoss}원 (${String.format("%.2f", profitRate)}%)")
        android.util.Log.d("PortfolioCalculator", "  - API profitRate: ${userStatus.profitRate}% vs 계산된 profitRate: ${profitRate}%")
        
        return MyPagePortfolioSummary(
            accountId = userStatus.accountId,
            balance = userStatus.balance,
            totalPurchaseAmount = totalPurchaseAmount,
            totalCurrentValue = totalCurrentValue,
            profitLoss = profitLoss, // 계산된 평가손익
            profitRate = userStatus.profitRate, // API의 수익률 (그래프 중앙용)
            holdings = portfolioReturns,
            nickname = userStatus.nickname,
            personality = userStatus.personality
        )
    }
    
    /**
     * 개별 보유 종목 수익률 계산
     */
    private fun calculateHoldingReturn(
        holding: HoldingResponseDto,
        realTimeData: StockRealTimeData?
    ): MyPageHolding {
        val currentPrice = realTimeData?.price ?: (holding.totalPurchaseAmount.toDouble() / holding.quantity)
        val currentValue = currentPrice * holding.quantity
        val purchaseAmount = holding.totalPurchaseAmount.toDouble()
        val profitLoss = currentValue - purchaseAmount
        val profitRate = if (purchaseAmount > 0) (profitLoss / purchaseAmount) * 100 else 0.0
        
        return MyPageHolding(
            stockCode = holding.stockCode,
            stockName = holding.stockName,
            quantity = holding.quantity,
            purchaseAmount = holding.totalPurchaseAmount,
            currentValue = currentValue.toLong(),
            profitLoss = profitLoss.toLong(),
            profitRate = profitRate,
            weight = 0.0 // 나중에 전체 기준으로 계산
        )
    }
    
    /**
     * 보유 비율 계산 (총 매수 기준)
     */
    fun calculateHoldingWeights(holdings: List<MyPageHolding>): List<MyPageHolding> {
        val totalPurchaseAmount = holdings.sumOf { it.purchaseAmount }
        android.util.Log.d("PortfolioCalculator", "📊 비율 계산:")
        android.util.Log.d("PortfolioCalculator", "  - 총 매수금액: ${totalPurchaseAmount}원")
        
        return holdings.map { holding ->
            val weight = if (totalPurchaseAmount > 0) {
                (holding.purchaseAmount.toDouble() / totalPurchaseAmount) * 100
            } else 0.0
            
            android.util.Log.d("PortfolioCalculator", "  - ${holding.stockName}: ${holding.purchaseAmount}원 → ${String.format("%.1f", weight)}%")
            
            holding.copy(weight = weight)
        }
    }
    
    /**
     * 파이차트용 데이터 생성 (상위 5개 + 기타)
     */
    fun createPieChartData(holdings: List<MyPageHolding>): List<PieChartItem> {
        val sortedHoldings = holdings.sortedByDescending { it.purchaseAmount }
        val top5 = sortedHoldings.take(5)
        val others = sortedHoldings.drop(5)
        
        val pieChartData = mutableListOf<PieChartItem>()
        
        // 상위 5개 추가
        top5.forEach { holding ->
            pieChartData.add(
                PieChartItem(
                    name = holding.stockName,
                    value = holding.purchaseAmount,
                    percentage = holding.weight,
                    color = getStockColor(pieChartData.size)
                )
            )
        }
        
        // 기타 묶어서 추가
        if (others.isNotEmpty()) {
            val othersValue = others.sumOf { it.purchaseAmount }
            val othersPercentage = others.sumOf { it.weight }
            pieChartData.add(
                PieChartItem(
                    name = "기타",
                    value = othersValue,
                    percentage = othersPercentage,
                    color = Color.Gray,
                    isOthers = true
                )
            )
        }
        
        return pieChartData
    }
    
    /**
     * 단일 종목의 실시간 수익률 계산 (기존 로직 유지)
     */
    fun calculateStockReturn(
        holding: HoldingItem,
        realTimePrice: Double
    ): PortfolioReturn {
        val quantity = holding.quantity
        val buyPrice = holding.averagePrice.toDouble()
        
        val profit = (realTimePrice - buyPrice) * quantity
        val returnRate = if (buyPrice > 0) ((realTimePrice - buyPrice) / buyPrice) * 100 else 0.0
        val totalValue = realTimePrice * quantity
        
        return PortfolioReturn(
            stockCode = holding.stockCode,
            stockName = holding.stockName,
            quantity = quantity,
            buyPrice = buyPrice,
            currentPrice = realTimePrice,
            profit = profit,
            returnRate = returnRate,
            totalValue = totalValue
        )
    }
    
    /**
     * 전체 포트폴리오의 실시간 수익률 계산
     */
    fun calculateTotalPortfolioReturn(
        holdings: List<HoldingItem>,
        realTimePrices: Map<String, StockRealTimeData>
    ): TotalPortfolioSummary {
        val stockReturns = holdings.mapNotNull { holding ->
            val realTimeData = realTimePrices[holding.stockCode]
            if (realTimeData != null) {
                calculateStockReturn(holding, realTimeData.price)
            } else null
        }
        
        val totalInvestment = stockReturns.sumOf { it.buyPrice * it.quantity }
        val totalCurrentValue = stockReturns.sumOf { it.totalValue }
        val totalProfit = totalCurrentValue - totalInvestment
        val totalReturnRate = if (totalInvestment > 0) (totalProfit / totalInvestment) * 100 else 0.0
        
        return TotalPortfolioSummary(
            totalInvestment = totalInvestment,
            totalCurrentValue = totalCurrentValue,
            totalProfit = totalProfit,
            totalReturnRate = totalReturnRate,
            stockReturns = stockReturns
        )
    }
    
    /**
     * 배치로 여러 종목의 수익률 한번에 계산
     */
    fun batchCalculateReturns(
        holdings: List<HoldingItem>,
        realTimePrices: Map<String, StockRealTimeData>
    ): Map<String, PortfolioReturn> {
        return holdings.associate { holding ->
            val realTimeData = realTimePrices[holding.stockCode]
            val portfolioReturn = if (realTimeData != null) {
                calculateStockReturn(holding, realTimeData.price)
            } else {
                // 실시간 데이터가 없으면 기존 가격으로 계산
                val quantity = holding.quantity
                val buyPrice = holding.averagePrice.toDouble()
                PortfolioReturn(
                    stockCode = holding.stockCode,
                    stockName = holding.stockName,
                    quantity = quantity,
                    buyPrice = buyPrice,
                    currentPrice = buyPrice,
                    profit = 0.0,
                    returnRate = 0.0,
                    totalValue = holding.totalValue.toDouble()
                )
            }
            holding.stockCode to portfolioReturn
        }
    }
    
    /**
     * 증감률 기준으로 포트폴리오 정렬
     */
    fun sortByReturnRate(stockReturns: List<PortfolioReturn>, ascending: Boolean = false): List<PortfolioReturn> {
        return if (ascending) {
            stockReturns.sortedBy { it.returnRate }
        } else {
            stockReturns.sortedByDescending { it.returnRate }
        }
    }
    
    /**
     * 수익 금액 기준으로 포트폴리오 정렬
     */
    fun sortByProfit(stockReturns: List<PortfolioReturn>, ascending: Boolean = false): List<PortfolioReturn> {
        return if (ascending) {
            stockReturns.sortedBy { it.profit }
        } else {
            stockReturns.sortedByDescending { it.profit }
        }
    }
    
    /**
     * 종목별 비중 계산
     */
    fun calculateWeights(summary: TotalPortfolioSummary): Map<String, Double> {
        return if (summary.totalCurrentValue > 0) {
            summary.stockReturns.associate { stock ->
                stock.stockCode to (stock.totalValue / summary.totalCurrentValue * 100)
            }
        } else {
            emptyMap()
        }
    }
    
    /**
     * 위험도가 높은 종목 찾기 (변동성이 큰 종목)
     */
    fun findHighRiskStocks(
        stockReturns: List<PortfolioReturn>,
        riskThreshold: Double = 10.0
    ): List<PortfolioReturn> {
        return stockReturns.filter { kotlin.math.abs(it.returnRate) > riskThreshold }
    }
    
    /**
     * 실시간 포트폴리오 알림이 필요한 종목들
     */
    fun getAlertStocks(
        stockReturns: List<PortfolioReturn>,
        profitAlertThreshold: Double = 5.0,
        lossAlertThreshold: Double = -3.0
    ): List<Pair<PortfolioReturn, String>> {
        return stockReturns.mapNotNull { stock ->
            when {
                stock.returnRate >= profitAlertThreshold -> 
                    stock to "수익률 ${String.format("%.2f", stock.returnRate)}% 달성!"
                stock.returnRate <= lossAlertThreshold -> 
                    stock to "손실률 ${String.format("%.2f", kotlin.math.abs(stock.returnRate))}% 발생"
                else -> null
            }
        }
    }
    
    private fun parseQuantity(quantityString: String): Int {
        return quantityString.replace("주", "").replace(",", "").trim().toIntOrNull() ?: 0
    }
    
    /**
     * 포트폴리오 성과 통계 계산
     */
    fun calculatePerformanceStats(summary: TotalPortfolioSummary): Map<String, Any> {
        val profits = summary.stockReturns.map { it.profit }
        val returns = summary.stockReturns.map { it.returnRate }
        
        return mapOf(
            "winning_stocks" to summary.stockReturns.count { it.profit > 0 },
            "losing_stocks" to summary.stockReturns.count { it.profit < 0 },
            "max_profit" to (profits.maxOrNull() ?: 0.0),
            "max_loss" to (profits.minOrNull() ?: 0.0),
            "avg_return" to (returns.average().takeIf { !it.isNaN() } ?: 0.0),
            "return_volatility" to calculateVolatility(returns)
        )
    }
    
    private fun calculateVolatility(returns: List<Double>): Double {
        if (returns.isEmpty()) return 0.0
        val mean = returns.average()
        val variance = returns.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * 파이차트용 색상 할당 (MyPageScreen과 동일한 순서)
     */
    private fun getStockColor(index: Int): Color {
        val colors = listOf(
            Color(0xFF42A6FF), // MainBlue
            Color(0xFFFF99C5), // MainPink
            Color(0xFFFFE28A), // AppColors.Yellow
            Color(0xFFC8FACC), // AppColors.Green
            Color(0xFFC5B5F9), // AppColors.Purple
        )
        return colors.getOrElse(index % colors.size) { Color(0xFFC4C4C4) } // AppColors.Gray
    }
}