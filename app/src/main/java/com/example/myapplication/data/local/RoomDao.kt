package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoomDao {

    @Query("SELECT * FROM cached_rooms")
    suspend fun getAllRooms(): List<CachedRoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<CachedRoomEntity>)

    @Query("DELETE FROM cached_rooms")
    suspend fun clearRooms()
}
