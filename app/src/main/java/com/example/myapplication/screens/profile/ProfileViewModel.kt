package com.example.myapplication.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.User
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    var uiState by mutableStateOf(ProfileUiState())
        private set
        
    init {
        loadUserProfile()
    }
    
    fun loadUserProfile() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            var user: User? = null
            var attempt = 0
            var lastError: String? = null
            
            // Retry logic: try 3 times with 1s delay
            while (user == null && attempt < 3) {
                val result = authRepository.getUserProfile()
                if (result.isSuccess) {
                    user = result.getOrNull()
                } else {
                    lastError = result.exceptionOrNull()?.message
                    if (attempt < 2) delay(1000)
                }
                attempt++
            }

            if (user != null) {
                uiState = uiState.copy(isLoading = false, user = user)
            } else {
                uiState = uiState.copy(isLoading = false, error = lastError ?: "User not found")
            }
        }
    }
    
    fun signOut(onSignOutSuccess: () -> Unit) {
         viewModelScope.launch {
            val result = authRepository.signOut()
             if (result.isSuccess) {
                 onSignOutSuccess()
             } else {
                 uiState = uiState.copy(error = "Failed to sign out")
             }
         }
    }
}
