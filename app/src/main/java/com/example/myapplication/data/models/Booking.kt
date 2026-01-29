package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

// Booking.kt
@Serializable
data class Booking(
    val id: String,
    val room_id: String,
    val tenant_id: String,
    val start_date: String,
    val end_date: String,
    val monthly_rent: Double,
    val status: String = "pending", // pending, active, expired
    val payment_status: String = "unpaid", // unpaid, paid, partial
    val created_at: String?
)