package com.example.myapplication.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val status: String? = null,
    @SerialName("booking_id") val bookingId: String? = null,
    val mpesa_receipt_number: String? = null,
    @SerialName("created_at")
    val created_at: String? = null // Necessary for sorting and display
    // val amount: Double? = null // Optional: You can add this column to your DB if you want to show amount here
)
