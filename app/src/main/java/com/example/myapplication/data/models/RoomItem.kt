package com.example.myapplication.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "room_items")
data class RoomItem(
    @PrimaryKey
    val number: String,
    val type: String,
    val price: Int,
    val isOccupied: Boolean
)
