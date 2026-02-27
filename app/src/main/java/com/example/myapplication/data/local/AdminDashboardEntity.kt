package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_dashboard")
data class AdminDashboardEntity(
    @PrimaryKey val userId: String,
    val totalRooms: Int,
    val availableRooms: Int,
    val activeTenants: Int,
    val pendingRequests: Int,
    val estimatedRevenue: Double
)
