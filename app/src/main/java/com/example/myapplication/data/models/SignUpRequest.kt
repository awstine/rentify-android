package com.example.myapplication.data.models

import com.example.myapplication.datasource.remote.SignInRequestDto
import com.example.myapplication.datasource.remote.SignUpRequestDto

data class SignUpRequest(
    val email: String,
    val password: String,
    val userData: User
)

fun SignUpRequest.toDto(): SignUpRequestDto =
    SignUpRequestDto(
        email = email,
        password = password,
        userData = userData.toDto()
    )

data class SignInRequest(val email: String, val password: String)

fun SignInRequest.toDto(): SignInRequestDto =
    SignInRequestDto(
        email = email,
        password = password
    )