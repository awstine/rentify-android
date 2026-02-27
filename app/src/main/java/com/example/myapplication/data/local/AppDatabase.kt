package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedRoomEntity::class, TenantDashboardEntity::class, AdminDashboardEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
    abstract fun tenantDashboardDao(): TenantDashboardDao
    abstract fun adminDashboardDao(): AdminDashboardDao
}
