package com.example.myapplication.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PaymentTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val transactions: List<PaymentTransaction> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(HistoryUiState())
        private set

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            val user = authRepository.getCurrentUser()
            if (user != null) {
                val result = paymentRepository.getTenantPaymentHistory(user.id)
                if (result.isSuccess) {
                    uiState = uiState.copy(
                        isLoading = false,
                        transactions = result.getOrNull() ?: emptyList()
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                uiState = uiState.copy(isLoading = false, error = "User not logged in")
            }
        }
    }
}