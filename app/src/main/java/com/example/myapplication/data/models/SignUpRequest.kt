package com.example.myapplication.data.models

import com.example.myapplication.datasource.remote.SignUpDto

data class SignUpRequest(
    val email: String,
    val password: String,
    val userData: User
)

fun SignUpRequest.toDto(): SignUpDto =
    SignUpDto(
        email = email,
        password = password,
        userData = userData
    )