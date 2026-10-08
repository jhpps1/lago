package com.lago.app.data.local.dao

import androidx.room.*
import com.lago.app.data.local.entity.CachedChartData
import com.lago.app.data.local.entity.CachedStockInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ChartCacheDao {
    
    // Chart Data Cache
    @Query("SELECT * FROM cached_chart_data WHERE id = :id AND expiryTime > :currentTime")
    suspend fun getCachedChartData(id: String, currentTime: Long = System.currentTimeMillis()): CachedChartData?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChartData(cachedChartData: CachedChartData)
    
    @Query("DELETE FROM cached_chart_data WHERE expiryTime <= :currentTime")
    suspend fun deleteExpiredChartData(currentTime: Long = System.currentTimeMillis())
    
    // Stock Info Cache
    @Query("SELECT * FROM cached_stock_info WHERE stockCode = :stockCode AND expiryTime > :currentTime")
    suspend fun getCachedStockInfo(stockCode: String, currentTime: Long = System.currentTimeMillis()): CachedStockInfo?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockInfo(cachedStockInfo: CachedStockInfo)
    
    @Query("DELETE FROM cached_stock_info WHERE expiryTime <= :currentTime")
    suspend fun deleteExpiredStockInfo(currentTime: Long = System.currentTimeMillis())
    
    // Cleanup old data
    @Query("DELETE FROM cached_chart_data WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldChartData(cutoffTime: Long)
    
    @Query("DELETE FROM cached_stock_info WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldStockInfo(cutoffTime: Long)
    
    // Flow for real-time updates
    @Query("SELECT * FROM cached_stock_info WHERE stockCode = :stockCode")
    fun observeCachedStockInfo(stockCode: String): Flow<CachedStockInfo?>
}