package com.example.myapplication.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.User
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
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
            val user = authRepository.getCurrentUser()
            if (user != null) {
                authRepository.getUserProfile(user.id).collect { result ->
                    if (result.isSuccess) {
                        uiState = uiState.copy(
                            isLoading = false,
                            user = result.getOrNull()
                        )
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message
                        )
                    }
                }
            } else {
                uiState = uiState.copy(isLoading = false, error = "User not logged in")
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
