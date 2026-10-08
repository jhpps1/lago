package com.lago.app.domain.usecase

import com.lago.app.domain.entity.Transaction
import com.lago.app.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(userId: Long): Result<List<Transaction>> {
        return transactionRepository.getTransactions(userId)
    }
}