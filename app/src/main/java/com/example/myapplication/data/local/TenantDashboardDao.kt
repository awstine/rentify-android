package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TenantDashboardDao {

    @Query("SELECT * FROM tenant_dashboard WHERE userId = :userId")
    suspend fun getDashboard(userId: String): TenantDashboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDashboard(data: TenantDashboardEntity)
}
