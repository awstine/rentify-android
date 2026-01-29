package com.example.myapplication.screens.payment

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PaymentTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRequestSent: Boolean = false, // STK Push sent
    val isPaymentComplete: Boolean = false, // Money received & Room allocated
    val isPaymentFailed: Boolean = false, // Payment failed
    val paymentMessage: String? = null
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    var uiState by mutableStateOf(PaymentUiState())
        private set

    private var pollingJob: Job? = null
    private var statusObserverJob: Job? = null
    private val TAG = "PaymentVM"

    // To prevent processing old "failed" transactions on retry
    private var lastTransactionId: String? = null

    // Updated function signature to accept 'numberOfMonths'
    fun initiatePayment(
        bookingId: String,
        amount: Double,
        phoneNumber: String,
        roomNumber: String,
        numberOfMonths: Int 
    ) {
        // Cancel any existing listeners
        stopPolling()
        statusObserverJob?.cancel()

        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                error = null,
                isPaymentFailed = false,
                isPaymentComplete = false,
                isRequestSent = false
            )

            // 0. Snapshot current latest transaction ID to ignore it later (Fix for retry bug)
            val currentLatest = paymentRepository.getLatestPaymentTransaction(bookingId)
            lastTransactionId = currentLatest?.id
            Log.d(TAG, "Ignoring transaction ID: $lastTransactionId")

            Log.d(TAG, "Initiating Payment for $bookingId for $numberOfMonths months")

            // 1. TRIGGER STK PUSH (With the total amount)
            val result = paymentRepository.initiateMpesaPayment(bookingId, amount, phoneNumber)

            if (result.isSuccess) {
                uiState = uiState.copy(
                    isRequestSent = true,
                    paymentMessage = "Please enter your M-Pesa PIN."
                )

                // Start observing. We pass numberOfMonths so we can update the DB *after* success.
                observePayment(bookingId, numberOfMonths)

            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    isRequestSent = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to send request"
                )
            }
        }
    }

    private fun observePayment(bookingId: String, numberOfMonths: Int) {
        statusObserverJob = viewModelScope.launch {
            paymentRepository.observePaymentTransaction(bookingId).collect { transaction ->
                if (handlePaymentStatus(transaction, bookingId, numberOfMonths)) {
                    // Stop collecting if terminal state reached
                    this.coroutineContext.cancel() // Cancel this job
                }
            }
        }
        startPolling(bookingId, numberOfMonths)
    }

    private fun startPolling(bookingId: String, numberOfMonths: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val timeout = System.currentTimeMillis() + 120000 // 2 minutes

            while (isActive && !uiState.isPaymentComplete && !uiState.isPaymentFailed && System.currentTimeMillis() < timeout) {
                delay(3000)
                Log.d(TAG, "Polling check...")
                val transaction = paymentRepository.getLatestPaymentTransaction(bookingId)
                if (handlePaymentStatus(transaction, bookingId, numberOfMonths)) {
                    break
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun handlePaymentStatus(transaction: PaymentTransaction?, bookingId: String, numberOfMonths: Int): Boolean {
        if (transaction == null) return false
        
        // --- KEY FIX FOR RETRY ---
        // If the transaction ID is the same as the one we saw BEFORE starting this request,
        // it means we are seeing stale data (e.g. the previous failure). Ignore it.
        if (lastTransactionId != null && transaction.id == lastTransactionId) {
            Log.d(TAG, "Skipping stale transaction: ${transaction.id}")
            return false
        }
        
        val status = transaction.status ?: return false
        val normalizedStatus = status.lowercase().trim()

        return when (normalizedStatus) {
            "completed", "success", "paid" -> {
                Log.d(TAG, "Payment COMPLETED! Finalizing booking...")
                
                // CRITICAL FIX: Only NOW do we update the booking in the database
                val updateResult = paymentRepository.updateBookingDuration(bookingId, numberOfMonths)
                
                if (updateResult.isSuccess) {
                     uiState = uiState.copy(
                        isPaymentComplete = true,
                        isLoading = false,
                        isRequestSent = false,
                        isPaymentFailed = false,
                        paymentMessage = "Payment Received! Room allocated."
                    )
                } else {
                     uiState = uiState.copy(
                        isPaymentComplete = true,
                        isLoading = false,
                        isRequestSent = false,
                        isPaymentFailed = false,
                        paymentMessage = "Payment Received! (Room update pending)"
                    )
                }
                
                stopPolling()
                statusObserverJob?.cancel()
                true
            }
            "failed", "cancelled" -> {
                Log.d(TAG, "Payment FAILED.")
                uiState = uiState.copy(
                    isPaymentFailed = true,
                    isLoading = false,
                    isRequestSent = false,
                    isPaymentComplete = false,
                    paymentMessage = "Payment Failed or Cancelled."
                )
                stopPolling()
                statusObserverJob?.cancel()
                true
            }
            else -> false // Still pending
        }
    }

    fun resetState() {
        uiState = PaymentUiState()
        stopPolling()
        statusObserverJob?.cancel()
    }
}
