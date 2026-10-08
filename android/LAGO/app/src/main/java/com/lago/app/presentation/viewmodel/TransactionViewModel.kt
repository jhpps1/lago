package com.lago.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lago.app.domain.entity.Transaction
import com.lago.app.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _transactionState = MutableStateFlow<TransactionUiState>(TransactionUiState.Loading)
    val transactionState: StateFlow<TransactionUiState> = _transactionState.asStateFlow()

    fun loadTransactions(userId: Long) {
        viewModelScope.launch {
            _transactionState.value = TransactionUiState.Loading
            getTransactionsUseCase(userId).fold(
                onSuccess = { transactions ->
                    _transactionState.value = TransactionUiState.Success(transactions)
                },
                onFailure = { exception ->
                    _transactionState.value = TransactionUiState.Error(exception.message ?: "Unknown error")
                }
            )
        }
    }
}

sealed class TransactionUiState {
    object Loading : TransactionUiState()
    data class Success(val transactions: List<Transaction>) : TransactionUiState()
    data class Error(val message: String) : TransactionUiState()
}