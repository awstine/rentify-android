package com.example.myapplication.screens.payment

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isRequestSent: Boolean = false, 
    val isPaymentComplete: Boolean = false, 
    val isPaymentFailed: Boolean = false, 
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

    private var lastTransactionId: String? = null

    fun initiatePayment(
        bookingId: String,
        amount: Double,
        phoneNumber: String,
        roomNumber: String,
        numberOfMonths: Int
    ) {
        stopPolling()
        statusObserverJob?.cancel()

        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                isRequestSent = true,
                error = null,
                isPaymentFailed = false,
                isPaymentComplete = false
            )

            val currentLatest = paymentRepository.getLatestPaymentTransaction(bookingId)
            lastTransactionId = currentLatest?.id
            Log.d(TAG, "Ignoring transaction ID: $lastTransactionId")

            Log.d(TAG, "Initiating Payment for $bookingId for $numberOfMonths months")

            val result = paymentRepository.initiateMpesaPayment(bookingId, amount, phoneNumber)

            if (result.isSuccess) {
                uiState = uiState.copy(
                    isLoading = false,
                    paymentMessage = "Please enter your M-Pesa PIN."
                )
                observePayment(bookingId, numberOfMonths)
            } else {
                Log.e(TAG, "STK Push initiation failed", result.exceptionOrNull())
                uiState = uiState.copy(
                    isLoading = false,
                    isRequestSent = false,
                    isPaymentFailed = true,
                    paymentMessage = "Payment request failed. Please check your connection and try again."
                )
            }
        }
    }

    private fun observePayment(bookingId: String, numberOfMonths: Int) {
        statusObserverJob = viewModelScope.launch {
            paymentRepository.observePaymentTransaction(bookingId).collect { transaction ->
                if (handlePaymentStatus(transaction, bookingId, numberOfMonths)) {
                    this.coroutineContext.cancel()
                }
            }
        }
        startPolling(bookingId, numberOfMonths)
    }

    private fun startPolling(bookingId: String, numberOfMonths: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val timeout = System.currentTimeMillis() + 120000

            while (isActive && !uiState.isPaymentComplete && !uiState.isPaymentFailed && System.currentTimeMillis() < timeout) {
                delay(3000)
                Log.d(TAG, "Polling check...")
                val transaction = paymentRepository.getLatestPaymentTransaction(bookingId)
                if (handlePaymentStatus(transaction, bookingId, numberOfMonths)) {
                    break
                }
            }

            if (isActive && !uiState.isPaymentComplete) {
                Log.w(TAG, "Polling timed out for booking ID: $bookingId")
                uiState = uiState.copy(
                    isPaymentFailed = true,
                    isLoading = false,
                    isRequestSent = false,
                    paymentMessage = "We could not confirm the payment. Please check your M-Pesa or try again."
                )
                stopPolling()
                statusObserverJob?.cancel()
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun handlePaymentStatus(transaction: PaymentTransaction?, bookingId: String, numberOfMonths: Int): Boolean {
        if (transaction == null) return false

        if (lastTransactionId != null && transaction.id == lastTransactionId) {
            Log.d(TAG, "Skipping stale transaction: ${transaction.id}")
            return false
        }

        val status = transaction.status ?: return false
        val normalizedStatus = status.lowercase().trim()

        return when (normalizedStatus) {
            "completed", "success", "paid" -> {
                Log.d(TAG, "Payment COMPLETED! Finalizing booking...")

                val updateResult = paymentRepository.updateBookingDuration(bookingId, numberOfMonths)

                uiState = uiState.copy(
                    isPaymentComplete = true,
                    isLoading = false,
                    isRequestSent = false,
                    isPaymentFailed = false,
                    paymentMessage = if (updateResult.isSuccess) "Payment Received! Room allocated." else "Payment Received! (Room update pending)"
                )

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
                    paymentMessage = "Payment Failed or Cancelled by user."
                )
                stopPolling()
                statusObserverJob?.cancel()
                true
            }
            else -> false
        }
    }

    fun resetState() {
        uiState = PaymentUiState()
        stopPolling()
        statusObserverJob?.cancel()
    }
}
