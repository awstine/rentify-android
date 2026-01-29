package com.example.myapplication.screens.auth.register

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.User
import com.example.myapplication.data.repository.AuthRepository
import kotlinx.coroutines.launch

// 1. UI State: Represents all the state needed for the Register Screen.
data class RegisterUiState(
    val username: String = "",
    val mobile: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val isPasswordVisible: Boolean = false,

    // State for validation errors
    val usernameError: String? = null,
    val mobileError: String? = null,
    val passwordError: String? = null,

    // State to manage the registration process
    val isLoading: Boolean = false,
    val registrationSuccess: Boolean = false
)

// 2. UI Events: Defines all the actions a user can perform on the screen.
sealed interface RegisterUiEvent {
    data class UsernameChanged(val value: String) : RegisterUiEvent
    data class MobileChanged(val value: String) : RegisterUiEvent
    data class PasswordChanged(val value: String) : RegisterUiEvent
    data class RememberMeChanged(val value: Boolean) : RegisterUiEvent
    object TogglePasswordVisibility : RegisterUiEvent
    object Submit : RegisterUiEvent
}


class RegisterViewModel : ViewModel() {

    // Create an instance of the repository
    private val authRepository = AuthRepository()

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.UsernameChanged -> {
                uiState = uiState.copy(username = event.value, usernameError = null)
            }
            is RegisterUiEvent.MobileChanged -> {
                // Allow empty or digits only
                if (event.value.isEmpty() || event.value.all { it.isDigit() }) {
                    uiState = uiState.copy(mobile = event.value, mobileError = null)
                }
            }
            is RegisterUiEvent.PasswordChanged -> {
                uiState = uiState.copy(password = event.value, passwordError = null)
            }
            is RegisterUiEvent.RememberMeChanged -> {
                uiState = uiState.copy(rememberMe = event.value)
            }
            is RegisterUiEvent.TogglePasswordVisibility -> {
                uiState = uiState.copy(isPasswordVisible = !uiState.isPasswordVisible)
            }
            is RegisterUiEvent.Submit -> {
                viewModelScope.launch {
                    handleRegistration()
                }
            }
        }
    }

    private suspend fun handleRegistration() {
        uiState = uiState.copy(
            isLoading = true,
            usernameError = null,
            mobileError = null,
            passwordError = null
        )

        // Add logging to debug
        Log.d("RegisterViewModel", "Email: ${uiState.username}")
        Log.d("RegisterViewModel", "Mobile: ${uiState.mobile}")
        Log.d("RegisterViewModel", "Mobile length: ${uiState.mobile.length}")
        Log.d("RegisterViewModel", "Mobile isEmpty: ${uiState.mobile.isEmpty()}")

        // --- Validation ---
        var hasError = false

        // Validate email
        if (uiState.username.trim().isEmpty()) {
            uiState = uiState.copy(usernameError = "Email is required")
            hasError = true
        } else if (uiState.username.length < 3) {
            uiState = uiState.copy(usernameError = "Email must be at least 3 characters")
            hasError = true
        }

        // Validate mobile number
        val trimmedMobile = uiState.mobile.trim()
        if (trimmedMobile.isEmpty()) {
            uiState = uiState.copy(mobileError = "Mobile number is required")
            hasError = true
        } else if (trimmedMobile.length < 10) {
            uiState = uiState.copy(mobileError = "Mobile number must be at least 10 digits")
            hasError = true
        }

        // Validate password
        if (uiState.password.isEmpty()) {
            uiState = uiState.copy(passwordError = "Password is required")
            hasError = true
        } else if (uiState.password.length < 6) {
            uiState = uiState.copy(passwordError = "Password must be at least 6 characters")
            hasError = true
        }

        if (hasError) {
            uiState = uiState.copy(isLoading = false)
            return
        }

        // --- Call the Supabase sign-up function ---
        val result = authRepository.signUp(
            email = uiState.username.trim(),
            password = uiState.password,
            userData = User(
                id = "",
                email = uiState.username.trim(),
                phone_number = trimmedMobile, // Use trimmed mobile
                id_number = null,
                role = "tenant",
                full_name = null,
                created_at = null
            )
        )

        result.fold(
            onSuccess = {
                Log.d("RegisterViewModel", "Registration successful!")
                uiState = uiState.copy(isLoading = false, registrationSuccess = true)
            },
            onFailure = { error ->
                Log.e("RegisterViewModel", "Registration failed", error)
                val errorMessage = error.message ?: "An unknown error occurred"

                if (errorMessage.contains("Unable to validate email address: invalid format", ignoreCase = true)) {
                    uiState = uiState.copy(
                        isLoading = false,
                        usernameError = "Invalid email format. Please enter a valid email."
                    )
                } else if (errorMessage.contains("phone_number", ignoreCase = true)) {
                    uiState = uiState.copy(
                        isLoading = false,
                        mobileError = "Mobile number is required"
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        passwordError = errorMessage
                    )
                }
            }
        )
    }
}