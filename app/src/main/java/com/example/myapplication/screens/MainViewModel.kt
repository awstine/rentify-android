package com.example.myapplication.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.UserRepository
import com.example.myapplication.datasource.preferences.RentifyPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: RentifyPreferences,
    private val userRepository: UserRepository
) : ViewModel() {

    var userRole by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch {
            try {
                val hasSession = authRepository.hasValidSession()
                Log.d("MainViewModel", "Has valid session: $hasSession")
                
                if (hasSession) {
                    // 1. Try to get role from network/metadata (getUserProfile handles both)
                    val networkResult = userRepository.getUserProfile()
                    val networkRole = networkResult.getOrNull()?.role
                    
                    if (networkRole != null) {
                        Log.d("MainViewModel", "Found role from network/metadata: $networkRole")
                        userRole = networkRole
                        // Role is already cached inside getUserProfile, but we can ensure it here too
                        preferences.saveUserRole(networkRole)
                    } else {
                        // 2. Fallback to local cache if network/metadata fails
                        preferences.getUserRole().collect {
                            userRole = it
                        }
                    }
                } else {
                    userRole = null
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error initializing session", e)
                 preferences.getUserRole().collect {
                     userRole = it
                }
            } finally {
                isLoading = false
                Log.d("MainViewModel", "Final user role: $userRole")
            }
        }
    }

    fun onLoginSuccess(role: String) {
        Log.d("MainViewModel", "Login success, setting role: $role")
        userRole = role
        viewModelScope.launch {
            preferences.saveUserRole(role)
        }
    }

    fun onSignOut() {
        Log.d("MainViewModel", "Signing out")
        viewModelScope.launch {
            authRepository.signOut()
            userRole = null
        }
    }
}
