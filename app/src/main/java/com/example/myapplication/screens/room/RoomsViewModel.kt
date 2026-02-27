package com.example.myapplication.screens.room

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.models.Property
import com.example.myapplication.data.models.Room
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PropertyRepository
import com.example.myapplication.di.SupabaseClient
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class RoomWithTenant(
    val room: Room,
    val tenantName: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

// Helper class for internal use
data class BookingDetails(
    val name: String,
    val startDate: String,
    val endDate: String
)

data class RoomsUiState(
    val rooms: List<RoomWithTenant> = emptyList(),
    val properties: List<Property> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLandlord: Boolean = false,
    val isAdmin: Boolean = false,
    val isTenant: Boolean = false,
    val isBookingSuccess: Boolean = false,
    val bookingId: String? = null,
    val bookingRoom: Room? = null,
    val hasActiveBooking: Boolean = false
)

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val propertyRepository: PropertyRepository,
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    var uiState by mutableStateOf(RoomsUiState())
        private set

    init {
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val profileResult = authRepository.getUserProfile()
                val profile = profileResult.getOrNull()



                val role = profile?.role?.lowercase(Locale.ROOT) ?: "tenant"
                val userId = profile?.id

                // 1. Fetch the data based on role
                when (role) {
                    "admin", "landlord" -> {
                        // ... (This part stays the same as your code) ...
                        // Fetch properties and rooms logic...
                        val properties = if (role == "admin") {
                            propertyRepository.getAvailableProperties().getOrDefault(emptyList())
                        } else {
                            propertyRepository.getPropertiesForLandlord(userId!!).getOrDefault(emptyList())
                        }

                        val rooms = if (role == "admin") {
                            propertyRepository.getAllRooms().getOrDefault(emptyList())
                        } else {
                            propertyRepository.getRoomsForLandlord(userId!!).getOrDefault(emptyList())
                        }

                        // Attach Tenant Names logic...
                        val roomsWithTenants = rooms.map { room ->
                            var tenantName: String? = null
                            var startDate: String? = null
                            var endDate: String? = null

                            if (!room.is_available) {
                                val details = fetchBookingDetails(room.id)
                                if (details != null) {
                                    tenantName = details.name
                                    startDate = details.startDate
                                    endDate = details.endDate
                                } else {
                                    tenantName = "Unknown"
                                }
                            }
                            RoomWithTenant(room, tenantName, startDate, endDate)
                        }

                        uiState = uiState.copy(
                            isLoading = false,
                            rooms = roomsWithTenants,
                            properties = properties,
                            isLandlord = (role == "landlord"),
                            isAdmin = (role == "admin"),
                            isTenant = false
                        )
                    }
                    else -> {
                        // === TENANT LOGIC (THE FIX IS HERE) ===
                        val result = propertyRepository.getAllAvailableRooms()
                        val rooms = result.getOrDefault(emptyList())
                        val roomsWithTenants = rooms.map { RoomWithTenant(it, null) }

                        // Check if tenant has active booking
                        var hasBooking = false
                        if (userId != null) {
                            val bookingsResult = bookingRepository.getBookingsForTenant(userId)
                            val bookings = bookingsResult.getOrDefault(emptyList())

                            // FIX: Only block if status is 'active' (Paid).
                            // We IGNORE 'pending' so they can try to book again if they failed to pay.
                            hasBooking = bookings.any { it.status == "active" }
                        }

                        uiState = uiState.copy(
                            isLoading = false,
                            rooms = roomsWithTenants,
                            isLandlord = false,
                            isAdmin = false,
                            isTenant = true,
                            hasActiveBooking = hasBooking
                        )
                    }
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message)
            }
        }
    }

    private suspend fun fetchBookingDetails(roomId: String): BookingDetails? {
        return try {
            // A. Find the ACTIVE booking for this room
            // We specifically look for 'active' or 'paid' status so we don't
            // accidentally pick up a pending/abandoned booking attempt.
            val booking = SupabaseClient.client.postgrest["bookings"]
                .select {
                    filter {
                        eq("room_id", roomId)
                        // CRITICAL FIX: Only fetch the confirmed booking!
                        // Adjust 'active' to match exactly what you save in DB ('active' or 'paid')
                        isIn("status", listOf("active", "paid", "approved"))
                    }
                    // If there are multiple active ones (rare), get the latest
                    order("created_at", order = Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<Booking>()

            if (booking != null) {
                // B. Find the Profile of the tenant
                val tenantProfile = SupabaseClient.client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", booking.tenant_id)
                        }
                    }.decodeSingleOrNull<com.example.myapplication.data.models.User>()

                val name: String
                if (tenantProfile != null) {
                    // --- NAME EXTRACTION LOGIC ---
                    if (!tenantProfile.full_name.isNullOrBlank()) {
                        name = tenantProfile.full_name
                    } else if (!tenantProfile.email.isNullOrBlank()) {
                        val emailHandle = tenantProfile.email.substringBefore("@")
                        val firstName = emailHandle.substringBefore(".")
                        name = firstName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                        }
                    } else {
                        name = "Unnamed User"
                    }
                } else {
                    name = "Profile Locked"
                }

                // Return the REAL contract dates
                BookingDetails(name, booking.start_date, booking.end_date)
            } else {
                // If no ACTIVE booking exists, but room is marked occupied,
                // it might be a database inconsistency or a very new pending booking.
                // You can return null or a placeholder.
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    fun createRoom(room: Room) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = propertyRepository.createRoom(room)
            if (result.isSuccess) {
                loadRooms()
            } else {
                uiState = uiState.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun updateRoom(room: Room) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = propertyRepository.updateRoom(room)
            if (result.isSuccess) {
                loadRooms()
            } else {
                uiState = uiState.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val result = propertyRepository.deleteRoom(roomId)
            if (result.isSuccess) {
                loadRooms()
            } else {
                uiState = uiState.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun createPropertyAndRoom(propertyName: String, propertyAddress: String, room: Room) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val profileResult = authRepository.getUserProfile()
                val profile = profileResult.getOrNull()
                if (profile != null) {
                    val propertyId = UUID.randomUUID().toString()
                    val newProperty = Property(
                        id = propertyId,
                        landlord_id = profile.id,
                        name = propertyName,
                        address = propertyAddress,
                        description = null,
                        total_rooms = 0,
                        created_at = null
                    )

                    val propResult = propertyRepository.createProperty(newProperty)
                    if (propResult.isSuccess) {
                        val roomWithProp = room.copy(property_id = propertyId)
                        val roomResult = propertyRepository.createRoom(roomWithProp)
                        if (roomResult.isSuccess) {
                            loadRooms()
                        } else {
                            uiState = uiState.copy(isLoading = false, error = "Property created but room failed: " + roomResult.exceptionOrNull()?.message)
                        }
                    } else {
                        uiState = uiState.copy(isLoading = false, error = propResult.exceptionOrNull()?.message)
                    }
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createProperty(name: String, address: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val profileResult = authRepository.getUserProfile()
            val profile = profileResult.getOrNull()

            if (profile != null) {
                val newProperty = Property(
                    id = UUID.randomUUID().toString(),
                    landlord_id = profile.id,
                    name = name,
                    address = address,
                    description = "",
                    total_rooms = 0,
                    created_at = null
                )

                val result = propertyRepository.createProperty(newProperty)

                if (result.isSuccess) {
                    loadRooms()
                } else {
                    uiState = uiState.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun bookRoom(room: Room) {
        if (uiState.hasActiveBooking) {
            uiState = uiState.copy(error = "You already have a booked room. Please check your history.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, isBookingSuccess = false)
            try {
                val profileResult = authRepository.getUserProfile()
                val profile = profileResult.getOrNull()
                if (profile != null) {
                    val booking = Booking(
                        id = UUID.randomUUID().toString(),
                        room_id = room.id,
                        tenant_id = profile.id,
                        start_date = java.time.LocalDate.now().toString(), // Default to today
                        end_date = java.time.LocalDate.now().plusMonths(1).toString(), // Default 1 month
                        monthly_rent = room.monthly_rent,
                        status = "pending",
                        payment_status = "unpaid",
                        created_at = null
                    )

                    val result = bookingRepository.createBooking(booking)
                    if (result.isSuccess) {
                        val createdBooking = result.getOrNull()
                        uiState = uiState.copy(
                            isLoading = false,
                            isBookingSuccess = true,
                            bookingId = createdBooking?.id,
                            bookingRoom = room
                        )
                        loadRooms()
                    } else {
                         uiState = uiState.copy(isLoading = false, error = result.exceptionOrNull()?.message)
                    }
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun resetBookingState() {
        uiState = uiState.copy(isBookingSuccess = false, bookingId = null, bookingRoom = null)
    }
}
