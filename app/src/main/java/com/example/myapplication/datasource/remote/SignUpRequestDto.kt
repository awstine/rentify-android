package com.example.myapplication.datasource.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val userData: UserDto,
)

@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String
)


@Serializable
data class UserDto(
    @SerialName("id")
    val userId: String,
    val profilePhotoUrl: String,
    @SerialName("phone_number")
    val phoneNumber: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("role")
    val role: Role
)

enum class Role(val value: String) {
    ADMIN("admin"),
    TENANT("tenant"),

    LANDLORD("landlord");

    companion object {
        fun fromValue(value: String?): Role {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: TENANT
        }
    }
}

@Serializable
data class UserResponseDto(
    val id: String,
    val email: String?,
    @SerialName("phone_number")
    val phoneNumber: String?,
    @SerialName("id_number")
    val idNumber: String?,
    val role: String, // 'landlord', 'tenant', 'admin'
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("created_at")
    val createdAt: String?,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null
)
