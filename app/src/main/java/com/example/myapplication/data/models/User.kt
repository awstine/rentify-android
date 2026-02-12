package com.example.myapplication.data.models

// User.kt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String?,
    @SerialName("phone_number")
    val phone_number: String?,
    @SerialName("id_number")
    val id_number: String?,
    val role: String, // 'landlord', 'tenant', 'admin'
    @SerialName("full_name")
    val full_name: String?,
    @SerialName("created_at")
    val created_at: String?,
    @SerialName("profile_image_url")
    val profile_image_url: String? = null
)