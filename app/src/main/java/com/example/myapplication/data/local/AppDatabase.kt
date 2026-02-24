package com.example.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CachedRoomEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
}