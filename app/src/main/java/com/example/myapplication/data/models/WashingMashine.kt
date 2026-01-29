package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

// WashingMachine.kt
@Serializable
data class WashingMachine(
    val id: String,
    val property_id: String?,
    val machine_number: String,
    val location: String?,
    val status: String = "available", // available, in_use, maintenance
    val cost_per_use: Double
)