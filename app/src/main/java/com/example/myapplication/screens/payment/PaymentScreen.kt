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
import androidx.compose.material.icons.filled.Remove
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
    amount: Double, // This is the Base Monthly Rent
    roomNumber: String,
    onPaymentSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    var phoneNumber by remember { mutableStateOf("") }

    // NEW: State for number of months
    var numberOfMonths by remember { mutableIntStateOf(1) }

    // NEW: Calculate Total dynamically
    val totalAmountToPay = amount * numberOfMonths

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
            // STATE 1: PAYMENT COMPLETED
            if (state.isPaymentComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Payment Successful!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Room $roomNumber allocated.\nPaid for $numberOfMonths month(s).",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onPaymentSuccess,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to My Rooms")
                }
            }
            // STATE 2: PAYMENT FAILED
            else if (state.isPaymentFailed) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Payment Failed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.paymentMessage ?: "Something went wrong. Please try again.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again")
                }
            }
            // STATE 3: WAITING FOR PIN
            else if (state.isRequestSent) {
                CircularProgressIndicator(color = NavyPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Check your phone", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We sent a request for KES ${totalAmountToPay.toInt()}.\nEnter PIN to complete.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
            // STATE 4: INPUT FORM
            else {
                Text("Room $roomNumber", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(24.dp))

                // --- NEW: Month Selector ---
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
                    // Decrease Button
                    IconButton(
                        onClick = { if (numberOfMonths > 1) numberOfMonths-- },
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
                            text = "$numberOfMonths",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = if (numberOfMonths == 1) "Month" else "Months",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Increase Button
                    IconButton(
                        onClick = { numberOfMonths++ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = NavyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Total Display ---
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
                    singleLine = true
                )

                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.error, color = Color.Red, fontSize = 14.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (phoneNumber.isNotBlank()) {
                            // Pass 'numberOfMonths' so the database gets updated
                            viewModel.initiatePayment(
                                bookingId = bookingId,
                                amount = amount, // <-- CORRECTED: Pass the base amount
                                phoneNumber = phoneNumber,
                                roomNumber = roomNumber,
                                numberOfMonths = numberOfMonths
                            )
                        }
                    },
                    enabled = !state.isLoading && phoneNumber.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Pay KES ${totalAmountToPay.toInt()}")
                    }
                }
            }
        }
    }
}
