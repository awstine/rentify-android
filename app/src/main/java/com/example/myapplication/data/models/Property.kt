package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

// Property.kt
@Serializable
data class Property(
    val id: String,
    val landlord_id: String,
    val name: String,
    val address: String,
    val description: String?,
    val total_rooms: Int,
    val created_at: String?
)
