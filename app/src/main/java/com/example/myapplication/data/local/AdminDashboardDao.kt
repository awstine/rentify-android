package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AdminDashboardDao {

    @Query("SELECT * FROM admin_dashboard WHERE userId = :userId")
    suspend fun getDashboard(userId: String): AdminDashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDashboard(data: AdminDashboardEntity)
}