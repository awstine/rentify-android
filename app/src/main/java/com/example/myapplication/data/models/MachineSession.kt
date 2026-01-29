package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

// MachineSession.kt
@Serializable
data class MachineSession(
    val id: String,
    val machine_id: String,
    val tenant_id: String,
    val start_time: String,
    val end_time: String?,
    val cost: Double,
    val status: String = "active" // active, completed, cancelled
)