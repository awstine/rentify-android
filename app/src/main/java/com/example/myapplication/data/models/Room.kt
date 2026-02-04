package com.example.myapplication.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "rooms")
data class Room(
    @PrimaryKey
    val id: String,
    @SerialName("property_id")
    val property_id: String?,
    @SerialName("room_number")
    val room_number: String,
    val floor: Int?,
    @SerialName("monthly_rent")
    val monthly_rent: Double,
    @SerialName("is_available")
    val is_available: Boolean = true
)
