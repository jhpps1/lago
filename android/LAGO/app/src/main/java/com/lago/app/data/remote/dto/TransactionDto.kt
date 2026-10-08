package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.lago.app.domain.entity.Transaction
import java.text.SimpleDateFormat
import java.util.*

data class TransactionDto(
    @SerializedName("tradeId")
    val tradeId: Long,
    @SerializedName("accountId")
    val accountId: Long,
    @SerializedName("stockName")
    val stockName: String,
    @SerializedName("stockId")
    val stockId: String,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("buySell")
    val buySell: String,
    @SerializedName("price")
    val price: Long,
    @SerializedName("tradeAt")
    val tradeAt: String,
    @SerializedName("isQuiz")
    val isQuiz: Boolean
)

fun TransactionDto.toDomain(): Transaction {
    return try {
        android.util.Log.v("DTO_MAPPING", "Converting TransactionDto to Domain: $this")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val parsedDate = dateFormat.parse(tradeAt)
        
        android.util.Log.v("DTO_MAPPING", "Original date string: '$tradeAt'")
        android.util.Log.v("DTO_MAPPING", "Parsed date: $parsedDate")
        
        val transaction = Transaction(
            tradeId = tradeId,
            accountId = accountId,
            stockName = stockName,
            stockId = stockId,
            quantity = quantity,
            buySell = buySell,
            price = price,
            tradeAt = parsedDate ?: Date(),
            isQuiz = isQuiz
        )
        
        android.util.Log.v("DTO_MAPPING", "Successfully converted to Transaction: $transaction")
        transaction
        
    } catch (e: Exception) {
        android.util.Log.e("DTO_MAPPING", "Error converting TransactionDto to Domain", e)
        android.util.Log.e("DTO_MAPPING", "Failed DTO: $this")
        
        // Return transaction with current date as fallback
        Transaction(
            tradeId = tradeId,
            accountId = accountId,
            stockName = stockName,
            stockId = stockId,
            quantity = quantity,
            buySell = buySell,
            price = price,
            tradeAt = Date(),
            isQuiz = isQuiz
        )
    }
}