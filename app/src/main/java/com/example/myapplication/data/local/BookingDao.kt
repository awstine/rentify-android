package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.models.Booking
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<Booking>)

    @Query("SELECT * FROM bookings WHERE tenant_id = :tenantId")
    fun getBookingsForTenant(tenantId: String): Flow<List<Booking>>
}
