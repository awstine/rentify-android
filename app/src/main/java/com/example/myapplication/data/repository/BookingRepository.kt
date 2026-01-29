package com.example.myapplication.data.repository

import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.di.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookingRepository {

    suspend fun createBooking(booking: Booking): Result<Booking> {
        return withContext(Dispatchers.IO) {
            try {
                val createdBooking = SupabaseClient.client.postgrest["bookings"]
                    .insert(booking) {
                        select() // Explicitly request the inserted row
                    }
                    .decodeSingle<Booking>()
                Result.success(createdBooking)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getCurrentBooking(tenantId: String): Result<Booking?> {
        return withContext(Dispatchers.IO) {
            try {
                val booking = SupabaseClient.client.postgrest["bookings"]
                    .select {
                        filter {
                            eq("tenant_id", tenantId)
                            eq("status", "active") // <--- ADD THIS FILTER
                            // We only want 'active' bookings. Ignore 'pending'.
                        }
                    }
                    .decodeSingleOrNull<Booking>()
                Result.success(booking)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getBookingsForTenant(tenantId: String): Result<List<Booking>> {
        return withContext(Dispatchers.IO) {
            try {
                val bookings = SupabaseClient.client.postgrest["bookings"]
                    .select {
                        filter {
                            eq("tenant_id", tenantId)
                        }
                    }
                    .decodeList<Booking>()
                Result.success(bookings)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getBookingsForLandlord(landlordId: String): Result<List<Booking>> {
        return withContext(Dispatchers.IO) {
            try {
                // First get landlord's properties
                val properties = SupabaseClient.client.postgrest["properties"]
                    .select {
                        filter {
                            eq("landlord_id", landlordId)
                        }
                    }
                    .decodeList<Property>()

                if (properties.isEmpty()) return@withContext Result.success(emptyList())

                // Get all rooms in these properties
                val propertyIds = properties.map { it.id }
                val rooms = SupabaseClient.client.postgrest["rooms"]
                    .select {
                        filter {
                            isIn("property_id", propertyIds) // Use isIn for list of IDs
                        }
                    }
                    .decodeList<Room>()

                if (rooms.isEmpty()) return@withContext Result.success(emptyList())

                // Get bookings for these rooms
                val roomIds = rooms.map { it.id }
                val bookings = SupabaseClient.client.postgrest["bookings"]
                    .select {
                        filter {
                            isIn("room_id", roomIds) // Use isIn for list of IDs
                        }
                    }
                    .decodeList<Booking>()

                Result.success(bookings)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
