package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.CachedRoomEntity
import com.example.myapplication.data.local.RoomDao
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PropertyRepository @Inject constructor(
    private val roomDao: RoomDao
) {
    suspend fun getPropertiesForLandlord(landlordId: String): Result<List<Property>> {
        return withContext(Dispatchers.IO) {
            try {
                val properties = SupabaseClient.client.postgrest["properties"]
                    .select{filter{eq("landlord_id", landlordId)}}
                    .decodeList<Property>()
                Result.success(properties)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getAvailableProperties(): Result<List<Property>> {
        return withContext(Dispatchers.IO) {
            try {
                // Optimized: The RLS policy "Tenants view available properties" on Supabase
                // now handles the filtering. We simply fetch the properties visible to us.
                val properties = SupabaseClient.client.postgrest["properties"]
                    .select()
                    .decodeList<Property>()
                Result.success(properties)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun getAvailableRoomsForProperty(propertyId: String): Result<List<Room>> {
        return withContext(Dispatchers.IO) {
            try {
                val rooms = SupabaseClient.client.postgrest["rooms"]
                    .select {
                        filter {
                            eq("property_id", propertyId)
                            eq("is_available", true)
                        }
                    }
                    .decodeList<Room>()
                Result.success(rooms)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getAllAvailableRooms(): Result<List<Room>> {
        return withContext(Dispatchers.IO) {
            try {
                // 🌍 Try network first
                val rooms = SupabaseClient.client.postgrest["rooms"]
                    .select {
                        filter { eq("is_available", true) }
                    }
                    .decodeList<Room>()

                // 💾 Save to local cache
                val cached = rooms.mapNotNull { room ->
                    room.property_id?.let {
                        CachedRoomEntity(
                            id = room.id,
                            propertyId = it,
                            roomNumber = room.room_number,
                            floor = room.floor,
                            monthlyRent = room.monthly_rent,
                            isAvailable = room.is_available
                        )
                    }
                }

                roomDao.clearRooms()
                roomDao.insertRooms(cached)

                Result.success(rooms)

            } catch (e: Exception) {

                // 📵 If network fails → load from cache
                try {
                    val cachedRooms = roomDao.getAllRooms()

                    val rooms = cachedRooms.map {
                        Room(
                            id = it.id,
                            property_id = it.propertyId,
                            room_number = it.roomNumber,
                            floor = it.floor,
                            monthly_rent = it.monthlyRent,
                            is_available = it.isAvailable
                        )
                    }

                    Result.success(rooms)

                } catch (cacheError: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun getAllRooms(): Result<List<Room>> {
        return withContext(Dispatchers.IO) {
            try {
                // 🌍 Try network first
                val rooms = SupabaseClient.client.postgrest["rooms"]
                    .select()
                    .decodeList<Room>()

                // 💾 Save to local cache
                val cached = rooms.mapNotNull { room ->
                    room.property_id?.let {
                        CachedRoomEntity(
                            id = room.id,
                            propertyId = it,
                            roomNumber = room.room_number,
                            floor = room.floor,
                            monthlyRent = room.monthly_rent,
                            isAvailable = room.is_available
                        )
                    }
                }

                roomDao.clearRooms()
                roomDao.insertRooms(cached)

                Result.success(rooms)

            } catch (e: Exception) {

                // 📵 If network fails → load from cache
                try {
                    val cachedRooms = roomDao.getAllRooms()

                    val rooms = cachedRooms.map {
                        Room(
                            id = it.id,
                            property_id = it.propertyId,
                            room_number = it.roomNumber,
                            floor = it.floor,
                            monthly_rent = it.monthlyRent,
                            is_available = it.isAvailable
                        )
                    }

                    Result.success(rooms)

                } catch (cacheError: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun getPropertiesByIds(ids: List<String>): Result<List<Property>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return withContext(Dispatchers.IO) {
            try {
                val properties = SupabaseClient.client.postgrest["properties"]
                    .select {
                        filter {
                            isIn("id", ids)
                        }
                    }
                    .decodeList<Property>()
                Result.success(properties)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getRoom(roomId: String): Result<Room> {
        return withContext(Dispatchers.IO) {
            try {
                val room = SupabaseClient.client.postgrest["rooms"]
                    .select {
                        filter {
                            eq("id", roomId)
                        }
                    }
                    .decodeSingle<Room>()
                Result.success(room)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getProperty(propertyId: String): Result<Property> {
        return withContext(Dispatchers.IO) {
            try {
                val property = SupabaseClient.client.postgrest["properties"]
                    .select {
                        filter {
                            eq("id", propertyId)
                        }
                    }
                    .decodeSingle<Property>()
                Result.success(property)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    // ... inside PropertyRepository class

    suspend fun createProperty(property: Property): Result<Property> {
        return withContext(Dispatchers.IO) {
            try {
                val createdProperty = SupabaseClient.client.postgrest["properties"]
                    .insert(property) {
                        select() // <--- THIS IS CRITICAL. It asks Supabase to return the new row.
                    }
                    .decodeSingle<Property>()
                Result.success(createdProperty)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun createRoom(room: Room): Result<Room> {
        return withContext(Dispatchers.IO) {
            try {
                val createdRoom = SupabaseClient.client.postgrest["rooms"]
                    .insert(room) {
                        select() // <--- THIS IS CRITICAL
                    }
                    .decodeSingle<Room>()
                Result.success(createdRoom)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun updateRoom(room: Room): Result<Room> {
        return withContext(Dispatchers.IO) {
            try {
                val updatedRoom = SupabaseClient.client.postgrest["rooms"]
                    .update(room) {
                        filter {
                            eq("id", room.id)
                        }
                        select()
                    }
                    .decodeSingle<Room>()
                Result.success(updatedRoom)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseClient.client.postgrest["rooms"]
                    .delete {
                        filter {
                            eq("id", roomId)
                        }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getRoomsForLandlord(landlordId: String): Result<List<Room>> {
        return withContext(Dispatchers.IO) {
            try {
                val propertiesResult = getPropertiesForLandlord(landlordId)
                val properties = propertiesResult.getOrNull() ?: emptyList()
                
                if (properties.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val propertyIds = properties.map { it.id }

                val rooms = SupabaseClient.client.postgrest["rooms"]
                    .select {
                        filter {
                            isIn("property_id", propertyIds)
                        }
                    }
                    .decodeList<Room>()
                Result.success(rooms)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
