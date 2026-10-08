package com.lago.app.domain.entity

import java.util.Date

data class Transaction(
    val tradeId: Long,
    val accountId: Long,
    val stockName: String,
    val stockId: String,
    val quantity: Int,
    val buySell: String,
    val price: Long,
    val tradeAt: Date,
    val isQuiz: Boolean
)