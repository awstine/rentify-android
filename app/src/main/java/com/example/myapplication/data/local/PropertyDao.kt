package com.example.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.models.Property
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<Property>)

    @Query("SELECT * FROM properties")
    fun getProperties(): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE landlordId = :landlordId")
    fun getPropertiesForLandlord(landlordId: String): Flow<List<Property>>
}
