package com.example.myapplication.screens.tenant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.models.MachineSession
import com.example.myapplication.data.models.User
import com.example.myapplication.data.models.WashingMachine
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.MachineRepository
import com.example.myapplication.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class TenantHomeUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val activeBooking: Booking? = null,
    val roomNumber: String = "Loading...",
    val machines: List<WashingMachine> = emptyList(),
    val activeSession: MachineSession? = null,
    val error: String? = null,
    val displayName: String = "Tenant"
)

@HiltViewModel
class TenantHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository,
    private val machineRepository: MachineRepository,
    private val propertyRepository: PropertyRepository
) : ViewModel() {

    var uiState by mutableStateOf(TenantHomeUiState())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            // 1. Get User
            var user = authRepository.getCurrentUser()
            
            // If user is null, try reloading the session from storage (handles process death/restore)
            if (user == null) {
                if (authRepository.hasValidSession()) {
                     user = authRepository.getCurrentUser()
                }
            }
            
            if (user == null) {
                uiState = uiState.copy(isLoading = false, error = "User not found")
                return@launch
            }

            // Extract Name from Email with safety check
            val emailName = user.email?.substringBefore("@")
            val formattedName = if (!emailName.isNullOrBlank()) {
                emailName.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() 
                }
            } else {
                "Tenant"
            }

            // 2. Get Bookings
            val bookingsResult = bookingRepository.getBookingsForTenant(user.id)
            val bookings = bookingsResult.getOrNull() ?: emptyList()
            val activeBooking = bookings.find { it.status == "active" }

            var roomNumber = "No Room"
            if (activeBooking != null) {
                val roomResult = propertyRepository.getRoom(activeBooking.room_id)
                roomResult.onSuccess {
                    roomNumber = "Room ${it.room_number}"
                }
            }

            // 3. Get Machines
            val machinesResult = machineRepository.getMachinesForTenant()
            val machines = machinesResult.getOrNull() ?: emptyList()

            // 4. Get Active Session (for the current user)
            val sessionResult = machineRepository.getActiveSessionsForTenant(user.id)
            val activeSession = sessionResult.getOrNull()?.firstOrNull()

            uiState = uiState.copy(
                isLoading = false,
                user = user,
                displayName = formattedName,
                activeBooking = activeBooking,
                roomNumber = roomNumber,
                machines = machines,
                activeSession = activeSession
            )
        }
    }
}
