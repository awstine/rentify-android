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

    // Initialize the view model with default 1 month
    LaunchedEffect(Unit) {
        viewModel.setPaymentDetails(baseAmount = amount, months = 1)
    }

    val totalAmountToPay = state.totalAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // --- STATE 1: SUCCESS ---
            if (state.isPaymentComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Payment Successful!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Room $roomNumber allocated.\nPaid for ${state.numberOfMonths} month(s).",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onPaymentSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Go to My Rooms", fontSize = 16.sp)
                }
            }

            // --- STATE 2: FAILED ---
            else if (state.isPaymentFailed) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = Color.Red,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Payment Failed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.paymentMessage ?: "Transaction declined or timed out.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Try Again", fontSize = 16.sp)
                }
            }

            // --- STATE 3: PROCESSING / WAITING (The change you wanted) ---
            // We enter this state if the request is sent OR if we are currently loading
            else if (state.isRequestSent || state.isLoading) {

                // Show different icon/text based on exactly what is happening
                if (state.isRequestSent) {
                    // STK Push sent, waiting for user PIN
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Check Phone",
                        tint = NavyPrimary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Check your phone", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "We sent a request for KES ${totalAmountToPay.toInt()}.\nPlease enter your M-Pesa PIN.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = NavyPrimary)

                } else {
                    // Just clicked button, communicating with server
                    CircularProgressIndicator(
                        color = NavyPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Processing...", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Initiating secure payment...",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }

            // --- STATE 4: INPUT FORM ---
            else {
                // Room & Month Selector Section
                Text("Room $roomNumber", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(32.dp))

                Text("Duration", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
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
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = NavyPrimary)
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.numberOfMonths}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = if (state.numberOfMonths == 1) "Month" else "Months",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(
                        onClick = { viewModel.setPaymentDetails(amount, state.numberOfMonths + 1) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = NavyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Total Amount to Pay", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "KES ${totalAmountToPay.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("M-Pesa Phone Number") },
                    placeholder = { Text("2547...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(state.error, color = Color.Red, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.weight(1f)) // Push button to bottom

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
                    // Disable button if loading or phone is empty
                    enabled = !state.isLoading && phoneNumber.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay KES ${totalAmountToPay.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}