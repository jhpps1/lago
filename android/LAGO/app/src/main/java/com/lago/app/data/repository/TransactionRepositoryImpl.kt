package com.lago.app.data.repository

import com.lago.app.data.remote.ApiResponse
import com.lago.app.data.remote.RemoteDataSource
import com.lago.app.data.remote.dto.toDomain
import com.lago.app.domain.entity.Transaction
import com.lago.app.domain.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val remoteDataSource: RemoteDataSource
) : TransactionRepository {
    
    override suspend fun getTransactions(userId: Long): Result<List<Transaction>> {
        return try {
            android.util.Log.d("REPOSITORY", "TransactionRepository - Getting transactions for userId: $userId")
            
            when (val response = remoteDataSource.getTransactions(userId)) {
                is ApiResponse.Success -> {
                    android.util.Log.d("REPOSITORY", "TransactionRepository - API Success, mapping ${response.data.size} items")
                    val transactions = response.data.map { dto ->
                        android.util.Log.v("REPOSITORY", "Mapping DTO: $dto")
                        dto.toDomain()
                    }
                    android.util.Log.d("REPOSITORY", "TransactionRepository - Successfully mapped ${transactions.size} transactions")
                    Result.success(transactions)
                }
                is ApiResponse.Error -> {
                    android.util.Log.e("REPOSITORY", "TransactionRepository - API Error: ${response.message}")
                    Result.failure(Exception("API Error: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("REPOSITORY", "TransactionRepository - Exception occurred", e)
            Result.failure(Exception("Network Error: ${e.message}"))
        }
    }
    
    override suspend fun getHistoryTransactions(userId: Long): Result<List<Transaction>> {
        return try {
            android.util.Log.d("REPOSITORY", "TransactionRepository - Getting history transactions for userId: $userId")
            
            when (val response = remoteDataSource.getHistoryTransactions(userId)) {
                is ApiResponse.Success -> {
                    android.util.Log.d("REPOSITORY", "TransactionRepository - History API Success, mapping ${response.data.size} items")
                    val transactions = response.data.map { dto ->
                        android.util.Log.v("REPOSITORY", "Mapping History DTO: $dto")
                        dto.toDomain()
                    }
                    android.util.Log.d("REPOSITORY", "TransactionRepository - Successfully mapped ${transactions.size} history transactions")
                    Result.success(transactions)
                }
                is ApiResponse.Error -> {
                    android.util.Log.e("REPOSITORY", "TransactionRepository - History API Error: ${response.message}")
                    Result.failure(Exception("History API Error: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("REPOSITORY", "TransactionRepository - History Exception occurred", e)
            Result.failure(Exception("History Network Error: ${e.message}"))
        }
    }
    
    override suspend fun getAiTransactions(aiId: Long): Result<List<Transaction>> {
        return try {
            android.util.Log.d("REPOSITORY", "TransactionRepository - Getting AI transactions for aiId: $aiId")
            
            when (val response = remoteDataSource.getAiTransactions(aiId)) {
                is ApiResponse.Success -> {
                    android.util.Log.d("REPOSITORY", "TransactionRepository - AI API Success, mapping ${response.data.size} items")
                    val transactions = response.data.map { dto ->
                        android.util.Log.v("REPOSITORY", "Mapping AI DTO: $dto")
                        dto.toDomain()
                    }
                    android.util.Log.d("REPOSITORY", "TransactionRepository - Successfully mapped ${transactions.size} AI transactions")
                    Result.success(transactions)
                }
                is ApiResponse.Error -> {
                    android.util.Log.e("REPOSITORY", "TransactionRepository - AI API Error: ${response.message}")
                    Result.failure(Exception("AI API Error: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("REPOSITORY", "TransactionRepository - AI Exception occurred", e)
            Result.failure(Exception("AI Network Error: ${e.message}"))
        }
    }
    
    private fun createMockTransactions(userId: Long): List<Transaction> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        return listOf(
            Transaction(
                tradeId = 1,
                accountId = 1,
                stockName = "삼성전자",
                stockId = "005930",
                quantity = 5,
                buySell = "SELL",
                price = 72500,
                tradeAt = sdf.parse("2025-08-05T10:30:00") ?: java.util.Date(),
                isQuiz = false
            ),
            Transaction(
                tradeId = 2,
                accountId = 1,
                stockName = "한화생명",
                stockId = "088350",
                quantity = 2,
                buySell = "BUY",
                price = 275000,
                tradeAt = sdf.parse("2025-08-05T11:15:00") ?: java.util.Date(),
                isQuiz = false
            ),
            Transaction(
                tradeId = 3,
                accountId = 1,
                stockName = "삼성전자",
                stockId = "005930",
                quantity = 3,
                buySell = "SELL",
                price = 71800,
                tradeAt = sdf.parse("2025-08-05T14:20:00") ?: java.util.Date(),
                isQuiz = false
            ),
            Transaction(
                tradeId = 4,
                accountId = 1,
                stockName = "한화생명",
                stockId = "088350",
                quantity = 1,
                buySell = "BUY",
                price = 273000,
                tradeAt = sdf.parse("2025-08-06T09:30:00") ?: java.util.Date(),
                isQuiz = false
            ),
            Transaction(
                tradeId = 5,
                accountId = 1,
                stockName = "LG전자",
                stockId = "066570",
                quantity = 10,
                buySell = "BUY",
                price = 82000,
                tradeAt = sdf.parse("2025-07-28T13:45:00") ?: java.util.Date(),
                isQuiz = false
            )
        )
    }
}