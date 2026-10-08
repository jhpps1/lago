package com.lago.app.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lago.app.domain.entity.Transaction
import com.lago.app.presentation.theme.*
import com.lago.app.presentation.ui.components.CommonTopAppBar
import com.lago.app.presentation.viewmodel.OrderHistoryViewModel
import com.lago.app.presentation.viewmodel.OrderType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onBackClick: () -> Unit = {},
    userId: Int? = null, // null이면 ViewModel에서 UserPreferences에서 가져옴
    type: Int = 0, // 0: 모의투자, 1: 역사모드
    viewModel: OrderHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedOrderType by remember { mutableStateOf(OrderType.ALL) }
    var isDateDropdownExpanded by remember { mutableStateOf(false) }
    var isOrderTypeDropdownExpanded by remember { mutableStateOf(false) }
    
    val dateOptions = if (uiState.transactions.isNotEmpty()) {
        viewModel.getAvailableMonths()
    } else {
        emptyList()
    }
    
    var selectedDate by remember(dateOptions) {
        mutableStateOf(dateOptions.firstOrNull() ?: "2025년 8월")
    }

    val filteredHistory = if (uiState.transactions.isNotEmpty()) {
        viewModel.getFilteredTransactions(selectedOrderType, selectedDate)
    } else {
        emptyList()
    }
    
    LaunchedEffect(userId, type) {
        android.util.Log.d("ORDER_HISTORY", "OrderHistoryScreen LaunchedEffect - userId=$userId, type=$type")
        viewModel.loadTransactions(userId, type)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
    ) {
        CommonTopAppBar(
            title = "거래 내역",
            onBackClick = onBackClick
        )
        

        // Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Filter
            Box {
                Card(
                    modifier = Modifier
                        .clickable { isDateDropdownExpanded = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDate,
                            style = SubtitleSb14,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = isDateDropdownExpanded,
                    onDismissRequest = { isDateDropdownExpanded = false }
                ) {
                    dateOptions.forEach { date ->
                        DropdownMenuItem(
                            text = { Text(date) },
                            onClick = {
                                selectedDate = date
                                isDateDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Order Type Filter
            Box {
                Card(
                    modifier = Modifier
                        .clickable { isOrderTypeDropdownExpanded = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedOrderType.displayName,
                            style = SubtitleSb14,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = isOrderTypeDropdownExpanded,
                    onDismissRequest = { isOrderTypeDropdownExpanded = false }
                ) {
                    OrderType.entries.forEach { orderType ->
                        DropdownMenuItem(
                            text = { Text(orderType.displayName) },
                            onClick = {
                                selectedOrderType = orderType
                                isOrderTypeDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Order History List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error ?: "오류가 발생했습니다",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(filteredHistory) { transaction ->
                    TransactionItemRow(transaction)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun TransactionItemRow(transaction: Transaction) {
    val dateFormat = SimpleDateFormat("M.dd", Locale.getDefault())
    val displayDate = dateFormat.format(transaction.tradeAt)

    val orderTypeKorean = when (transaction.buySell) {
        "BUY" -> "구매"
        "SELL" -> "판매"
        else -> transaction.buySell
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date on the left
            Text(
                text = displayDate,
                style = TitleB16,
                color = Color.Black,
                modifier = Modifier.width(45.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Stock info
            Column {
                Text(
                    text = transaction.stockName,
                    style = SubtitleSb16,
                    color = Color.Black
                )
                
                Text(
                    text = "${transaction.quantity}주 $orderTypeKorean",
                    style = BodyR14,
                    color = when (transaction.buySell) {
                        "SELL" -> MainBlue
                        else -> Color.Gray
                    }
                )
            }
        }
        
        Text(
            text = "주당 ${String.format(Locale.getDefault(), "%,d", transaction.price)}원",
            style = SubtitleSb16,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OrderHistoryScreenPreview() {
    LagoTheme {
        OrderHistoryScreen(userId = 1)
    }
}