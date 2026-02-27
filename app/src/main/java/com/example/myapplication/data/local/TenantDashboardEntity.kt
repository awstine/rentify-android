package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenant_dashboard")
data class TenantDashboardEntity(
    @PrimaryKey val userId: String,
    val roomNumber: String,
    val monthlyRent: Double?,
    val paymentStatus: String?,
    val dueDate: String?
)
