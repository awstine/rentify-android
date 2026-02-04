package com.example.myapplication.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Booking.kt
@Serializable
@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey
    val id: String,
    @SerialName("room_id")
    val room_id: String,
    @SerialName("tenant_id")
    val tenant_id: String,
    @SerialName("start_date")
    val start_date: String,
    @SerialName("end_date")
    val end_date: String,
    @SerialName("monthly_rent")
    val monthly_rent: Double,
    val status: String = "pending", // pending, active, expired
    @SerialName("payment_status")
    val payment_status: String = "unpaid", // unpaid, paid, partial
    @SerialName("created_at")
    val created_at: String?
)
