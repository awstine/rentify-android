package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.local.PropertyDao
import com.example.myapplication.data.local.RoomDao
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class PropertyRepository @Inject constructor(
    private val propertyDao: PropertyDao,
    private val roomDao: RoomDao
) {

    private fun <T> networkBoundResource(
        query: () -> Flow<T>,
        fetch: suspend () -> T,
        saveFetchResult: suspend (T) -> Unit
    ): Flow<Result<T>> = flow {
        val data = query().first()
        // This is a simplified version, in a real app you'd want to check for staleness
        if (data != null) {
            emit(Result.success(data))
        }

        val fetchResult = try {
            Result.success(fetch())
        } catch (throwable: Throwable) {
            Log.e("PropertyRepository", "Network fetch failed", throwable)
            Result.failure(throwable)
        }

        if (fetchResult.isSuccess) {
            saveFetchResult(fetchResult.getOrThrow())
            // After saving, query the data again to get the updated flow
            emit(Result.success(query().first()))
        } else {
            // If network fails, still try to emit local data if available
            if (data != null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(fetchResult.exceptionOrNull()!!))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun getPropertiesForLandlord(landlordId: String): Flow<Result<List<Property>>> {
        return networkBoundResource(
            query = { propertyDao.getPropertiesForLandlord(landlordId) },
            fetch = { SupabaseClient.client.postgrest["properties"].select{filter{eq("landlord_id", landlordId)}}.decodeList() },
            saveFetchResult = { properties -> propertyDao.insertProperties(properties) }
        )
    }

    fun getAvailableProperties(): Flow<Result<List<Property>>> {
        return networkBoundResource(
            query = { propertyDao.getProperties() },
            fetch = { SupabaseClient.client.postgrest["properties"].select().decodeList() },
            saveFetchResult = { properties -> propertyDao.insertProperties(properties) }
        )
    }

    fun getAllAvailableRooms(): Flow<Result<List<Room>>> {
        return networkBoundResource(
            query = { roomDao.getRooms() },
            fetch = {
                Log.d("PropertyRepository", "Fetching available rooms from network")
                SupabaseClient.client.postgrest["rooms"].select { filter { eq("is_available", true) } }.decodeList()
            },
            saveFetchResult = { rooms ->
                Log.d("PropertyRepository", "Saving ${rooms.size} rooms to local database")
                roomDao.insertRooms(rooms)
            }
        )
    }

    fun getAllRooms(): Flow<Result<List<Room>>> {
        return networkBoundResource(
            query = { roomDao.getRooms() },
            fetch = { SupabaseClient.client.postgrest["rooms"].select().decodeList() },
            saveFetchResult = { rooms -> roomDao.insertRooms(rooms) }
        )
    }

    // Note: getPropertiesByIds, getRoom, getProperty are not converted to networkBoundResource
    // as they are single-shot operations and the offline caching strategy might differ.
    // You could convert them if you have a clear offline-first requirement for these specific calls.

    suspend fun getPropertiesByIds(ids: List<String>): Result<List<Property>> {
        if (ids.isEmpty()) return Result.success(emptyList())
         try {
            val properties = SupabaseClient.client.postgrest["properties"]
                .select {
                    filter {
                        isIn("id", ids)
                    }
                }
                .decodeList<Property>()
            return Result.success(properties)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getRoom(roomId: String): Result<Room> {
        try {
            val room = SupabaseClient.client.postgrest["rooms"]
                .select {
                    filter {
                        eq("id", roomId)
                    }
                }
                .decodeSingle<Room>()
            return Result.success(room)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    suspend fun getProperty(propertyId: String): Result<Property> {
        try {
            val property = SupabaseClient.client.postgrest["properties"]
                .select {
                    filter {
                        eq("id", propertyId)
                    }
                }
                .decodeSingle<Property>()
            return Result.success(property)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun createProperty(property: Property): Result<Unit> {
        try {
            SupabaseClient.client.postgrest["properties"].insert(property)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun createRoom(room: Room): Result<Unit> {
        try {
            SupabaseClient.client.postgrest["rooms"].insert(room)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun updateRoom(room: Room): Result<Unit> {
        try {
            SupabaseClient.client.postgrest["rooms"].update(room) { filter { eq("id", room.id) } }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun deleteRoom(roomId: String): Result<Unit> {
        try {
            SupabaseClient.client.postgrest["rooms"].delete { filter { eq("id", roomId) } }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getRoomsForLandlord(landlordId: String): Flow<Result<List<Room>>> {
        // This logic is a bit more complex. We first get the properties of the landlord,
        // then get the rooms for those properties. This might require a more sophisticated
        // offline strategy, potentially involving database relations.
        // For now, we'll keep it as a direct network call, but this is a candidate for improvement.
        return flow {
            try {
                val propertiesResult = getPropertiesForLandlord(landlordId).first()
                val properties = propertiesResult.getOrThrow()
                
                if (properties.isEmpty()) {
                    emit(Result.success(emptyList()))
                    return@flow
                }

                val propertyIds = properties.map { it.id }

                val rooms = SupabaseClient.client.postgrest["rooms"]
                    .select {
                        filter {
                            isIn("property_id", propertyIds)
                        }
                    }
                    .decodeList<Room>()
                emit(Result.success(rooms))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }.flowOn(Dispatchers.IO)
    }
}
