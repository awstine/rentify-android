package com.example.myapplication.data.models

import com.example.myapplication.datasource.remote.Role
import com.example.myapplication.datasource.remote.UserDto
import com.example.myapplication.datasource.remote.UserResponseDto
import kotlinx.datetime.LocalDateTime

data class User(
    val id: String,
    val email: String?,
    val phone_number: String?,
    val id_number: String?,
    val role: String, // 'landlord', 'tenant', 'admin'
    val full_name: String?,
    val created_at: String?,
    val profileImageUrl: String? = null
)


fun User.toDto (): UserDto {
    return UserDto(
        userId = id,
        profilePhotoUrl = profileImageUrl ?: "",
        phoneNumber = phone_number ?: "",
        fullName = full_name ?: "",
        role = Role.fromValue(role)
    )
}

fun UserDto.toModel (): User =
    User(
        id = this.userId,
        email = "",
        phoneNumber,
        id_number = "",
        role = role.name,
        full_name = fullName,
        created_at = LocalDateTime.toString(),
        profileImageUrl = profilePhotoUrl,
    )

fun UserResponseDto.toUser(): User =
    User(
        id = id,
        email = "",
        phoneNumber,
        id_number = "",
        role = role,
        full_name = fullName,
        created_at = LocalDateTime.toString(),
        profileImageUrl = profileImageUrl,
    )