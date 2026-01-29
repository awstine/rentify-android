package com.example.myapplication.data.models

data class RoomItem(
    val number: String,
    val type: String,
    val price: Int,
    val isOccupied: Boolean
)

val rooms = listOf(
    RoomItem("1", "1 Bedroom", 15000, isOccupied = true),
    RoomItem("2", "Bedsitter", 8000, isOccupied = false), // Vacant
    RoomItem("3", "1 Bedroom", 15000, isOccupied = true),
    RoomItem("4", "Bedsitter", 25000, isOccupied = false) // Vacant
)
