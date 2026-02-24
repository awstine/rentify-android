package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_rooms")
data class CachedRoomEntity(
    @PrimaryKey val id: String,
    val propertyId: String,
    val roomNumber: String,
    val floor: Int?,
    val monthlyRent: Double,
    val isAvailable: Boolean
)
