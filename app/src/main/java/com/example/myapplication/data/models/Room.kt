package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

// Room.kt
@Serializable
data class Room(
    val id: String,
    val property_id: String?,
    val room_number: String,
    val floor: Int?,
    val monthly_rent: Double,
    val is_available: Boolean = true
)