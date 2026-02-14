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
            uiState = uiState.copy(isLoading = true)
            val result = authRepository.getUserProfile()
            uiState = result.fold(
                onSuccess = { user -> uiState.copy(isLoading = false, user = user) },
                onFailure = { error -> uiState.copy(isLoading = false, error = error.message) }
            )
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
                    uiState.copy(isLoading = false, user = updatedUser)
                },
                onFailure = { error -> uiState.copy(isLoading = false, error = error.message) }
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
