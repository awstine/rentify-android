package com.example.myapplication.screens.auth.reset_password

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val resetSuccess: Boolean = false,
    val error: String? = null
)

sealed class ResetPasswordUiEvent {
    data class EmailChanged(val email: String) : ResetPasswordUiEvent()
    object Submit : ResetPasswordUiEvent()
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    var uiState by mutableStateOf(ResetPasswordUiState())
        private set

    fun onEvent(event: ResetPasswordUiEvent) {
        when (event) {
            is ResetPasswordUiEvent.EmailChanged -> {
                uiState = uiState.copy(email = event.email)
            }
            is ResetPasswordUiEvent.Submit -> {
                resetPassword()
            }
        }
    }

    private fun resetPassword() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            val result = authRepository.resetPassword(uiState.email)
            result.fold(
                onSuccess = {
                    uiState = uiState.copy(isLoading = false, resetSuccess = true)
                },
                onFailure = {
                    uiState = uiState.copy(isLoading = false, error = it.message)
                }
            )
        }
    }
}
