package com.example.myapplication.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentPayment(
    @SerialName("room_name") val roomName: String,
    @SerialName("occupant_name") val occupantName: String,
    val amount: Double
)
