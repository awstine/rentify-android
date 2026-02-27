package com.example.myapplication.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var userRole by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            val hasSession = authRepository.hasValidSession()
            if (hasSession) {
                userRole = authRepository.getUserProfile().getOrNull()?.role
            }
            isLoading = false
        }
    }

    fun onLoginSuccess(role: String) {
        userRole = role
    }

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            userRole = null
        }
    }
}