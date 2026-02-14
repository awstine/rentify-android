package com.example.myapplication.screens.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.repository.PaymentTransaction
import com.example.myapplication.ui.theme.NavyPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state = viewModel.uiState
    val backgroundColor = Color(0xFFF5F5DC) // Matching ProfileScreen background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My Payments", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NavyPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHistory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NavyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyPrimary)
                }
            } else if (state.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.error}", color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            } else if (state.transactions.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CHANGED: Use itemsIndexed to pass the index to the item
                    itemsIndexed(state.transactions) { index, transaction ->
                        HistoryItem(transaction = transaction, index = index)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3E5E8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions yet",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF1E232C)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your payment history will appear here once you make a transaction.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryItem(transaction: PaymentTransaction, index: Int) {
    // ANIMATION LOGIC
    val alphaAnim = remember { Animatable(0f) }
    val translationYAnim = remember { Animatable(100f) }

    LaunchedEffect(Unit) {
        // Stagger the animation based on the item index
        delay(index * 75L)

        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            translationYAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    // Status Logic
    val status = transaction.status?.lowercase()?.trim() ?: ""
    val isSuccess = status == "completed" || status == "success" || status == "paid"
    val isFailed = status == "failed" || status == "cancelled" || status == "rejected"
    val isPending = !isSuccess && !isFailed

    // Styling based on status
    val icon = when {
        isSuccess -> Icons.Rounded.Check
        isFailed -> Icons.Rounded.Close
        else -> Icons.Rounded.Schedule
    }

    val statusColor = when {
        isSuccess -> Color(0xFF4CAF50) // Green
        isFailed -> Color(0xFFF44336)  // Red
        else -> Color(0xFFFF9800)      // Orange
    }

    val statusBgColor = statusColor.copy(alpha = 0.1f)

    // Robust Date Formatting
    val dateString = try {
        val rawDate = transaction.created_at
        if (!rawDate.isNullOrBlank()) {
            val instant = try {
                // 1. Try standard strict ISO-8601 (e.g., "2024-02-14T09:46:23Z")
                Instant.parse(rawDate)
            } catch (e: Exception) {
                try {
                    // 2. Try common SQL format by replacing space with 'T' (e.g., "2024-02-14 09:46:23")
                    var sqlFormat = rawDate.replace(" ", "T")
                    // If it has no timezone, assume UTC ('Z')
                    if (!sqlFormat.endsWith("Z") && !sqlFormat.contains('+') && sqlFormat.indexOf('-', 11) == -1) {
                        sqlFormat += "Z"
                    }
                    Instant.parse(sqlFormat)
                } catch (e2: Exception) {
                    // 3. Try parsing as a Unix Epoch timestamp (e.g., "1707903983000")
                    Instant.ofEpochMilli(rawDate.toLong())
                }
            }
            
            // Format it nicely for the UI
            val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • HH:mm").withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } else {
            "Unknown Date"
        }
    } catch (e: Exception) {
        // If all parsing fails, just show the raw database string instead of hiding it!
        transaction.created_at ?: "Unknown Date" 
    }

    // Amount Formatting
    val amountValue = transaction.amount?.toString()?.toDoubleOrNull() ?: 0.0
    val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "KE")).format(amountValue)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            // APPLIED ANIMATION TO MODIFIER
            .graphicsLayer {
                alpha = alphaAnim.value
                translationY = translationYAnim.value
            }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Status Icon inside a soft-colored circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle: Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSuccess) "Payment Success" else if (isFailed) "Payment Failed" else "Processing",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E232C)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                if (isSuccess && !transaction.mpesa_receipt_number.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ref: ${transaction.mpesa_receipt_number}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                    )
                }
            }

            // Right: Amount & Status Text
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isFailed) Color.Gray else Color(0xFF1E232C)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
        }
    }
}
