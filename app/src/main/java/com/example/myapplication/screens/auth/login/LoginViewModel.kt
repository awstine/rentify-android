package com.example.myapplication.screens.auth.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. UI State: Represents all the state needed for the Login Screen.
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isPasswordVisible: Boolean = false,

    // State for validation errors
    val usernameError: String? = null,
    val passwordError: String? = null,

    // State to manage the login process
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false
)

// 2. UI Events: Defines all the actions a user can perform.
sealed interface LoginUiEvent {
    data class UsernameChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data class RememberMeChanged(val value: Boolean) : LoginUiEvent
    object TogglePasswordVisibility : LoginUiEvent
    object Submit : LoginUiEvent
}

// 3. The ViewModel
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // A mutable state that holds the current UI state.
    var uiState by mutableStateOf(LoginUiState())
        private set

    // Event Handler: Processes UI events to update the state.
    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.UsernameChanged -> {
                uiState = uiState.copy(username = event.value, usernameError = null)
            }

            is LoginUiEvent.PasswordChanged -> {
                uiState = uiState.copy(password = event.value, passwordError = null)
            }

            is LoginUiEvent.RememberMeChanged -> {
                uiState = uiState.copy(rememberMe = event.value)
            }

            is LoginUiEvent.TogglePasswordVisibility -> {
                uiState = uiState.copy(isPasswordVisible = !uiState.isPasswordVisible)
            }

            is LoginUiEvent.Submit -> {
                viewModelScope.launch {
                    handleLogin()
                }
            }
        }
    }

    // Business Logic: Contains the actual logic for logging in.
    private suspend fun handleLogin() {
        // Reset previous errors and start loading
        uiState = uiState.copy(isLoading = true, usernameError = null, passwordError = null)

        // --- Basic Validation ---
        if (uiState.username.isBlank()) {
            uiState = uiState.copy(isLoading = false, usernameError = "Username cannot be empty")
            return
        }
        if (uiState.password.isBlank()) {
            uiState = uiState.copy(isLoading = false, passwordError = "Password cannot be empty")
            return
        }

        val result = authRepository.signIn(
            email = uiState.username,
            password = uiState.password
        )

        result.fold(
            onSuccess = {
                uiState = uiState.copy(isLoading = false, loginSuccess = true)
            },
            onFailure = { 
                uiState = uiState.copy(isLoading = false, passwordError = "Invalid login credentials")
            }
        )
    }

    suspend fun hasValidSession(): Boolean {
        return authRepository.hasValidSession()
    }
}
