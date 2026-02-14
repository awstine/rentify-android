package com.example.myapplication.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.theme.NavyPrimary
import java.text.NumberFormat
import java.util.Locale

enum class PaymentSheetContent {
    PROCESSING,
    SUCCESS,
    FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    bookingId: String,
    amount: Double,
    roomNumber: String,
    onPaymentSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    var phoneNumber by remember { mutableStateOf("") }
    val backgroundColor = Color(0xFFF7F7F9)

    // Bottom Sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var sheetContent by remember { mutableStateOf<PaymentSheetContent?>(null) }

    LaunchedEffect(Unit) {
        viewModel.setPaymentDetails(baseAmount = amount, months = 1)
    }

    LaunchedEffect(state) {
        when {
            state.isPaymentComplete -> {
                sheetContent = PaymentSheetContent.SUCCESS
                showBottomSheet = true
            }
            state.isPaymentFailed -> {
                sheetContent = PaymentSheetContent.FAILED
                showBottomSheet = true
            }
            state.isLoading || state.isRequestSent -> {
                sheetContent = PaymentSheetContent.PROCESSING
                showBottomSheet = true
            }
            else -> {
                showBottomSheet = false
            }
        }
    }

    val totalAmountToPay = state.totalAmount
    val formattedTotal = NumberFormat.getCurrencyInstance(Locale("en", "KE")).format(totalAmountToPay)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Payment", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NavyPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NavyPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        viewModel.initiatePayment(
                            bookingId = bookingId,
                            phoneNumber = phoneNumber,
                            roomNumber = roomNumber
                        )
                    }
                },
                enabled = !state.isLoading && phoneNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay $formattedTotal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // 1. Booking Details Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Room Number", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(roomNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E232C))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    Text("Duration (Months)", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Duration Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F6F8), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.numberOfMonths > 1) {
                                    viewModel.setPaymentDetails(amount, state.numberOfMonths - 1)
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NavyPrimary)
                        }

                        Text(
                            text = "${state.numberOfMonths}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )

                        IconButton(
                            onClick = { viewModel.setPaymentDetails(amount, state.numberOfMonths + 1) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = NavyPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Payment Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("Total Amount to Pay", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedTotal,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. M-Pesa Details
            Text(
                text = "Mobile Money Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E232C),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("M-Pesa Phone Number") },
                placeholder = { Text("e.g. 254712345678") },
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyPrimary,
                    focusedLabelColor = NavyPrimary,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = NavyPrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    errorContainerColor = Color.White
                )
            )

            if (state.error != null && !showBottomSheet) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(state.error, color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // --- MODAL BOTTOM SHEET ---
        if (showBottomSheet && sheetContent != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    viewModel.resetState()
                    showBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                PaymentBottomSheetContent(
                    content = sheetContent!!,
                    state = state,
                    roomNumber = roomNumber,
                    formattedAmount = formattedTotal,
                    onSuccessAction = onPaymentSuccess,
                    onRetryAction = { viewModel.resetState() }
                )
            }
        }
    }
}

@Composable
private fun PaymentBottomSheetContent(
    content: PaymentSheetContent,
    state: PaymentUiState,
    roomNumber: String,
    formattedAmount: String,
    onSuccessAction: () -> Unit,
    onRetryAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .padding(bottom = 32.dp), // Extra padding for system nav bar
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (content) {
            PaymentSheetContent.PROCESSING -> {
                val isSent = state.isRequestSent
                val iconColor = NavyPrimary
                val bgColor = iconColor.copy(alpha = 0.1f)

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(bgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSent) {
                        Icon(
                            Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        CircularProgressIndicator(color = iconColor, strokeWidth = 3.dp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (isSent) "Check your phone" else "Processing...",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E232C)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isSent) "We sent a request for $formattedAmount.Please enter your M-Pesa PIN."
                    else "Initiating secure payment via M-Pesa...",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                if(isSent) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = NavyPrimary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            }

            PaymentSheetContent.SUCCESS -> {
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0xFFE8F5E9), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Payment Successful!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E232C))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Room $roomNumber successfully allocated. You paid for ${state.numberOfMonths} month(s).", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = onSuccessAction,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Go to My Rooms", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            PaymentSheetContent.FAILED -> {
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0xFFFFEBEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Error, contentDescription = "Failed", tint = Color.Red, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Payment Failed", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E232C))
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.paymentMessage ?: "Transaction declined or timed out.Please check your balance and try again.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 15.sp, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = onRetryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
