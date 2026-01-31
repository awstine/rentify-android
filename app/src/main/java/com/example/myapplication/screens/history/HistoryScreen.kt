package com.example.myapplication.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.repository.PaymentTransaction
import com.example.myapplication.ui.theme.NavyPrimary

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit // Added callback for back navigation
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Payments", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHistory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyPrimary)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.error}", color = Color.Red)
                }
            } else if (state.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No transactions found.", color = Color.Gray)
                        Text("Your payment history will appear here.", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.transactions) { transaction ->
                        HistoryItem(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(transaction: PaymentTransaction) {
    // Normalize status to lowercase to avoid case-sensitivity issues
    val status = transaction.status?.lowercase()?.trim() ?: ""
    val isSuccess = status == "completed" || status == "success" || status == "paid"
    val isFailed = status == "failed" || status == "cancelled" || status == "rejected"

    // Formatting the Date
    val dateString = try {
        if (transaction.created_at != null) {
            val instant = Instant.parse(transaction.created_at)
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } else {
            "Unknown Date"
        }
    } catch (e: Exception) {
        "Unknown Date"
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Status Icon & Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isSuccess) Color(0xFF4CAF50) else if (isFailed) Color.Red else Color.Gray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isSuccess) "Payment Success" else if (isFailed) "Payment Failed" else "Processing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = NavyPrimary
                    )
                    Text(
                        text = dateString,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (isSuccess && transaction.mpesa_receipt_number != null) {
                        Text(
                            text = "Ref: ${transaction.mpesa_receipt_number}",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Right: Status Pill
            Text(
                text = if (isSuccess) "PAID" else "---",
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) Color(0xFF4CAF50) else Color.LightGray
            )
        }
    }
}
