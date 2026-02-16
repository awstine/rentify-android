package com.example.myapplication.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Booking
import com.example.myapplication.data.models.RecentPayment
import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.BookingRepository
import com.example.myapplication.data.repository.PaymentRepository
import com.example.myapplication.data.repository.PropertyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardState(
    val isLoading: Boolean = false,
    val totalRooms: Int = 0,
    val availableRooms: Int = 0,
    val activeTenants: Int = 0,
    val pendingRequests: Int = 0,
    val estimatedRevenue: Double = 0.0,
    val recentPayments: List<RecentPayment> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AdminHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val propertyRepository: PropertyRepository,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    var uiState by mutableStateOf(AdminDashboardState())
        private set

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val user = authRepository.getCurrentUser()
                if (user == null) {
                    uiState = uiState.copy(isLoading = false, error = "User not found")
                    return@launch
                }

                // 1. Fetch Rooms
                val roomsResult = propertyRepository.getRoomsForLandlord(user.id)
                val rooms = roomsResult.getOrNull() ?: emptyList()
                val totalRooms = rooms.size
                val availableRooms = rooms.count { it.is_available }

                // 2. Fetch Bookings
                val bookingsResult = bookingRepository.getBookingsForLandlord(user.id)
                val bookings = bookingsResult.getOrNull() ?: emptyList()
                
                val activeTenants = bookings.count { it.status == "active" }
                val pendingRequests = bookings.count { it.status == "pending" }
                
                // Simple revenue calc: Sum of monthly rent for active paid bookings
                val estimatedRevenue = bookings
                    .filter { it.status == "active" && it.payment_status == "paid" }
                    .sumOf { it.monthly_rent }

                // 3. Fetch Recent Payments
                val recentPaymentsResult = paymentRepository.getRecentPayments(user.id)
                val recentPayments = recentPaymentsResult.getOrNull() ?: emptyList()

                uiState = uiState.copy(
                    isLoading = false,
                    totalRooms = totalRooms,
                    availableRooms = availableRooms,
                    activeTenants = activeTenants,
                    pendingRequests = pendingRequests,
                    estimatedRevenue = estimatedRevenue,
                    recentPayments = recentPayments
                )

            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e.message)
            }
        }
    }
}
