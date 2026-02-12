package com.example.myapplication.screens.profile

import com.example.myapplication.data.models.User

data class ProfileState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)
