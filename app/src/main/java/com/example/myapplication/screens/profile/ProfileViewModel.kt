package com.example.myapplication.screens.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(ProfileState())
        private set

    private val _profilePhotoUpdateSuccess = MutableSharedFlow<Unit>()
    val profilePhotoUpdateSuccess = _profilePhotoUpdateSuccess.asSharedFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            val result = authRepository.getUserProfile()
            
            if (result.isSuccess) {
                uiState = uiState.copy(isLoading = false, user = result.getOrNull(), error = null)
            } else {
                // Network failed, try offline metadata
                val offlineUser = authRepository.getUserFromMetadata()
                if (offlineUser != null) {
                    uiState = uiState.copy(
                        isLoading = false, 
                        user = offlineUser,
                        error = "No internet connection"
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

    fun updateProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = authRepository.uploadProfilePhoto(uri)
            uiState = result.fold(
                onSuccess = { imageUrl ->
                    val updatedUser = uiState.user?.copy(profile_image_url = imageUrl)
                    _profilePhotoUpdateSuccess.emit(Unit)
                    uiState.copy(isLoading = false, user = updatedUser, error = null)
                },
                onFailure = { error -> uiState.copy(isLoading = false, error = "No internet connection") }
            )
        }
    }

    fun signOut(onSignOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignOut()
        }
    }
}
