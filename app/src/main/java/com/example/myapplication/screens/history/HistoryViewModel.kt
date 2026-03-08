package com.example.myapplication.screens.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PaymentTransaction
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.datasource.preferences.RentifyPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
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
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferences: RentifyPreferences
) : ViewModel() {

    var uiState by mutableStateOf(HistoryUiState())
        private set

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // Robust Fallback: Try fresh profile -> Metadata -> Cached ID
            var profile = userRepository.getUserProfile().getOrNull()
            if (profile == null) {
                profile = userRepository.getUserFromMetadata()
            }
            
            val cachedId = preferences.getUserId().firstOrNull()
            val userId = profile?.id ?: cachedId

            if (userId == null) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "User not found"
                )
                return@launch
            }

            val result = paymentRepository.getTenantPaymentHistory(userId)
            if (result.isSuccess) {
                uiState = uiState.copy(
                    isLoading = false,
                    transactions = result.getOrNull() ?: emptyList(),
                    error = null
                )
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "No internet connection"
                )
            }
        }
    }
}
