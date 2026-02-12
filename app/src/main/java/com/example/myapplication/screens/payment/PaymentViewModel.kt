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
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

data class PaymentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRequestSent: Boolean = false,
    val isPaymentComplete: Boolean = false,
    val isPaymentFailed: Boolean = false,
    val paymentMessage: String? = null,
    val numberOfMonths: Int = 1,
    val baseAmount: Double = 0.0,
    val totalAmount: Double = 0.0
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

    fun setPaymentDetails(baseAmount: Double, months: Int) {
        uiState = uiState.copy(
            baseAmount = baseAmount,
            numberOfMonths = months,
            totalAmount = baseAmount * months
        )
    }

    fun resetState() {
        uiState = uiState.copy(
            isLoading = false,
            isRequestSent = false,
            isPaymentComplete = false,
            isPaymentFailed = false,
            error = null,
            paymentMessage = null
        )
        stopPolling()
        statusObserverJob?.cancel()
    }

    fun initiatePayment(
        bookingId: String,
        phoneNumber: String,
        roomNumber: String
    ) {
        stopPolling()
        statusObserverJob?.cancel()

        val currentAmount = uiState.totalAmount
        val currentMonths = uiState.numberOfMonths

        viewModelScope.launch {
            uiState = uiState.copy(
                isLoading = true,
                error = null,
                isPaymentFailed = false,
                isPaymentComplete = false,
                isRequestSent = false
            )

            val currentLatest = paymentRepository.getLatestPaymentTransaction(bookingId)
            lastTransactionId = currentLatest?.id

            // 1. Initiate the Request
            val result = paymentRepository.initiateMpesaPayment(bookingId, currentAmount, phoneNumber)

            if (result.isSuccess) {
                uiState = uiState.copy(
                    isRequestSent = true,
                    paymentMessage = "Please enter your M-Pesa PIN."
                )
                observePayment(bookingId, currentMonths)
            } else {
                // 2. ERROR HANDLING: Convert technical error to user-friendly text
                val rawError = result.exceptionOrNull()
                val friendlyMessage = getUserFriendlyError(rawError)

                uiState = uiState.copy(
                    isLoading = false,
                    isRequestSent = false,
                    isPaymentFailed = true,
                    paymentMessage = friendlyMessage // <--- Use the clean message
                )
            }
        }
    }

    // --- NEW HELPER FUNCTION ---
    private fun getUserFriendlyError(error: Throwable?): String {
        if (error == null) return "An unexpected error occurred."

        val message = error.message?.lowercase() ?: ""

        return when {
            // Internet / Connection Issues
            error is IOException || message.contains("unable to resolve host") || message.contains("no address associated") -> {
                "No internet connection. Please check your data or Wi-Fi."
            }
            // Timeout (Server took too long)
            error is SocketTimeoutException || message.contains("timeout") || message.contains("timed out") -> {
                "Connection timed out. The server is taking too long to respond."
            }
            // Server Errors (500, 503, etc)
            message.contains("500") || message.contains("internal server error") -> {
                "Our servers are having trouble right now. Please try again later."
            }
            // Not Found / Bad Request (404, 400)
            message.contains("404") || message.contains("400") -> {
                "Service unavailable. Please contact support."
            }
            // Invalid Phone Number (Common validation error)
            message.contains("invalid phone") || message.contains("phone number") -> {
                "Please enter a valid M-Pesa phone number."
            }
            // Fallback for everything else
            else -> "Something went wrong. Please try again."
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
                val transaction = paymentRepository.getLatestPaymentTransaction(bookingId)
                if (handlePaymentStatus(transaction, bookingId, numberOfMonths)) {
                    break
                }
            }
            // If loop finishes without success
            if (!uiState.isPaymentComplete && !uiState.isPaymentFailed) {
                uiState = uiState.copy(
                    isPaymentFailed = true,
                    isLoading = false,
                    isRequestSent = false,
                    paymentMessage = "Payment timed out. If you paid, please wait a moment for confirmation."
                )
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    private suspend fun handlePaymentStatus(transaction: PaymentTransaction?, bookingId: String, numberOfMonths: Int): Boolean {
        if (transaction == null) return false
        if (lastTransactionId != null && transaction.id == lastTransactionId) return false

        val status = transaction.status?.lowercase()?.trim() ?: return false

        return when (status) {
            "completed", "success", "paid" -> {
                paymentRepository.updateBookingDuration(bookingId, numberOfMonths)
                uiState = uiState.copy(
                    isPaymentComplete = true,
                    isLoading = false,
                    isRequestSent = false,
                    isPaymentFailed = false,
                    paymentMessage = "Payment Successful!"
                )
                stopPolling()
                true
            }
            "failed", "cancelled" -> {
                uiState = uiState.copy(
                    isPaymentFailed = true,
                    isLoading = false,
                    isRequestSent = false,
                    isPaymentComplete = false,
                    // Give a slightly more specific message for user cancellations
                    paymentMessage = if (status == "cancelled") "You cancelled the payment." else "Payment failed. Please check your balance and try again."
                )
                stopPolling()
                true
            }
            else -> false
        }
    }
}